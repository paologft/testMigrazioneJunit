package com.gft.test;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
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


    @org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.MethodName.class)
    @org.junit.jupiter.api.Tag("Fast")
    @org.junit.jupiter.api.Tag("Integration")
    public static class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();


        private static Path classTmpRoot;

        @org.junit.jupiter.api.io.TempDir
        Path tempDir;

        @RegisterExtension
        final DisableOnDebugExtension disableOnDebug = new DisableOnDebugExtension(2);

        @RegisterExtension
        final StopwatchExtension stopwatch = new StopwatchExtension();

        @RegisterExtension
        final VerifierExtension verifier = new VerifierExtension();

        @RegisterExtension
        final CustomRuleExtension customRule = new CustomRuleExtension();

        @RegisterExtension
        final RuleChainLikeExtension chain = new RuleChainLikeExtension();


        @org.junit.jupiter.api.BeforeAll
        public static void beforeClass(@org.junit.jupiter.api.io.TempDir Path classTmp) throws IOException {
            // touching class-level temp dir to force creation
            classTmpRoot = classTmp;
            File root = classTmpRoot.toFile();
            assertTrue(root.exists());
        }

        @org.junit.jupiter.api.AfterAll
        public static void afterClass() {
            // cleanup
        }

        @org.junit.jupiter.api.BeforeEach
        public void beforeEach() {
            events.add("before");
        }

        @org.junit.jupiter.api.AfterEach
        public void afterEach() {
            events.add("after");
        }

        @org.junit.jupiter.api.Disabled("Ignored for demonstration: should become @Disabled in Jupiter")
        @org.junit.jupiter.api.Test
        public void test00_ignored_method() {
            fail("Should never run");
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Timeout(value = 100L, unit = TimeUnit.MILLISECONDS)
        public void test01_expected_and_timeout_together() throws Exception {
            assertThrows(IllegalStateException.class, () -> {
                Thread.sleep(5L);
                throw new IllegalStateException("expected+timeout");
            });
        }

        @org.junit.jupiter.api.Test
        public void test02_expected_exception_rule_message() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                throw new IllegalArgumentException("bad argument");
            });
            assertThat(ex.getMessage(), containsString("bad"));
        }

        @org.junit.jupiter.api.Test
        public void test03_temp_folder_rule() throws IOException {
            File f = Files.createFile(tempDir.resolve("x.txt")).toFile();
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Tag("WindowsOnly")
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            assumeTrue(os.contains("win"), "Run only on Windows");

            // also show explicit assumption violated exception sometimes found in legacy code
            if (!os.contains("win")) {
                throw new org.opentest4j.TestAbortedException("not windows");
            }

            assertTrue(true);
        }

        @org.junit.jupiter.api.Test
        public void test05_fail_try_catch_pattern() {
            try {
                Integer.parseInt("NaN");
                fail("Expected NumberFormatException");
            } catch (NumberFormatException ex) {
                assertThat(ex.getMessage(), containsString("NaN"));
            }
        }

        @org.junit.jupiter.api.Test
        public void test06_testname_rule(TestInfo testInfo) {
            assertThat(testInfo.getTestMethod().get().getName(), startsWith("test06_"));
        }

        @org.junit.jupiter.api.Test
        public void test07_rulechain_ordering_observable() {
            // just ensure chain markers are recorded somewhere during execution
            // note: the exact order can be asserted if your tool wants stable output
            assertNotNull(events);
        }

        private class DisableOnDebugExtension implements BeforeEachCallback {
            private final long timeoutSeconds;

            private DisableOnDebugExtension(long timeoutSeconds) {
                this.timeoutSeconds = timeoutSeconds;
            }

            @Override
            public void beforeEach(ExtensionContext context) throws Exception {
                if (java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp")) {
                    context.publishReportEntry("disableOnDebug", "debug detected");
                    throw new org.opentest4j.TestAbortedException("Disabled on debug with timeoutSeconds=" + timeoutSeconds);
                }
            }
        }

        private class StopwatchExtension implements AfterTestExecutionCallback {
            @Override
            public void afterTestExecution(ExtensionContext context) throws Exception {
                Optional<Long> start = context.getStore(ExtensionContext.Namespace.create(getClass(), context.getUniqueId())).get("startNanos", Long.class) == null
                        ? Optional.empty()
                        : Optional.of(context.getStore(ExtensionContext.Namespace.create(getClass(), context.getUniqueId())).get("startNanos", Long.class));
                long startNanos = start.orElseGet(System::nanoTime);
                long nanos = System.nanoTime() - startNanos;
                events.add("stopwatch-finish:" + context.getRequiredTestMethod().getName() + ":" + nanos);
            }
        }

        private class VerifierExtension implements AfterEachCallback {
            @Override
            public void afterEach(ExtensionContext context) throws Exception {
                assertNotNull(events, "events should never be null");
            }
        }

        private class CustomRuleExtension implements BeforeEachCallback, AfterEachCallback {
            @Override
            public void beforeEach(ExtensionContext context) throws Exception {
                events.add("customRule-before:" + context.getRequiredTestMethod().getName());
            }

            @Override
            public void afterEach(ExtensionContext context) throws Exception {
                events.add("customRule-after:" + context.getRequiredTestMethod().getName());
            }
        }

        private class RuleChainLikeExtension implements BeforeEachCallback, AfterEachCallback {
            @Override
            public void beforeEach(ExtensionContext context) throws Exception {
                events.add("chain-outer-before");
                events.add("chain-inner-before");
                context.getStore(ExtensionContext.Namespace.create(getClass(), context.getUniqueId())).put("startNanos", System.nanoTime());
            }

            @Override
            public void afterEach(ExtensionContext context) throws Exception {
                events.add("chain-inner-after");
                events.add("chain-outer-after");
            }
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

        static Stream<org.junit.jupiter.params.provider.Arguments> dataArgs() {
            return data().stream().map(a -> org.junit.jupiter.params.provider.Arguments.of(a[0], a[1], a[2]));
        }

        @org.junit.jupiter.api.BeforeEach
        public void beforeEach() {
            // per-invocation setup
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("dataArgs")
        public void test_concat(String left, String right, String expected) {
            assertEquals(expected, left + right);
        }
    }


    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.SelectClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    public static class AllTestsSuiteJUnit4 {
        // empty
    }


    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.IncludeTags({"Fast"})
    @org.junit.platform.suite.api.ExcludeTags({"Slow"})
    @org.junit.platform.suite.api.SelectClasses({
            RuleAndLifecycleShowcase.class,
            ParameterizedRunnerShowcase.class
    })
    public static class FastOnlySuiteJUnit4 {
        // empty
    }


    @org.junit.jupiter.api.Disabled("Demonstration of @Ignore at class level -> should become @Disabled")
    public static class IgnoredClassExample {

        @org.junit.jupiter.api.Test
        public void willNotRun() {
            fail("Should never run");
        }
    }
}
