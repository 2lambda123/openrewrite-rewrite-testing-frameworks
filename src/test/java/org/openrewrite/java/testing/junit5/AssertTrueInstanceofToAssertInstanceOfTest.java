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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class AssertTrueInstanceofToAssertInstanceOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "junit-jupiter-api-5.9", "junit-4.13"))
          .recipe(new AssertTrueInstanceofToAssertInstanceOf());
    }

    @DocumentExample
    @Test
    void testJUnit5() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.jupiter.api.Assertions.assertTrue;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertTrue(list instanceof Iterable);
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.jupiter.api.Assertions.assertInstanceOf;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertInstanceOf(Iterable.class, list);
                  }
              }
              """
          ));
    }

    @Test
    void testJUnit5WithReason() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.jupiter.api.Assertions.assertTrue;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertTrue(list instanceof Iterable, "Not instance of Iterable");
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.jupiter.api.Assertions.assertInstanceOf;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertInstanceOf(Iterable.class, list, "Not instance of Iterable");
                  }
              }
              """
          ));
    }

    @Test
    void testJUnit4() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.Assert.assertTrue;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertTrue(list instanceof Iterable);
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.jupiter.api.Assertions.assertInstanceOf;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertInstanceOf(Iterable.class, list);
                  }
              }
              """
          ));
    }

    @Test
    void testJUnit4WithReason() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.Assert.assertTrue;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertTrue("Not instance of Iterable", list instanceof Iterable);
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;
              import java.util.ArrayList;
              import java.util.List;
              
              import static org.junit.jupiter.api.Assertions.assertInstanceOf;
              
              class ATest {
                  @Test
                  void testJUnit5() {
                      List<String> list = new ArrayList<>();
                      assertInstanceOf(Iterable.class, list, "Not instance of Iterable");
                  }
              }
              """
          ));
    }
}
