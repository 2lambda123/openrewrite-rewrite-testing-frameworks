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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.testing.jmockit.JMockitTestUtils.setParserSettings;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

class JMockitMockUpToMockitoTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        setParserSettings(spec, "jmockit-1.22", "junit-4.13.2");
    }

    @DocumentExample
    @Test
    void mockUpStaticMethodTest() {
        //language=java
        rewriteRun(
          java(
            """
              import mockit.Mock;
              import mockit.MockUp;
              import static org.junit.Assert.assertEquals;
              import org.junit.Test;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      new MockUp<MyClazz>() {
              
                          @Mock
                          public int staticMethod() {
                              return 1024;
                          }
              
                          @Mock
                          public int staticMethod(int v) {
                              return 128;
                          }
                      };
                      assertEquals(1024, MockUpTest.MyClazz.staticMethod());
                      assertEquals(128, MockUpTest.MyClazz.staticMethod(0));
                  }
              
                  public static class MyClazz {
                      public static int staticMethod() {
                          return 0;
                      }
              
                      public static int staticMethod(int v) {
                          return 1;
                      }
                  }
              }
              """, """
              import static org.junit.Assert.assertEquals;
              import static org.mockito.ArgumentMatchers.*;
              import static org.mockito.Mockito.*;
              
              import org.junit.Test;
              import org.mockito.MockedStatic;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      try (MockedStatic mockStaticMockUpTest_MyClazz = mockStatic(MockUpTest.MyClazz.class)) {
                          mockStaticMockUpTest_MyClazz.when(() -> MockUpTest.MyClazz.staticMethod()).thenAnswer(invocation -> {
                              return 1024;
                          });
                          mockStaticMockUpTest_MyClazz.when(() -> MockUpTest.MyClazz.staticMethod(anyInt())).thenAnswer(invocation -> {
                              return 128;
                          });
                          assertEquals(1024, MockUpTest.MyClazz.staticMethod());
                          assertEquals(128, MockUpTest.MyClazz.staticMethod(0));
                      }
                  }
              
                  public static class MyClazz {
                      public static int staticMethod() {
                          return 0;
                      }
              
                      public static int staticMethod(int v) {
                          return 1;
                      }
                  }
              }
              """));
    }

    @Test
    void mockUpInstanceMethodTest() {
        //language=java
        rewriteRun(
          spec -> spec.afterTypeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              package com.foo;
              public static class MyClazz {
                  public String getMsg() {
                      return "msg";
                  }
              
                  public String getMsg(String echo) {
                      return echo;
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.foo.MyClazz;
              import org.junit.Test;
              import mockit.Mock;
              import mockit.MockUp;
              import static org.junit.Assert.assertEquals;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      new MockUp<MyClazz>() {
                          @Mock
                          public String getMsg() {
                              return "mockMsg";
                          }
                          @Mock
                          public String getMsg(String echo) {
                              return "mockEchoMsg";
                          }
                      };
                      assertEquals("mockMsg", new MyClazz().getMsg());
                      assertEquals("mockEchoMsg", new MyClazz().getMsg("echo"));
                  }
              }
              """, """
              import com.foo.MyClazz;
              import org.junit.Test;
              import org.mockito.MockedConstruction;
              import static org.junit.Assert.assertEquals;
              import static org.mockito.AdditionalAnswers.delegatesTo;
              import static org.mockito.ArgumentMatchers.*;
              import static org.mockito.Mockito.*;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      MyClazz mockObjMyClazz = mock(MyClazz.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                      doAnswer(invocation -> {
                          return "mockMsg";
                      }).when(mockObjMyClazz).getMsg();
                      doAnswer(invocation -> {
                          return "mockEchoMsg";
                      }).when(mockObjMyClazz).getMsg(nullable(String.class));
                      try (MockedConstruction mockConsMyClazz = mockConstructionWithAnswer(MyClazz.class, delegatesTo(mockObjMyClazz))) {
                          assertEquals("mockMsg", new MyClazz().getMsg());
                          assertEquals("mockEchoMsg", new MyClazz().getMsg("echo"));
                      }
                  }
              }
              """)
        );
    }

    @Test
    void mockUpInnerStatementTest() {
        //language=java
        rewriteRun(
          java(
            """
              import mockit.Mock;
              import mockit.MockUp;
              
              import org.junit.Test;
              import static org.junit.Assert.assertEquals;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      new MockUp<MyClazz>() {
                          final String msg = "newMsg";
              
                          @Mock
                          public String getMsg() {
                              return msg;
                          }
                      };
              
                      // Should ignore the newClass statement
                      new Runnable() {
                          @Override
                          public void run() {
                              System.out.println("run");
                          }
                      };
                      assertEquals("newMsg", new MyClazz().getMsg());
                  }
              
                  public static class MyClazz {
                      public String getMsg() {
                          return "msg";
                      }
                  }
              }
              """, """
              import org.junit.Test;
              import org.mockito.MockedConstruction;
              
              import static org.junit.Assert.assertEquals;
              import static org.mockito.AdditionalAnswers.delegatesTo;
              import static org.mockito.Mockito.*;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      final String msg = "newMsg";
                      MockUpTest.MyClazz mockObjMockUpTest_MyClazz = mock(MockUpTest.MyClazz.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                      doAnswer(invocation -> {
                          return msg;
                      }).when(mockObjMockUpTest_MyClazz).getMsg();
                      try (MockedConstruction mockConsMockUpTest_MyClazz = mockConstructionWithAnswer(MockUpTest.MyClazz.class, delegatesTo(mockObjMockUpTest_MyClazz))) {
              
                          // Should ignore the newClass statement
                          new Runnable() {
                              @Override
                              public void run() {
                                  System.out.println("run");
                              }
                          };
                          assertEquals("newMsg", new MyClazz().getMsg());
                      }
                  }
              
                  public static class MyClazz {
                      public String getMsg() {
                          return "msg";
                      }
                  }
              }
              """));
    }

    @Test
    void mockUpVoidTest() {
        //language=java
        rewriteRun(
          java(
            """
              import mockit.Mock;
              import mockit.MockUp;
              import static org.junit.Assert.assertEquals;
              import org.junit.Test;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      new MockUp<MockUpClass>() {
                          @Mock
                          public void changeMsg() {
                              MockUpClass.Save.msg = "mockMsg";
                          }
              
                          @Mock
                          public void changeText(String text) {
                              MockUpClass.Save.text = "mockText";
                          }
                      };
              
                      assertEquals("mockMsg", new MockUpClass().getMsg());
                      assertEquals("mockText", new MockUpClass().getText());
                  }
              
                  public static class MockUpClass {
                      public static class Save {
                          public static String msg = "msg";
                          public static String text = "text";
                      }
              
                      public final String getMsg() {
                          changeMsg();
                          return Save.msg;
                      }
              
                      public void changeMsg() {
                          Save.msg = "newMsg";
                      }
              
                      public String getText() {
                          changeText("newText");
                          return Save.text;
                      }
              
                      public static void changeText(String text) {
                          Save.text = text;
                      }
                  }
              }
              """,
            """
              import static org.junit.Assert.assertEquals;
              import static org.mockito.AdditionalAnswers.delegatesTo;
              import static org.mockito.ArgumentMatchers.*;
              import static org.mockito.Mockito.*;
              
              import org.junit.Test;
              import org.mockito.MockedConstruction;
              import org.mockito.MockedStatic;
              
              public class MockUpTest {
                  @Test
                  public void test() {
                      MockUpTest.MockUpClass mockObjMockUpTest_MockUpClass = mock(MockUpTest.MockUpClass.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                      doAnswer(invocation -> {
                          MockUpClass.Save.msg = "mockMsg";
                          return null;
                      }).when(mockObjMockUpTest_MockUpClass).changeMsg();
                      try (MockedStatic mockStaticMockUpTest_MockUpClass = mockStatic(MockUpTest.MockUpClass.class);MockedConstruction mockConsMockUpTest_MockUpClass = mockConstructionWithAnswer(MockUpTest.MockUpClass.class, delegatesTo(mockObjMockUpTest_MockUpClass))) {
                          mockStaticMockUpTest_MockUpClass.when(() -> MockUpTest.MockUpClass.changeText(nullable(String.class))).thenAnswer(invocation -> {
                              String text = (String) invocation.getArgument(0);
                              MockUpClass.Save.text = "mockText";
                              return null;
                          });
              
                          assertEquals("mockMsg", new MockUpClass().getMsg());
                          assertEquals("mockText", new MockUpClass().getText());
                      }
                  }
              
                  public static class MockUpClass {
                      public static class Save {
                          public static String msg = "msg";
                          public static String text = "text";
                      }
              
                      public final String getMsg() {
                          changeMsg();
                          return Save.msg;
                      }
              
                      public void changeMsg() {
                          Save.msg = "newMsg";
                      }
              
                      public String getText() {
                          changeText("newText");
                          return Save.text;
                      }
              
                      public static void changeText(String text) {
                          Save.text = text;
                      }
                  }
              }
              """));
    }

    @Test
    public void mockUpAtSetUpWithoutTearDownTest() {
        rewriteRun(
          java(
            """
              import org.junit.Before;
              import org.junit.Test;
              import mockit.Mock;
              import mockit.MockUp;
              import static org.junit.Assert.assertEquals;
              
              public class MockUpTest {
                  @Before
                  public void setUp() {
                      new MockUp<MyClazz>() {
                          @Mock
                          public String getMsg() {
                              return "mockMsg";
                          }
                      };
                  }
              
                  @Test
                  public void test() {
                      assertEquals("mockMsg", new MyClazz().getMsg());
                  }
              
                  public static class MyClazz {
                      public String getMsg() {
                          return "msg";
                      }
                  }
              }
              """,
            """
              import org.junit.After;
              import org.junit.Before;
              import org.junit.Test;
              import org.mockito.MockedConstruction;
              import static org.junit.Assert.assertEquals;
              import static org.mockito.AdditionalAnswers.delegatesTo;
              import static org.mockito.Mockito.*;
              
              public class MockUpTest {
                  private MockedConstruction mockConsMockUpTest_MyClazz;
              
                  @Before
                  public void setUp() {
                      MockUpTest.MyClazz mockObjMockUpTest_MyClazz = mock(MockUpTest.MyClazz.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                      doAnswer(invocation -> {
                          return "mockMsg";
                      }).when(mockObjMockUpTest_MyClazz).getMsg();
                      mockConsMockUpTest_MyClazz = mockConstructionWithAnswer(MockUpTest.MyClazz.class, delegatesTo(mockObjMockUpTest_MyClazz));
                  }
              
                  @After
                  public void tearDown() {
                      mockConsMockUpTest_MyClazz.closeOnDemand();
                  }
              
                  @Test
                  public void test() {
                      assertEquals("mockMsg", new MyClazz().getMsg());
                  }
              
                  public static class MyClazz {
                      public String getMsg() {
                          return "msg";
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    public void mockUpAtSetUpWithTearDownTest() {
        rewriteRun(
          java(
            """
              import org.junit.Before;
              import org.junit.After;
              import org.junit.Test;
              import mockit.Mock;
              import mockit.MockUp;
              import static org.junit.Assert.assertEquals;
              
              public class MockUpTest {
                  @Before
                  public void setUp() {
                      new MockUp<MyClazz>() {
                          @Mock
                          public String getMsg() {
                              return "mockMsg";
                          }
              
                          @Mock
                          public String getStaticMsg() {
                              return "mockStaticMsg";
                          }
                      };
                  }
              
                  @After
                  public void tearDown() {
                  }
              
                  @Test
                  public void test() {
                      assertEquals("mockMsg", new MyClazz().getMsg());
                      assertEquals("mockStaticMsg", MyClazz.getStaticMsg());
                  }
              
                  public static class MyClazz {
                      public String getMsg() {
                          return "msg";
                      }
              
                      public static String getStaticMsg() {
                          return "staticMsg";
                      }
                  }
              }
              """,
            """
              import org.junit.Before;
              import org.junit.After;
              import org.junit.Test;
              import org.mockito.MockedConstruction;
              import org.mockito.MockedStatic;
              import static org.junit.Assert.assertEquals;
              import static org.mockito.AdditionalAnswers.delegatesTo;
              import static org.mockito.Mockito.*;
              
              public class MockUpTest {
                  private MockedConstruction mockConsMockUpTest_MyClazz;
                  private MockedStatic mockStaticMockUpTest_MyClazz;
              
                  @Before
                  public void setUp() {
                      MockUpTest.MyClazz mockObjMockUpTest_MyClazz = mock(MockUpTest.MyClazz.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                      doAnswer(invocation -> {
                          return "mockMsg";
                      }).when(mockObjMockUpTest_MyClazz).getMsg();
                      mockConsMockUpTest_MyClazz = mockConstructionWithAnswer(MockUpTest.MyClazz.class, delegatesTo(mockObjMockUpTest_MyClazz));
                      mockStaticMockUpTest_MyClazz = mockStatic(MockUpTest.MyClazz.class);
                      mockStaticMockUpTest_MyClazz.when(() -> MockUpTest.MyClazz.getStaticMsg()).thenAnswer(invocation -> {
                          return "mockStaticMsg";
                      });
                  }
              
                  @After
                  public void tearDown() {
                      mockStaticMockUpTest_MyClazz.closeOnDemand();
                      mockConsMockUpTest_MyClazz.closeOnDemand();
                  }
              
                  @Test
                  public void test() {
                      assertEquals("mockMsg", new MyClazz().getMsg());
                      assertEquals("mockStaticMsg", MyClazz.getStaticMsg());
                  }
              
                  public static class MyClazz {
                      public String getMsg() {
                          return "msg";
                      }
              
                      public static String getStaticMsg() {
                          return "staticMsg";
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    public void mockUpWithParamsTest() {
        rewriteRun(
          java(
            """
              import mockit.Mock;
              import mockit.MockUp;
              import org.junit.Test;
              
              import static org.junit.Assert.assertEquals;
              
              public class MockUpTest {
                  @Test
                  public void init() {
                      new MockUp<MyClazz>() {
                          @Mock
                          public String getMsg(String foo, String bar, String unused) {
                              return foo + bar;
                          }
                      };
                      assertEquals("foobar", new MyClazz().getMsg("foo", "bar", "unused"));
                  }
              
                  public static class MyClazz {
                      public String getMsg(String foo, String bar, String unused) {
                          return "msg";
                      }
                  }
              }
              """,
            """
              import org.junit.Test;
              import org.mockito.MockedConstruction;
              
              import static org.junit.Assert.assertEquals;
              import static org.mockito.AdditionalAnswers.delegatesTo;
              import static org.mockito.ArgumentMatchers.*;
              import static org.mockito.Mockito.*;
              
              public class MockUpTest {
                  @Test
                  public void init() {
                      MockUpTest.MyClazz mockObjMockUpTest_MyClazz = mock(MockUpTest.MyClazz.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
                      doAnswer(invocation -> {
                          String foo = (String) invocation.getArgument(0);
                          String bar = (String) invocation.getArgument(1);
                          return foo + bar;
                      }).when(mockObjMockUpTest_MyClazz).getMsg(nullable(String.class), nullable(String.class), nullable(String.class));
                      try (MockedConstruction mockConsMockUpTest_MyClazz = mockConstructionWithAnswer(MockUpTest.MyClazz.class, delegatesTo(mockObjMockUpTest_MyClazz))) {
                          assertEquals("foobar", new MyClazz().getMsg("foo", "bar", "unused"));
                      }
                  }
              
                  public static class MyClazz {
                      public String getMsg(String foo, String bar, String unused) {
                          return "msg";
                      }
                  }
              }
              """
          )
        );
    }
}
