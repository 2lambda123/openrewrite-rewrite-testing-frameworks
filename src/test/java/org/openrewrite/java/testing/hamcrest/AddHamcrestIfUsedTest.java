package org.openrewrite.java.testing.hamcrest;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class AddHamcrestIfUsedTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/hamcrest.yml", "org.openrewrite.java.testing.hamcrest.AddHamcrestIfUsed")
          .parser(JavaParser.fromJavaVersion().classpath("hamcrest"));
    }

    @Test
    void addHamcrest() {
        rewriteRun(
          spec -> defaults(spec),
          mavenProject(
            "project",
            srcTestJava(
              java("class Foo { org.hamcrest.Matchers matchers; }")
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                </project>
                """,
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.hamcrest</groupId>
                            <artifactId>hamcrest</artifactId>
                            <version>2.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void differentClass() {
        rewriteRun(
          spec -> defaults(spec),
          mavenProject(
            "project",
            srcTestJava(
              java("class Foo { org.hamcrest.Matcher matchers; }")
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                </project>
                """
            )
          )
        );
    }

    @Test
    void alreadyPresent() {
        rewriteRun(
          spec -> defaults(spec),
          mavenProject(
            "project",
            srcTestJava(
              java("class Foo { org.hamcrest.Matchers matchers; }")
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.hamcrest</groupId>
                            <artifactId>hamcrest</artifactId>
                            <version>2.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void alreadyTransitive() {
        rewriteRun(
          spec -> defaults(spec),
          mavenProject(
            "project",
            srcTestJava(
              java("class Foo { org.hamcrest.Matchers matchers; }")
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-test</artifactId>
                            <version>3.2.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
