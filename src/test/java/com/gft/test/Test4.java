<file path="pom.xml">
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.gft.test</groupId>
    <artifactId>test4</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <dependencies>
        <!-- JUnit 4 for legacy tests -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.13.2</version>
            <scope>test</scope>
        </dependency>

        <!-- JUnit Jupiter API and Engine for JUnit 5 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>

        <!-- JUnit Vintage Engine to run legacy JUnit 4 tests -->
        <dependency>
            <groupId>org.junit.vintage</groupId>
            <artifactId>junit-vintage-engine</artifactId>
            <version>5.10.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0-M9</version>
                <configuration>
                    <useModulePath>false</useModulePath>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
</file>
<file path="src/test/java/com/gft/test/Test4.java">
package com.gft.test;

import org.junit.*;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.*;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

// JUnit 5 imports
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import java.time.Duration;

/**
 * Second JUnit4 "kitchen sink" class for JUnit4 -> JUnit5 migration tools.
 * Adds additional constructs not always present in the first showcase.
 */
public class Test4 {

    public interface Fast {}
    public interface Slow {}
    public interface Integration {}
    public interface WindowsOnly {}

    // JUnit5 migrated tests
    @BeforeAll
    public static void beforeClass() {
        // nothing
    }

    @AfterAll
    public static void afterClass() {
        // nothing
    }

    @BeforeEach
    public void beforeEach() {
        // nothing
    }

    @AfterEach
    public void afterEach() {
        // nothing
    }

    @Disabled("Ignored for demonstration: should become @Disabled in Jupiter")
    @Test
    public void test00_ignored_method() {
        Assertions.fail("Should never run");
    }

    @Test
    public void test01_expected_and_timeout_together() {
        Assertions.assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
            Assertions.assertThrows(IllegalStateException.class, () -> {
                Thread.sleep(5L);
                throw new IllegalStateException("expected+timeout");
            });
        });
    }

    @Test
    public void test04_assume_and_assumption_violated_exception() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Assumptions.assumeTrue("Run only on Windows", os.contains("win"));

        // also show explicit assumption violated exception sometimes found in legacy code
        if (!os.contains("win")) {
            throw new AssumptionViolatedException("not windows");
        }

        Assertions.assertTrue(true);
    }

    @Test
    public void test05_fail_try_catch_pattern() {
        try {
            Integer.parseInt("NaN");
            Assertions.fail("Expected NumberFormatException");
        } catch (NumberFormatException ex) {
            assertThat(ex.getMessage(), containsString("NaN"));
        }
    }
}

// Legacy JUnit4 tests
@RunWith(Enclosed.class)
class LegacyTest4 {

    @FixMethodOrder(MethodSorters.JVM)
    @Category({Fast.class, Integration.class})
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();

        @ClassRule
        public static final TemporaryFolder classTmp = new TemporaryFolder();

        @Rule
        public final TemporaryFolder tmp = new TemporaryFolder();

        @Rule
        public final TestName testName = new TestName();

        @Rule
        public final ExpectedException thrown = ExpectedException.none();

        @Rule
        public final DisableOnDebug disableOnDebug = new DisableOnDebug(Timeout.seconds(2));

        @Rule
        public final Stopwatch stopwatch = new Stopwatch() {
            @Override protected void finished(long nanos, Description description) {
                events.add("stopwatch-finish:" + description.getMethodName() + ":" + nanos);
            }
        };

        @Rule
        public final Verifier verifier = new Verifier() {
            @Override protected void verify() {
                Assertions.assertNotNull("events should never be null", events);
            }
        };

        @Rule
        public final TestRule customRule = new TestRule() {
            @Override
            public Statement apply(final Statement base, final Description description) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        events.add("customRule-before:" + description.getMethodName());
                        try {
                            base.evaluate();
                        } finally {
                            events.add("customRule-after:" + description.getMethodName());
                        }
                    }
                };
            }
        };

        @Rule
        public final RuleChain chain = RuleChain
                .outerRule(new ExternalResource() {
                    @Override protected void before() { events.add("chain-outer-before"); }
                    @Override protected void after()  { events.add("chain-outer-after");  }
                })
                .around(new ExternalResource() {
                    @Override protected void before() { events.add("chain-inner-before"); }
                    @Override protected void after()  { events.add("chain-inner-after");  }
                });

        @BeforeClass
        public static void beforeClass() throws IOException {
            // touching class-level temp dir to force creation
            File root = classTmp.getRoot();
            Assertions.assertTrue(root.exists());
        }

        @AfterClass
        public static void afterClass() {
            // cleanup
        }

        @Before
        public void beforeEach() {
            events.add("before");
        }

        @After
        public void afterEach() {
            events.add("after");
        }

        @Ignore("Ignored for demonstration: should become @Disabled")
        @Test
        public void test00_ignored_method() {
            Assertions.fail("Should never run");
        }

        @Test
        public void test01_expected_and_timeout_together() {
            Assertions.assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
                Assertions.assertThrows(IllegalStateException.class, () -> {
                    Thread.sleep(5L);
                    throw new IllegalStateException("expected+timeout");
                });
            });
        }

        @Test
        public void test02_expected_exception_rule_message() {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage(containsString("bad"));
            throw new IllegalArgumentException("bad argument");
        }

        @Test
        public void test03_temp_folder_rule() throws IOException {
            File f = tmp.newFile("x.txt");
            Assertions.assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        @Test
        @Category(WindowsOnly.class)
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            Assumptions.assumeTrue("Run only on Windows", os.contains("win"));

            // also show explicit assumption violated exception sometimes found in legacy code
            if (!os.contains("win")) {
                throw new AssumptionViolatedException("not windows");
            }

            Assertions.assertTrue(true);
        }

        @Test
        public void test05_fail_try_catch_pattern() {
            try {
                Integer.parseInt("NaN");
                Assertions.fail("Expected NumberFormatException");
            } catch (NumberFormatException ex) {
                assertThat(ex.getMessage(), containsString("NaN"));
            }
        }

        @Test
        public void test06_testname_rule() {
            assertThat(testName.getMethodName(), startsWith("test06_"));
        }

        @Test
        public void test07_rulechain_ordering_observable() {
            // just ensure chain markers are recorded somewhere during execution
            // note: the exact order can be asserted if your tool wants stable output
            Assertions.assertNotNull(events);
        }
    }

    @RunWith(Parameterized.class)
    @Category(Slow.class) // -> @Tag("Slow")
    public static class ParameterizedRunnerShowcase {

        @Parameters(name = "{index}: concat({0},{1})={2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"a", "b", "ab"},
                    {"",  "x", "x"},
                    {"1", "2", "12"}
            });
        }

        @Parameterized.Parameter(0)
        public String left;

        @Parameterized.Parameter(1)
        public String right;

        @Parameterized.Parameter(2)
        public String expected;

        @Before
        public void beforeEach() {
            // per-invocation setup
        }

        @Test
        public void test_concat() {
            Assertions.assertEquals(expected, left + right);
        }
    }

    @RunWith(Suite.class)
    @SuiteClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    public static class AllTestsSuiteJUnit4 {
        // empty
    }

    @RunWith(Categories.class)
    @Categories.IncludeCategory(Fast.class)
    @Categories.ExcludeCategory(Slow.class)
    @SuiteClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    public static class FastOnlySuiteJUnit4 {
        // empty
    }

    @Ignore("Demonstration of @Ignore at class level -> should become @Disabled")
    public static class IgnoredClassExample {

        @Test
        public void willNotRun() {
            Assertions.fail("Should never run");
        }
    }
}
</file>
