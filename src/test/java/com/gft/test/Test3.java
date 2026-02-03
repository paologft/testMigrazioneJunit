package com.gft.test;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.runners.Enclosed;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.*;
import org.junit.runner.Description;
import org.junit.runners.*;
import org.junit.runners.Parameterized.Parameters;
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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Second JUnit4 "kitchen sink" class for JUnit4 -> JUnit5 migration tools.
 * Adds additional constructs not always present in the first showcase.
 */
public class Test3 {

    // =============================================================================================
    // Categories (JUnit4) -> Tags (JUnit5)
    // =============================================================================================
    public interface Fast {}
    public interface Slow {}
    public interface Integration {}
    public interface WindowsOnly {}

    // =============================================================================================
    // 1) Standard tests with many rules and edge patterns
    // =============================================================================================
    @FixMethodOrder(MethodSorters.JVM)                 // -> JUnit5: @TestMethodOrder (often MethodOrderer)
    @Category({Fast.class, Integration.class})         // -> JUnit5: @Tag("Fast") + @Tag("Integration")
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();

        // -------------------- ClassRule (JUnit4) -> Jupiter @TempDir static or extensions --------------------
        @ClassRule
        public static final TemporaryFolder classTmp = new TemporaryFolder();

        // -------------------- Common Rules -> Jupiter equivalents (extensions / assertions / @TempDir / TestInfo) ----
        @Rule
        public final TemporaryFolder tmp = new TemporaryFolder(); // -> @TempDir

        @Rule
        public final TestName testName = new TestName();          // -> TestInfo

        @Rule
        public final ExpectedException thrown = ExpectedException.none(); // -> assertThrows

        @Rule
        public final DisableOnDebug disableOnDebug = new DisableOnDebug(Timeout.seconds(2));
        // -> JUnit5: no direct rule; typically conditional execution + @Timeout

        @Rule
        public final Stopwatch stopwatch = new Stopwatch() {
            @Override protected void finished(long nanos, Description description) {
                events.add("stopwatch-finish:" + description.getMethodName() + ":" + nanos);
            }
        };
        // -> JUnit5: Timing via extensions or assertTimeout; no direct Stopwatch rule.

        @Rule
        public final Verifier verifier = new Verifier() {
            @Override protected void verify() {
                // runs after each test (even if it fails) -> can map to @AfterEach
                assertNotNull(events, "events should never be null");
            }
        };

        // Custom TestRule: typical “legacy” pattern -> in JUnit5 becomes extension/interceptor
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

        // RuleChain to force ordering (JUnit4) -> JUnit5: @Order on @RegisterExtension
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

        // -------------------- Lifecycle (JUnit4) -> JUnit5 @BeforeAll/@AfterAll/@BeforeEach/@AfterEach ----------
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

        // -------------------- Disabled test (JUnit4 @Ignore) -> JUnit5 @Disabled -------------------------------
        @Disabled("Ignored for demonstration: should become @Disabled in Jupiter")
        @Test
        public void test00_ignored_method() {
            fail("Should never run");
        }

        // -------------------- Combined expected + timeout (JUnit4) -> JUnit5 assertThrows + @Timeout ------------
        @Test
        public void test01_expected_and_timeout_together() throws Exception {
            assertThrows(IllegalStateException.class, () -> {
                Thread.sleep(5L);
                throw new IllegalStateException("expected+timeout");
            });
        }

        // -------------------- ExpectedException Rule -> assertThrows -------------------------------------------
        @Test
        public void test02_expected_exception_rule_message() {
            thrown.expect(IllegalArgumentException.class);
            thrown.expectMessage(containsString("bad"));
            throw new IllegalArgumentException("bad argument");
        }

        // -------------------- TemporaryFolder Rule -> @TempDir -------------------------------------------------
        @Test
        public void test03_temp_folder_rule() throws IOException {
            File f = tmp.newFile("x.txt");
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        // -------------------- Assumptions -> Assumptions in Jupiter --------------------------------------------
        @Test
        @Category(WindowsOnly.class) // -> @Tag("WindowsOnly")
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            assumeTrue(os.contains("win"), "Run only on Windows");

            // also show explicit assumption violated exception sometimes found in legacy code
            if (!os.contains("win")) {
                throw new AssumptionViolatedException("not windows");
            }

            assertTrue(true);
        }

        // -------------------- “fail + try/catch” pattern -> assertThrows ----------------------------------------
        @Test
        public void test05_fail_try_catch_pattern() {
            try {
                Integer.parseInt("NaN");
                fail("Expected NumberFormatException");
            } catch (NumberFormatException ex) {
                assertThat(ex.getMessage(), containsString("NaN"));
            }
        }

        // -------------------- TestName Rule -> TestInfo in Jupiter ---------------------------------------------
        @Test
        public void test06_testname_rule() {
            assertThat(testName.getMethodName(), startsWith("test06_"));
        }

        // -------------------- Demonstrate rule ordering side effects -------------------------------------------
        @Test
        public void test07_rulechain_ordering_observable() {
            // just ensure chain markers are recorded somewhere during execution
            // note: the exact order can be asserted if your tool wants stable output
            assertNotNull(events);
        }
    }

    // =============================================================================================
    // 2) Parameterized runner (JUnit4) -> @ParameterizedTest (JUnit5)
    // =============================================================================================
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

    // =============================================================================================
    // 3) Suite runner (JUnit4) -> JUnit Platform Suite (JUnit5)
    // =============================================================================================
    @RunWith(Suite.class)
    @SuiteClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    public static class AllTestsSuiteJUnit4 {
        // empty
    }

    // =============================================================================================
    // 4) Categories runner suite (JUnit4) -> tag-based include/exclude in JUnit5
    // =============================================================================================
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

    // =============================================================================================
    // 5) Class-level ignore (JUnit4) -> @Disabled (JUnit5)
    // =============================================================================================
    @Disabled("Demonstration of @Ignore at class level -> should become @Disabled")
    public static class IgnoredClassExample {

        @Test
        public void willNotRun() {
            fail("Should never run");
        }
    }
}
