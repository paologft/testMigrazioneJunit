package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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


    @Nested
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();

        @TempDir
        Path tempDir;

        @BeforeAll
        public static void beforeClass() throws IOException {
            // touching class-level temp dir to force creation
            File root = new File(System.getProperty("java.io.tmpdir"));
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

        @Test
        public void test01_expected_and_timeout_together() {
            assertTimeout(Duration.ofMillis(100), () -> {
                Thread.sleep(5L);
                throw new IllegalStateException("expected+timeout");
            });
        }

        @Test
        public void test02_expected_exception_rule_message() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                throw new IllegalArgumentException("bad argument");
            });
            assertThat(ex.getMessage(), containsString("bad"));
        }

        @Test
        public void test03_temp_folder_rule() throws IOException {
            Path file = Files.createFile(tempDir.resolve("x.txt"));
            assertTrue(Files.exists(file));
            assertThat(file.getFileName().toString(), endsWith(".txt"));
        }

        @Test
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
        public void test06_testname_rule(TestInfo testInfo) {
            assertThat(testInfo.getTestMethod().get().getName(), startsWith("test06_"));
        }

        @Test
        public void test07_rulechain_ordering_observable() {
            // just ensure chain markers are recorded somewhere during execution
            // note: the exact order can be asserted if your tool wants stable output
            assertNotNull(events);
        }
    }


    @Nested
    public static class ParameterizedRunnerShowcase {

        @MethodSource("data")
        @ParameterizedTest(name = "{index}: concat({0},{1})={2}")
        public void test_concat(String left, String right, String expected) {
            assertEquals(expected, left + right);
        }

        @BeforeEach
        public void beforeEach() {
            // per-invocation setup
        }

        public static Stream<Arguments> data() {
            return Stream.of(
                    Arguments.of("a", "b", "ab"),
                    Arguments.of("",  "x", "x"),
                    Arguments.of("1", "2", "12")
            );
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
