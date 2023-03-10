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
package org.openrewrite.java.testing.assertj;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("NewClassNamingConvention")
class JUnitFailToAssertJFailTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "junit-jupiter-api-5.9+"))
          .recipe(new JUnitFailToAssertJFail());
    }

    @Test
    void singleStaticMethodNoMessage() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;

              import static org.junit.jupiter.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      fail();
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;

              import static org.assertj.core.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      fail("");
                  }
              }
              """
          )
        );
    }

    @Test
    void singleStaticMethodWithMessage() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;

              import static org.junit.jupiter.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      fail("This should fail");
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;

              import static org.assertj.core.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      fail("This should fail");
                  }
              }
              """
          )
        );
    }

    @Test
    void singleStaticMethodWithMessageAndCause() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;

              import static org.junit.jupiter.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      Throwable t = new Throwable();
                      fail("This should fail", t);
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;

              import static org.assertj.core.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      Throwable t = new Throwable();
                      fail("This should fail", t);
                  }
              }
              """
          )
        );
    }

    @Test
    void singleStaticMethodWithCause() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;

              import static org.junit.jupiter.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      Throwable t = new Throwable();
                      fail(t);
                      fail(new Throwable());
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;

              import static org.assertj.core.api.Assertions.fail;

              public class MyTest {
                  @Test
                  public void test() {
                      Throwable t = new Throwable();
                      fail("", t);
                      fail("", new Throwable());
                  }
              }
              """
          )
        );
    }

    @Test
    void inlineReference() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;

              public class MyTest {
                  @Test
                  public void test() {
                      org.junit.jupiter.api.Assertions.fail();
                      org.junit.jupiter.api.Assertions.fail("This should fail");
                      org.junit.jupiter.api.Assertions.fail("This should fail", new Throwable());
                      org.junit.jupiter.api.Assertions.fail(new Throwable());
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;

              import static org.assertj.core.api.Assertions.fail;
              
              public class MyTest {
                  @Test
                  public void test() {
                      fail("");
                      fail("This should fail");
                      fail("This should fail", new Throwable());
                      fail("", new Throwable());
                  }
              }
              """
          )
        );
    }

    @Test
    void mixedReferences() {
        //language=java
        rewriteRun(
          java(
            """
              import org.junit.jupiter.api.Test;
              
              import static org.junit.jupiter.api.Assertions.fail;
              
              public class MyTest {
                  @Test
                  public void test() {
                      fail();
                      org.junit.jupiter.api.Assertions.fail("This should fail");
                      fail("This should fail", new Throwable());
                      org.junit.jupiter.api.Assertions.fail(new Throwable());
                  }
              }
              """,
            """
              import org.junit.jupiter.api.Test;

              import static org.assertj.core.api.Assertions.fail;
              
              public class MyTest {
                  @Test
                  public void test() {
                      fail("");
                      fail("This should fail");
                      fail("This should fail", new Throwable());
                      fail("", new Throwable());
                  }
              }
              """
          )
        );
    }
}
