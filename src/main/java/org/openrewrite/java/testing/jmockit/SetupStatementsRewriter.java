/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.testing.jmockit;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;

import java.util.*;

class SetupStatementsRewriter {

    private final JavaVisitor<ExecutionContext> visitor;
    private J.Block methodBody;

    SetupStatementsRewriter(JavaVisitor<ExecutionContext> visitor, J.Block methodBody) {
        this.visitor = visitor;
        this.methodBody = methodBody;
    }

    J.Block rewriteMethodBody() {
        List<Statement> statements = methodBody.getStatements();
        // iterate over each statement in the method body, find Expectations blocks and rewrite them
        for (Statement s : statements) {
            if (!JMockitUtils.isValidExpectationsNewClassStatement(s)) {
                continue;
            }
            J.NewClass nc = (J.NewClass) s;
            Set<String> spies = new HashSet<>();
            for (Expression newClassArg : nc.getArguments()) {
                if (newClassArg instanceof J.Identifier) {
                    spies.add(((J.Identifier) newClassArg).getSimpleName());
                }
            }

            assert nc.getBody() != null;
            J.Block expectationsBlock = (J.Block) nc.getBody().getStatements().get(0);

            // statement needs to be moved directly before expectations class instantiation
            JavaCoordinates coordinates = nc.getCoordinates().before();
            List<Statement> newExpectationsBlockStatements = new ArrayList<>();
            for (Statement expectationStatement : expectationsBlock.getStatements()) {
                if (!isSetupStatement(expectationStatement, spies)) {
                    newExpectationsBlockStatements.add(expectationStatement);
                    continue;
                }
                rewriteBodyStatement(expectationStatement, coordinates);
                // subsequent setup statements are moved in order
                coordinates = expectationStatement.getCoordinates().after();
            }
            // the new expectations block has the setup statements removed
            J.Block newExpectationsBlock = expectationsBlock.withStatements(newExpectationsBlockStatements);
            nc = nc.withBody(nc.getBody().withStatements(Collections.singletonList(newExpectationsBlock)));

            rewriteBodyStatement(nc, nc.getCoordinates().replace());
        }
        return methodBody;
    }

    private void rewriteBodyStatement(Statement statement, JavaCoordinates coordinates) {
        methodBody = JavaTemplate.builder("#{any()}")
                .javaParser(JavaParser.fromJavaVersion())
                .build()
                .apply(
                        new Cursor(visitor.getCursor(), methodBody),
                        coordinates,
                        statement
                );
    }

    private static boolean isSetupStatement(Statement expectationStatement, Set<String> spies) {
        if (expectationStatement instanceof J.MethodInvocation) {
            // a method invocation on a mock is not a setup statement
            J.MethodInvocation methodInvocation = (J.MethodInvocation) expectationStatement;
            if (methodInvocation.getSelect() instanceof J.MethodInvocation) {
                return isSetupStatement((Statement) methodInvocation.getSelect(), spies);
            } else if (methodInvocation.getSelect() instanceof J.Identifier) {
                return isNotMockIdentifier((J.Identifier) methodInvocation.getSelect(), spies);
            } else if (methodInvocation.getSelect() instanceof J.FieldAccess) {
                return isNotMockIdentifier((J.Identifier) ((J.FieldAccess) methodInvocation.getSelect()).getTarget(), spies);
            } else {
                return isNotMockIdentifier(methodInvocation.getName(), spies);
            }
        } else if (expectationStatement instanceof J.Assignment) {
            // an assignment to a jmockit reserved field is not a setup statement
            J.Assignment assignment = (J.Assignment) expectationStatement;
            if (assignment.getVariable() instanceof J.FieldAccess) {
                return true;
            }
            J.Identifier identifier = (J.Identifier) assignment.getVariable();
            String identifierName = identifier.getSimpleName();
            return !identifierName.equals("result")
                    && !identifierName.equals("times")
                    && !identifierName.equals("minTimes");
        }
        return true;
    }

    private static boolean isNotMockIdentifier(J.Identifier identifier, Set<String> spies) {
        if (spies.contains(identifier.getSimpleName())) {
            return false;
        }
        if (identifier.getType() instanceof JavaType.Method
                && TypeUtils.isAssignableTo("mockit.Expectations",
                ((JavaType.Method) identifier.getType()).getDeclaringType())) {
            return false;
        }
        JavaType.Variable fieldType = identifier.getFieldType();
        if (fieldType == null) {
            return true;
        }
        for (JavaType.FullyQualified annotationType : fieldType.getAnnotations()) {
            if (TypeUtils.isAssignableTo("mockit.Mocked", annotationType)
                    || TypeUtils.isAssignableTo("org.mockito.Mock", annotationType)
                    || TypeUtils.isAssignableTo("mockit.Injectable", annotationType)
                    || TypeUtils.isAssignableTo("mockit.Tested", annotationType)
                    || TypeUtils.isAssignableTo("org.mockito.InjectMocks", annotationType)) {
                return false;
            }
        }
        return true;
    }
}
