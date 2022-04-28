/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.testing.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class AssertEqualsNullToAssertNullTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .logCompilationWarningsAndErrors(true)
        .classpath("junit-jupiter-api")
        .build()

    override val recipe: Recipe
        get() = AssertEqualsNullToAssertNull()

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/200")
    @Suppress("ConstantConditions", "SimplifiableAssertion")
    @Test
    fun simplifyToAssertNull() = assertChanged(
        before = """
            import static org.junit.jupiter.api.Assertions.assertEquals;
            
            public class Test {
                void test() {
                    String s = null;
                    assertEquals(s, null);
                    assertEquals(null, s);
                }
            }
        """,
        after = """
            import static org.junit.jupiter.api.Assertions.assertNull;
            
            public class Test {
                void test() {
                    String s = null;
                    assertNull(s);
                    assertNull(s);
                }
            }
        """,
    )

    @Issue("https://github.com/openrewrite/rewrite-testing-frameworks/issues/200")
    @Suppress("ConstantConditions", "SimplifiableAssertion")
    @Test
    fun preserveStyleOfStaticImportOrNot() = assertChanged(
        before = """
            import org.junit.jupiter.api.Assertions;
            
            public class Test {
                void test() {
                    String s = null;
                    Assertions.assertEquals(s, null);
                    Assertions.assertEquals(null, s);
                }
            }
        """,
        after = """
            import org.junit.jupiter.api.Assertions;
            
            public class Test {
                void test() {
                    String s = null;
                    Assertions.assertNull(s);
                    Assertions.assertNull(s);
                }
            }
        """,
    )
}