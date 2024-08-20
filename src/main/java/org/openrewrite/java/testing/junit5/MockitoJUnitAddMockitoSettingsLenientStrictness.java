/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

public class MockitoJUnitAddMockitoSettingsLenientStrictness extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add `@MockitoSettings(strictness = Strictness.LENIENT)` when migration to JUnit 5";
    }

    @Override
    public String getDescription() {
        return "Add `@MockitoSettings(strictness = Strictness.LENIENT)` when migration to JUnit 5.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>("org.mockito.junit.jupiter.MockitoExtension", true),
                        Preconditions.not(new UsesType<>("org.mockito.junit.jupiter.MockitoSettings", true))
                ), new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        return super.visitClassDeclaration(classDecl, ctx);
                        // TODO
                    }
                });
    }
}
