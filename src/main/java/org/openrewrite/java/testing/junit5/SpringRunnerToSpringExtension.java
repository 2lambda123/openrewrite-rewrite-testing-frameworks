/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.testing.junit5;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.testing.junit5.FrameworkTypes.*;

/**
 * JUnit4 Spring test classes are annotated with @RunWith(SpringRunner.class)
 * Turn this into the JUnit5-compatible @ExtendsWith(SpringExtension.class)
 */
public class SpringRunnerToSpringExtension extends Recipe {

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new SpringRunnerToSpringExtensionVisitor();
    }

    public static class SpringRunnerToSpringExtensionVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final String springExtensionType =
                "org.springframework.test.context.junit.jupiter.SpringExtension";
        private static final String springRunnerType =
                "org.springframework.test.context.junit4.SpringRunner";

        // Reference @RunWith(SpringRunner.class) annotation for semantically equal to compare against
        private static final J.Annotation runWithSpringRunnerAnnotation = new J.Annotation(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                runWithIdent,
                JContainer.build(
                        Collections.singletonList(
                                JRightPadded.build(
                                        new J.FieldAccess(
                                                randomId(),
                                                Space.EMPTY,
                                                Markers.EMPTY,
                                                J.Identifier.build(
                                                        randomId(),
                                                        Space.EMPTY,
                                                        Markers.EMPTY,
                                                        "SpringRunner",
                                                        JavaType.buildType(springRunnerType)
                                                ),
                                                JLeftPadded.build(J.Identifier.build(randomId(), Space.EMPTY, Markers.EMPTY, "class", null)),
                                                JavaType.Class.build("java.lang.Class")
                                        )
                                )
                        )
                )
        );

        private static final JavaType.Class springJUnit4ClassRunnerType =
                JavaType.Class.build("org.springframework.test.context.junit4.SpringJUnit4ClassRunner");

        // Reference @RunWith(SpringJUnit4ClassRunner.class) annotation for semantically equal to compare against
        private static final J.Annotation runWithSpringJUnit4ClassRunnerAnnotation = new J.Annotation(
                randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                runWithIdent,
                JContainer.build(
                        Collections.singletonList(
                                JRightPadded.build(
                                        new J.FieldAccess(
                                                randomId(),
                                                Space.EMPTY,
                                                Markers.EMPTY,
                                                J.Identifier.build(
                                                        randomId(),
                                                        Space.EMPTY,
                                                        Markers.EMPTY,
                                                        "SpringJUnit4ClassRunner",
                                                        springJUnit4ClassRunnerType
                                                ),
                                                JLeftPadded.build(J.Identifier.build(randomId(), Space.EMPTY, Markers.EMPTY, "class", null)),
                                                JavaType.Class.build("java.lang.Class")
                                        )
                                )
                        )
                )
        );

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
            List<J.Annotation> keepAnnotations = cd.getLeadingAnnotations().stream().filter(
                    a -> !shouldReplaceAnnotation(a)
            ).collect(Collectors.toList());
            if (keepAnnotations.size() != cd.getLeadingAnnotations().size()) {
                maybeAddImport(extendWithType);
                maybeAddImport(springExtensionType);
                maybeRemoveImport(springRunnerType);
                maybeRemoveImport(springJUnit4ClassRunnerType);
                maybeRemoveImport(runWithType);
                cd = cd.withLeadingAnnotations(keepAnnotations);
                cd = cd.withTemplate(
                        template("@ExtendWith(SpringExtension.class)")
                                .imports("org.junit.jupiter.api.extension.ExtendWith", springExtensionType)
                                .javaParser( JavaParser.fromJavaVersion().dependsOn(Collections.singletonList(
                                        Parser.Input.fromString(
                                                "@Target({ ElementType.TYPE, ElementType.METHOD })\n" +
                                                "@Inherited\n" +
                                                "@Repeatable(Extensions.class)\n" +
                                                "@API(status = STABLE, since = \"5.0\")\n" +
                                                "public @interface ExtendWith {\n" +
                                                "Class<? extends Extension>[] value();\n" +
                                                "}"))).build())
                                .build(),
                        cd.getCoordinates().addAnnotation(
                                // TODO should this use some configuration (similar to styles) for annotation ordering?
                                Comparator.comparing(
                                        a -> TypeUtils.asFullyQualified(a.getType()).getFullyQualifiedName()
                                )
                        )
                );
            }
            return cd;
        }

        private boolean shouldReplaceAnnotation(J.Annotation maybeSpringRunner) {
            return SemanticallyEqual.areEqual(runWithSpringRunnerAnnotation, maybeSpringRunner)
                    || SemanticallyEqual.areEqual(runWithSpringJUnit4ClassRunnerAnnotation, maybeSpringRunner);
        }
    }
}