package com.gft.test;

import org.junit.internal.AssumptionViolatedException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    @org.junit.jupiter.api.TestMethodOrder(MethodName.class)                 // -> JUnit5: @TestMethodOrder (often MethodOrderer)
    @org.junit.jupiter.api.Tag("Fast")         // -> JUnit5: @Tag("Fast") + @Tag("Integration")
    @org.junit.jupiter.api.Tag("Integration")         // -> JUnit5: @Tag("Fast") + @Tag("Integration")
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();

        @TempDir
        static Path classTmp;

        @TempDir
        Path tempDir;

        @RegisterExtension
        final LegacyRuleChainExtension legacyRuleChainExtension = new LegacyRuleChainExtension();

        private class LegacyRuleChainExtension implements BeforeEachCallback, AfterEachCallback {
            @Override
            public void beforeEach(ExtensionContext context) {
                events.add("chain-outer-before");
                events.add("chain-inner-before");
                events.add("customRule-before:" + context.getRequiredTestMethod().getName());
            }

            @Override
            public void afterEach(ExtensionContext context) {
                events.add("customRule-after:" + context.getRequiredTestMethod().getName());
                events.add("chain-inner-after");
                events.add("chain-outer-after");
                assertNotNull(events, "events should never be null");
            }
        }

        // -------------------- Lifecycle (JUnit4) -> JUnit5 @BeforeAll/@AfterAll/@BeforeEach/@AfterEach ----------
        @BeforeAll
        public static void beforeClass() throws IOException {
            // touching class-level temp dir to force creation
            File root = classTmp.toFile();
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
        @org.junit.jupiter.api.Timeout(value=100L, unit=java.util.concurrent.TimeUnit.MILLISECONDS)
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
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                throw new IllegalArgumentException("bad argument");
            });
            assertThat(ex.getMessage(), containsString("bad"));
        }

        // -------------------- TemporaryFolder Rule -> @TempDir -------------------------------------------------
        @Test
        public void test03_temp_folder_rule() throws IOException {
            File f = Files.createFile(tempDir.resolve("x.txt")).toFile();
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        // -------------------- Assumptions -> Assumptions in Jupiter --------------------------------------------
        @Test
        @org.junit.jupiter.api.Tag("WindowsOnly") // -> @Tag("WindowsOnly")
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
        public void test06_testname_rule(TestInfo testInfo) {
            assertThat(testInfo.getTestMethod().get().getName(), startsWith("test06_"));
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
    @org.junit.jupiter.api.Tag("Slow") // -> @Tag("Slow")
    public static class ParameterizedRunnerShowcase {

        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"a", "b", "ab"},
                    {"",  "x", "x"},
                    {"1", "2", "12"}
            });
        }

        @BeforeEach
        public void beforeEach() {
            // per-invocation setup
        }

        @ParameterizedTest
        @MethodSource("data")
        public void test_concat(String left, String right, String expected) {
            assertEquals(expected, left + right);
        }
    }

    // =============================================================================================
    // 3) Suite runner (JUnit4) -> JUnit Platform Suite (JUnit5)
    // =============================================================================================
    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.SelectClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    public static class AllTestsSuiteJUnit4 {
        // empty
    }

    // =============================================================================================
    // 4) Categories runner suite (JUnit4) -> tag-based include/exclude in JUnit5
    // =============================================================================================
    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.SelectClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    @org.junit.platform.suite.api.IncludeTags({"Fast"})
    @org.junit.platform.suite.api.ExcludeTags({"Slow"})
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
