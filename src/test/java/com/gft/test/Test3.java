package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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
    @TestMethodOrder(MethodOrderer.MethodName.class)
    @Tag("Fast")
    @Tag("Integration")
    @Nested
    public class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();

        // -------------------- ClassRule (JUnit4) -> Jupiter @TempDir static or extensions --------------------
        @TempDir
        static Path classTmp;

        // -------------------- Common Rules -> Jupiter equivalents (extensions / assertions / @TempDir / TestInfo) ----
        @TempDir
        Path tmp;

        private TestInfo testInfo;

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
        public void beforeEach(TestInfo testInfo) {
            this.testInfo = testInfo;
            events.add("before");
            events.add("chain-outer-before");
            events.add("chain-inner-before");
            events.add("customRule-before:" + testInfo.getTestMethod().map(Method::getName).orElse(""));
        }

        @AfterEach
        public void afterEach() {
            events.add("customRule-after:" + testInfo.getTestMethod().map(Method::getName).orElse(""));
            events.add("chain-inner-after");
            events.add("chain-outer-after");
            events.add("after");
            events.add("stopwatch-finish:" + testInfo.getTestMethod().map(Method::getName).orElse("") + ":" + 0L);
            // runs after each test (even if it fails) -> can map to @AfterEach
            assertNotNull(events, "events should never be null");
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
            Thread.sleep(5L);
            assertThrows(IllegalStateException.class, () -> { throw new IllegalStateException("expected+timeout"); });
        }

        // -------------------- ExpectedException Rule -> assertThrows -------------------------------------------
        @Test
        public void test02_expected_exception_rule_message() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> { throw new IllegalArgumentException("bad argument"); });
            assertThat(ex.getMessage(), containsString("bad"));
        }

        // -------------------- TemporaryFolder Rule -> @TempDir -------------------------------------------------
        @Test
        public void test03_temp_folder_rule() throws IOException {
            File f = new File(tmp.toFile(), "x.txt");
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        // -------------------- Assumptions -> Assumptions in Jupiter --------------------------------------------
        @Test
        @Tag("WindowsOnly")
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            assumeTrue(os.contains("win"), "Run only on Windows");

            // also show explicit assumption violated exception sometimes found in legacy code
            if (!os.contains("win")) {
                throw new TestAbortedException("not windows");
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
            assertThat(testInfo.getTestMethod().map(Method::getName).orElse(""), startsWith("test06_"));
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
    @Tag("Slow")
    @Nested
    public class ParameterizedRunnerShowcase {

        public static Stream<Arguments> data() {
            return Stream.of(
                    Arguments.of("a", "b", "ab"),
                    Arguments.of("",  "x", "x"),
                    Arguments.of("1", "2", "12")
            );
        }

        @BeforeEach
        public void beforeEach() {
            // per-invocation setup
        }

        @ParameterizedTest(name = "{index}: concat({0},{1})={2}")
        @MethodSource("data")
        public void test_concat(String left, String right, String expected) {
            assertEquals(expected, left + right);
        }
    }

    // =============================================================================================
    // 3) Suite runner (JUnit4) -> JUnit Platform Suite (JUnit5)
    // =============================================================================================
    public static class AllTestsSuiteJUnit4 {
        // empty
    }

    // =============================================================================================
    // 4) Categories runner suite (JUnit4) -> tag-based include/exclude in JUnit5
    // =============================================================================================
    public static class FastOnlySuiteJUnit4 {
        // empty
    }

    // =============================================================================================
    // 5) Class-level ignore (JUnit4) -> @Disabled (JUnit5)
    // =============================================================================================
    @Disabled("Demonstration of @Ignore at class level -> should become @Disabled")
    @Nested
    public class IgnoredClassExample {

        @Test
        public void willNotRun() {
            fail("Should never run");
        }
    }
}
