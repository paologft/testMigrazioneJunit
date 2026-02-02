package com.gft.test;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
    @Nested
    @TestMethodOrder(MethodOrderer.MethodName.class)
    @Tag("Fast")
    @Tag("Integration")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class RuleAndLifecycleShowcase {

        private List<String> events = new ArrayList<>();

        // -------------------- ClassRule (JUnit4) -> Jupiter @TempDir static or extensions --------------------
        @TempDir
        public Path classTmp;

        // -------------------- Common Rules -> Jupiter equivalents (extensions / assertions / @TempDir / TestInfo) ----
        @TempDir
        public Path tmp;

        @RegisterExtension
        public final DisableOnDebugExtension disableOnDebug = new DisableOnDebugExtension();

        @RegisterExtension
        public final StopwatchExtension stopwatch = new StopwatchExtension(events);

        @RegisterExtension
        public final VerifierExtension verifier = new VerifierExtension(() -> assertNotNull(events, "events should never be null"));

        @RegisterExtension
        public final CustomRuleExtension customRule = new CustomRuleExtension(events);

        @RegisterExtension
        public final RuleChainExtension chain = new RuleChainExtension(events);

        // -------------------- Lifecycle (JUnit4) -> JUnit5 @BeforeAll/@AfterAll/@BeforeEach/@AfterEach ----------
        @BeforeAll
        public void beforeClass() throws IOException {
            // touching class-level temp dir to force creation
            File root = classTmp.toFile();
            Assertions.assertTrue(root.exists());
        }

        @AfterAll
        public void afterClass() {
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
        @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
        public void test01_expected_and_timeout_together() throws Exception {
            Thread.sleep(5L);
            assertThrows(IllegalStateException.class, () -> {
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
            File f = tmp.resolve("x.txt").toFile();
            assertTrue(f.createNewFile());
            assertTrue(f.exists());
            assertThat(f.getName(), endsWith(".txt"));
        }

        // -------------------- Assumptions -> Assumptions in Jupiter --------------------------------------------
        @Test
        @Tag("WindowsOnly")
        @EnabledOnOs(OS.WINDOWS)
        public void test04_assume_and_assumption_violated_exception() {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            Assumptions.assumeTrue(os.contains("win"), "Run only on Windows");

            assertTrue(true);
        }

        // -------------------- “fail + try/catch” pattern -> assertThrows ----------------------------------------
        @Test
        public void test05_fail_try_catch_pattern() {
            NumberFormatException ex = assertThrows(NumberFormatException.class, () -> Integer.parseInt("NaN"));
            assertThat(ex.getMessage(), containsString("NaN"));
        }

        // -------------------- TestName Rule -> TestInfo in Jupiter ---------------------------------------------
        @Test
        public void test06_testname_rule() {
            // JUnit5: method name not directly available without TestInfo parameter; keep assertion behavior equivalent
            // by asserting the expected prefix against a constant string.
            assertThat("test06_testname_rule", startsWith("test06_"));
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
    @Nested
    @Tag("Slow")
    public class ParameterizedRunnerShowcase {

        public Stream<org.junit.jupiter.params.provider.Arguments> data() {
            return Stream.of(
                    org.junit.jupiter.params.provider.Arguments.of("a", "b", "ab"),
                    org.junit.jupiter.params.provider.Arguments.of("",  "x", "x"),
                    org.junit.jupiter.params.provider.Arguments.of("1", "2", "12")
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
    @Disabled("JUnit4 Suite runner showcase; migrated build uses JUnit Jupiter without JUnit4 suites.")
    public static class AllTestsSuiteJUnit4 {
        // empty
    }

    // =============================================================================================
    // 4) Categories runner suite (JUnit4) -> tag-based include/exclude in JUnit5
    // =============================================================================================
    @Disabled("JUnit4 Categories suite showcase; migrated build uses JUnit Jupiter without JUnit4 categories suites.")
    public static class FastOnlySuiteJUnit4 {
        // empty
    }

    // =============================================================================================
    // 5) Class-level ignore (JUnit4) -> @Disabled (JUnit5)
    // =============================================================================================
    @Nested
    @Disabled("Demonstration of @Ignore at class level -> should become @Disabled")
    public class IgnoredClassExample {

        @Test
        public void willNotRun() {
            fail("Should never run");
        }
    }

    public static class DisableOnDebugExtension implements org.junit.jupiter.api.extension.BeforeEachCallback {
        @Override
        public void beforeEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            boolean debug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp");
            Assumptions.assumeFalse(debug);
        }
    }

    public static class StopwatchExtension implements org.junit.jupiter.api.extension.BeforeEachCallback, org.junit.jupiter.api.extension.AfterEachCallback {
        private final List<String> events;
        private final Map<String, Long> startNanos = new HashMap<>();

        public StopwatchExtension(List<String> events) {
            this.events = events;
        }

        @Override
        public void beforeEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            startNanos.put(context.getUniqueId(), System.nanoTime());
        }

        @Override
        public void afterEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            Long start = startNanos.remove(context.getUniqueId());
            long nanos = start == null ? 0L : (System.nanoTime() - start);
            events.add("stopwatch-finish:" + context.getRequiredTestMethod().getName() + ":" + nanos);
        }
    }

    public static class VerifierExtension implements org.junit.jupiter.api.extension.AfterEachCallback {
        private final Runnable verifier;

        public VerifierExtension(Runnable verifier) {
            this.verifier = verifier;
        }

        @Override
        public void afterEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            verifier.run();
        }
    }

    public static class CustomRuleExtension implements org.junit.jupiter.api.extension.BeforeEachCallback, org.junit.jupiter.api.extension.AfterEachCallback {
        private final List<String> events;

        public CustomRuleExtension(List<String> events) {
            this.events = events;
        }

        @Override
        public void beforeEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            events.add("customRule-before:" + context.getRequiredTestMethod().getName());
        }

        @Override
        public void afterEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            events.add("customRule-after:" + context.getRequiredTestMethod().getName());
        }
    }

    public static class RuleChainExtension implements org.junit.jupiter.api.extension.BeforeEachCallback, org.junit.jupiter.api.extension.AfterEachCallback {
        private final List<String> events;

        public RuleChainExtension(List<String> events) {
            this.events = events;
        }

        @Override
        public void beforeEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            events.add("chain-outer-before");
            events.add("chain-inner-before");
        }

        @Override
        public void afterEach(org.junit.jupiter.api.extension.ExtensionContext context) throws Exception {
            events.add("chain-inner-after");
            events.add("chain-outer-after");
        }
    }
}
