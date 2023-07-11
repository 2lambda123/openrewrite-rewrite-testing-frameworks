/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.stream.Collectors;

public class RemoveTryCatchBlocksFromUnitTests extends Recipe {
    private static final MethodMatcher ASSERT_FAIL_MATCHER = new MethodMatcher("org.junit.Assert fail(..)");
    private static final List<String> TEST_PATTERNS = Arrays.asList(
            "@org.junit.jupiter.api.Test",
            "@org.junit.jupiter.api.RepeatedTest",
            "@org.junit.jupiter.params.ParameterizedTest"
    );

    private static final List<AnnotationMatcher> MATCHERS = TEST_PATTERNS.stream().map(AnnotationMatcher::new).collect(Collectors.toList());

    @Override
    public String getDisplayName() {
        return "Unit test should throw exceptions instead of using `try-catch` blocks";
    }

    @Override
    public String getDescription() {
        return "When the code under test in a unit test throws an exception, the test itself fails. " +
                "Therefore, there is no need to surround the tested code with a try-catch.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-3658");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveTryCatchBlocksFromUnitsTestsVisitor();
    }

    private static class RemoveTryCatchBlocksFromUnitsTestsVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitTry(J.Try try_, ExecutionContext ctx) {
            J.Try t = (J.Try) super.visitTry(try_, ctx);

            if (t.getCatches().get(0).getBody().getStatements().stream().noneMatch(
                    s -> s instanceof J.MethodInvocation && ASSERT_FAIL_MATCHER.matches((J.MethodInvocation) s)
            )) {
                return t;
            }

            // replace method body
            maybeRemoveImport("org.junit.Assert");
            maybeAddImport("org.junit.jupiter.api.Assertions");
            return JavaTemplate.builder("Assertions.assertDoesNotThrow(() -> {#{any()}})")
                    .imports("org.junit.jupiter.api.Assertions")
                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "junit-jupiter-api-5.9"))
                    .build()
                    .apply(getCursor(), t.getCoordinates().replace(), t.getBody());
        }
    }
}