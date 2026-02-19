package com.gft.test;

import org.junit.internal.AssumptionViolatedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.suite.api.SelectClasses;

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


    @TestMethodOrder(MethodOrderer.MethodName.class)
    @org.junit.jupiter.api.Tag("Fast")
    @org.junit.jupiter.api.Tag("Integration")
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();


        private static final class ClassTempDirExtension implements BeforeAllCallback, AfterAllCallback {
            static Path classTmpRoot;

            @Override
            public void beforeAll(ExtensionContext context) throws Exception {
                classTmpRoot = Files.createTempDirectory("classTmp");
            }

            @Override
            public void afterAll(ExtensionContext context) throws Exception {
                // cleanup
            }
        }

        @RegisterExtension
        static final ClassTempDirExtension classTmp = new ClassTempDirExtension();


        @org.junit.jupiter.api.io.TempDir
        Path tempDir;


        @RegisterExtension
        final TestRuleReplacementExtension testRuleReplacementExtension = new TestRuleReplacementExtension();


        private final class TestRuleReplacementExtension implements BeforeEachCallback, AfterEachCallback {

            @Override
            public void beforeEach(ExtensionContext context) throws Exception {
                String methodName = context.getRequiredTestMethod().getName();
                events.add("customRule-before:" + methodName);
                events.add("chain-outer-before");
                events.add("chain-inner-before");
            }

            @Override
            public void afterEach(ExtensionContext context) throws Exception {
                String methodName = context.getRequiredTestMethod().getName();
                events.add("chain-inner-after");
                events.add("chain-outer-after");
                events.add("customRule-after:" + methodName);
            }
        }


        @BeforeAll
        public static void beforeClass() throws IOException {
            // touching class-level temp dir to force creation
            File root = ClassTempDirExtension.classTmpRoot.toFile();
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
        @org.junit.jupiter.api.Timeout(value=100L, unit=TimeUnit.MILLISECONDS)
        public void test01_expected_and_timeout_together() throws Exception {
            Assertions.assertThrows(IllegalStateException.class, () -> {
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
            File f = Files.createFile(tempDir.resolve("x.txt")).toFile();
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        @Test
        @org.junit.jupiter.api.Tag("WindowsOnly")
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


    @org.junit.jupiter.api.Tag("Slow")
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

        @ParameterizedTest(name = "{index}: concat({0},{1})={2}")
        @MethodSource("data")
        public void test_concat(String left, String right, String expected) {
            assertEquals(expected, left + right);
        }
    }


    @org.junit.platform.suite.api.Suite
    @SelectClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    public static class AllTestsSuiteJUnit4 {
        // empty
    }


    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.IncludeTags({"Fast"})
    @org.junit.platform.suite.api.ExcludeTags({"Slow"})
    @SelectClasses({
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
