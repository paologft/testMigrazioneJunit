package com.gft.test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Category;
import org.junit.experimental.runners.Enclosed;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.junit.runners.model.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.Assume.assumeTrue;

/**
 * Second JUnit4 "kitchen sink" class for JUnit4 -> JUnit5 migration tools.
 * Adds additional constructs not always present in the first showcase.
 */
@RunWith(Enclosed.class)
public class Test4 {

    public interface Fast {}
    public interface Slow {}
    public interface Integration {}
    public interface WindowsOnly {}


    @org.junit.FixMethodOrder(MethodSorters.JVM)
    @Category({Fast.class, Integration.class})
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();


        @org.junit.ClassRule
        public static final org.junit.rules.TemporaryFolder classTmp = new org.junit.rules.TemporaryFolder();


        @org.junit.Rule
        public final org.junit.rules.TemporaryFolder tmp = new org.junit.rules.TemporaryFolder();

        @org.junit.Rule
        public final org.junit.rules.TestName testName = new org.junit.rules.TestName();

        @org.junit.Rule
        public final org.junit.rules.ExpectedException thrown = org.junit.rules.ExpectedException.none();

        @org.junit.Rule
        public final org.junit.rules.DisableOnDebug disableOnDebug = new org.junit.rules.DisableOnDebug(org.junit.rules.Timeout.seconds(2));


        @org.junit.Rule
        public final org.junit.rules.Stopwatch stopwatch = new org.junit.rules.Stopwatch() {
            @Override protected void finished(long nanos, Description description) {
                events.add("stopwatch-finish:" + description.getMethodName() + ":" + nanos);
            }
        };


        @org.junit.Rule
        public final org.junit.rules.Verifier verifier = new org.junit.rules.Verifier() {
            @Override protected void verify() {
                assertNotNull(events, "events should never be null");
            }
        };


        @org.junit.Rule
        public final org.junit.rules.TestRule customRule = new org.junit.rules.TestRule() {
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


        @org.junit.Rule
        public final org.junit.rules.RuleChain chain = org.junit.rules.RuleChain
                .outerRule(new org.junit.rules.ExternalResource() {
                    @Override protected void before() { events.add("chain-outer-before"); }
                    @Override protected void after()  { events.add("chain-outer-after");  }
                })
                .around(new org.junit.rules.ExternalResource() {
                    @Override protected void before() { events.add("chain-inner-before"); }
                    @Override protected void after()  { events.add("chain-inner-after");  }
                });


        @BeforeAll
        public static void beforeClass() throws IOException {
            // touching class-level temp dir to force creation
            File root = classTmp.getRoot();
            assertTrue(root.exists());
        }

        @AfterAll
        public static void afterClass() {
            // cleanup
        }

        @BeforeEach
        public void beforeEach() {
            events.add("before");
        }

        @AfterEach
        public void afterEach() {
            events.add("after");
        }

        @Disabled("Ignored for demonstration: should become @Disabled in Jupiter")
        @Test
        public void test00_ignored_method() {
            fail("Should never run");
        }

        @org.junit.Test(expected = IllegalStateException.class, timeout = 100L)
        public void test01_expected_and_timeout_together() throws Exception {
            Thread.sleep(5L);
            throw new IllegalStateException("expected+timeout");
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
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        @Test
        @Category(WindowsOnly.class)
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            assumeTrue("Run only on Windows", os.contains("win"));

            // also show explicit assumption violated exception sometimes found in legacy code
            if (!os.contains("win")) {
                throw new AssumptionViolatedException("not windows");
            }

            assertTrue(true);
        }

        @Test
        public void test05_fail_try_catch_pattern() {
            try {
                Integer.parseInt("NaN");
                fail("Expected NumberFormatException");
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
            assertNotNull(events);
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

        @BeforeEach
        public void beforeEach() {
            // per-invocation setup
        }

        @Test
        public void test_concat() {
            assertEquals(expected, left + right);
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


    @Disabled("Demonstration of @Ignore at class level -> should become @Disabled")
    public static class IgnoredClassExample {

        @Test
        public void willNotRun() {
            fail("Should never run");
        }
    }
}
