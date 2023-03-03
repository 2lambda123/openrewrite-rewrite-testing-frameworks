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
package org.openrewrite.java.testing.mockito;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AnyToNullableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "mockito-core-3.12.4")
            .logCompilationWarningsAndErrors(true))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.testing.mockito")
            .build()
            .activateRecipes("org.openrewrite.java.testing.mockito.AnyToNullable"));
    }

    @Test
    void replaceAnyClassWithNullableClass() {
        //language=java
        rewriteRun(
          java("""
            class Example {
                String greet(Object obj) {
                    return "Hello " + obj;
                }
            }
            """),
          java(
            """
              import static org.mockito.Mockito.mock;
              import static org.mockito.Mockito.when;
              import static org.mockito.Mockito.any;

              class MyTest {
                   void test() {
                      Example example = mock(Example.class);
                      when(example.greet(any(Object.class))).thenReturn("Hello world");
                   }
              }
              """,
            """
              import static org.mockito.ArgumentMatchers.nullable;
              import static org.mockito.Mockito.mock;
              import static org.mockito.Mockito.when;
                            
              class MyTest {
                   void test() {
                      Example example = mock(Example.class);
                      when(example.greet(nullable(Object.class))).thenReturn("Hello world");
                   }
              }
              """
          )
        );
    }

}


