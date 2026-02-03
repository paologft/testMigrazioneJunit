package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Second JUnit4 "kitchen sink" class for JUnit4 -> JUnit5 migration tools.
 * Adds additional constructs not always present in the first showcase.
 */
public class Test4 {

    public interface Fast {}
    public interface Slow {}
    public interface Integration {}
    public interface WindowsOnly {}


    @Tag("Fast")
    @Tag("Integration")
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();


        @TempDir
        static File classTmp;


        @TempDir
        File tmp;

        private String testName;


        @BeforeAll
        public static void beforeClass() throws IOException {
            // touching class-level temp dir to force creation
            File root = classTmp;
            assertTrue(root.exists());
        }

        @AfterAll
        public static void afterClass() {
            // cleanup
        }

        @BeforeEach
        public void beforeEach(TestInfo testInfo) {
            testName = testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse(null);
            events.add("before");
            events.add("chain-outer-before");
            events.add("chain-inner-before");
            events.add("customRule-before:" + testName);
        }

        @AfterEach
        public void afterEach() {
            events.add("customRule-after:" + testName);
            events.add("chain-inner-after");
            events.add("chain-outer-after");
            events.add("after");
            assertNotNull(events, "events should never be null");
            events.add("stopwatch-finish:" + testName + ":" + 0L);
        }

        @Disabled("Ignored for demonstration: should become @Disabled in Jupiter")
        @Test
        public void test00_ignored_method() {
            fail("Should never run");
        }

        @Test
        @Timeout(value = 100, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
        public void test01_expected_and_timeout_together() throws Exception {
            Thread.sleep(5L);
            assertThrows(IllegalStateException.class, () -> { throw new IllegalStateException("expected+timeout"); });
        }

        @Test
        public void test02_expected_exception_rule_message() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> { throw new IllegalArgumentException("bad argument"); });
            assertThat(ex.getMessage(), containsString("bad"));
        }

        @Test
        public void test03_temp_folder_rule() throws IOException {
            File f = new File(tmp, "x.txt");
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        @Test
        @Tag("WindowsOnly")
        @EnabledOnOs(OS.WINDOWS)
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            assumeTrue(os.contains("win"), "Run only on Windows");

            // also show explicit assumption violated exception sometimes found in legacy code
            if (!os.contains("win")) {
                throw new org.opentest4j.TestAbortedException("not windows");
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
            assertThat(testName, startsWith("test06_"));
        }

        @Test
        public void test07_rulechain_ordering_observable() {
            // just ensure chain markers are recorded somewhere during execution
            // note: the exact order can be asserted if your tool wants stable output
            assertNotNull(events);
        }
    }


    @Tag("Slow") // -> @Tag("Slow")
    public static class ParameterizedRunnerShowcase {

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


    public static class AllTestsSuiteJUnit4 {
        // empty
    }


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

