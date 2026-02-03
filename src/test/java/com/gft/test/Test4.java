package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;
import java.util.Locale;
import java.util.stream.Stream;

public class Test4 {

    public interface Fast {}
    public interface Slow {}
    public interface Integration {}
    public interface WindowsOnly {}

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();

        @TempDir
        static Path classTmp;

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

        @Disabled("Ignored for demonstration: should become @Disabled in Jupiter")
        @Test
        public void test00_ignored_method() {
            fail("Should never run");
        }

        @Test
        public void test01_expected_and_timeout_together() throws Exception {
            assertThrows(IllegalStateException.class, () -> {
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
        public void test03_temp_folder_rule(@TempDir Path tempDir) throws IOException {
            Path filePath = tempDir.resolve("x.txt");
            Files.createFile(filePath);
            File f = filePath.toFile();
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        @Test
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            assumeTrue("Run only on Windows", os.contains("win"));

            // also show explicit assumption violated exception sometimes found in legacy code
            if (!os.contains("win")) {
                throw new Assumptions.AssumptionViolatedException("not windows");
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
            assertThat(testInfo.getDisplayName(), startsWith("test06_"));
        }

        @Test
        public void test07_rulechain_ordering_observable() {
            assertNotNull(events);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class ParameterizedRunnerShowcase {

        static Stream<Arguments> data() {
            return Stream.of(
                Arguments.of("a", "b", "ab"),
                Arguments.of("", "x", "x"),
                Arguments.of("1", "2", "12")
            );
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
