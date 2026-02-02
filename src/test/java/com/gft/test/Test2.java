package com.gft.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.hamcrest.MatcherAssert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("FastTests")
public class Test2 {

    public interface FastTests {}
    public interface SlowTests {}
    public interface DatabaseTests {}

    private static final AtomicInteger BEFORE_CLASS_COUNTER = new AtomicInteger(0);

    @BeforeAll
    public static void beforeAllJUnit4() {
        BEFORE_CLASS_COUNTER.incrementAndGet();
    }

    @AfterAll
    public static void afterAllJUnit4() {
        // cleanup
    }

    private List<String> buffer;

    @BeforeEach
    public void setUpJUnit4() {
        buffer = new ArrayList<>();
        buffer.add("init");
    }

    @AfterEach
    public void tearDownJUnit4() {
        buffer.clear();
    }

    @RegisterExtension
    final JUnit4StyleExtensions.ExternalResourceExtension resource = new JUnit4StyleExtensions.ExternalResourceExtension() {
        @Override
        protected void before() {
            buffer.add("resource-before");
        }

        @Override
        protected void after() {
            buffer.add("resource-after");
        }
    };

    @RegisterExtension
    final JUnit4StyleExtensions.ExternalResourceExtension chainOuter = new JUnit4StyleExtensions.ExternalResourceExtension() {
        @Override protected void before() { buffer.add("chain-outer-before"); }
        @Override protected void after() { buffer.add("chain-outer-after"); }
    };

    @RegisterExtension
    final JUnit4StyleExtensions.ExternalResourceExtension chainInner = new JUnit4StyleExtensions.ExternalResourceExtension() {
        @Override protected void before() { buffer.add("chain-inner-before"); }
        @Override protected void after() { buffer.add("chain-inner-after"); }
    };

    @RegisterExtension
    final JUnit4StyleExtensions.TestWatcherExtension watcher = new JUnit4StyleExtensions.TestWatcherExtension() {
        @Override protected void starting(org.junit.jupiter.api.extension.ExtensionContext context) {
            // could log: description.getMethodName()
        }

        @Override protected void failed(Throwable e, org.junit.jupiter.api.extension.ExtensionContext context) {
            // could log failure
        }

        @Override protected void succeeded(org.junit.jupiter.api.extension.ExtensionContext context) {
            // could log success
        }
    };

    @RegisterExtension
    static final JUnit4StyleExtensions.ExternalResourceExtension classResource = new JUnit4StyleExtensions.ExternalResourceExtension() {
        @Override protected void before() {
            // global once-per-class resource init
        }

        @Override protected void after() {
            // global cleanup
        }
    };

    @Disabled("Demonstration of @Ignore at method level")
    @Test
    public void test00_ignored() {
        Assertions.fail("Should never run");
    }

    @Test
    public void test01_assertions_basic() {
        Assertions.assertTrue(buffer.contains("init"), "buffer should contain init");
        Assertions.assertFalse(buffer.contains("X"), "buffer should not contain X");
        Assertions.assertNull(null);
        Assertions.assertNotNull(buffer);

        Assertions.assertEquals(1, buffer.size(), "size");
        Assertions.assertNotEquals(1, 2, "not equals");

        Assertions.assertSame(buffer, buffer, "same ref");
        Assertions.assertNotSame(buffer, new ArrayList<String>(), "different ref");

        Assertions.assertArrayEquals(new int[]{1,2,3}, new int[]{1,2,3});
    }

    @Test
    public void test02_hamcrest_assertThat() {
        MatcherAssert.assertThat("buffer has init", buffer, hasItem("init"));
        MatcherAssert.assertThat("counter", BEFORE_CLASS_COUNTER.get(), greaterThanOrEqualTo(1));
        MatcherAssert.assertThat("string", "hello", allOf(startsWith("he"), endsWith("lo")));
    }

    @Test
    public void test03_assumptions() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getProperty("run.assumption.tests", "true")),
                "Run only when property is set");

        Assumptions.assumeTrue(
                !System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("unknown")
        );

        // if we got here, the assumptions held
        Assertions.assertTrue(true);
    }

    @Test
    @Timeout(50)
    public void test04_timeout_annotation() throws InterruptedException {
        Thread.sleep(10L);
        Assertions.assertTrue(true);
    }

    @Test
    public void test05_expected_exception_annotation() {
        Assertions.assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                throw new IllegalArgumentException("boom");
            }
        });
    }

    @Test
    public void test06_expected_exception_rule() {
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                throw new IllegalStateException("bad state");
            }
        });
        MatcherAssert.assertThat(ex.getMessage(), containsString("state"));
    }

    @TempDir
    Path tmp;

    @Test
    public void test07_temporary_folder_rule() throws IOException {
        File f = tmp.resolve("demo.txt").toFile();
        Assertions.assertTrue(f.createNewFile());
        Assertions.assertTrue(f.exists(), "temp file should exist");
        MatcherAssert.assertThat(f.getName(), endsWith(".txt"));
    }

    @RegisterExtension
    final JUnit4StyleExtensions.ErrorCollectorExtension errors = new JUnit4StyleExtensions.ErrorCollectorExtension();

    @Test
    public void test08_error_collector() {
        errors.checkThat("a", "a", is("a"));
        errors.checkThat("1+1", 1 + 1, is(2));
        errors.checkThat("contains init", buffer, hasItem("init"));
        // test continues even if a check fails
    }

    @Test
    public void test09_manual_exception_assertion() {
        try {
            Integer.parseInt("not-a-number");
            Assertions.fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            MatcherAssert.assertThat(expected.getMessage(), containsString("not-a-number"));
        }
    }

    @Test
    public void test10_test_name_rule(TestInfo testInfo) {
        MatcherAssert.assertThat(testInfo.getTestMethod().get().getName(), startsWith("test10_"));
    }


    public static class ParameterizedExample {

        static Stream<org.junit.jupiter.params.provider.Arguments> data() {
            return Stream.of(
                    org.junit.jupiter.params.provider.Arguments.of("0", 0),
                    org.junit.jupiter.params.provider.Arguments.of("7", 7),
                    org.junit.jupiter.params.provider.Arguments.of("42", 42)
            );
        }

        @BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @ParameterizedTest(name = "{index}: parseInt({0}) = {1}") // -> JUnit5 display names differ
        @MethodSource("data")
        public void parsesIntegers(String input, int expected) {
            Assertions.assertEquals(expected, Integer.parseInt(input));
        }
    }

    public static class TheoriesExample {

        @ParameterizedTest
        @MethodSource("numbers")
        public void absIsNonNegative(int n) {
            Assumptions.assumeTrue(n != Integer.MIN_VALUE, "skip min int edge if desired");
            Assertions.assertTrue(Math.abs(n) >= 0);
        }

        static Stream<Integer> numbers() {
            return Stream.of(-1, 0, 1, 2, 10, 100);
        }

        @ParameterizedTest
        @MethodSource("additionPairs")
        public void additionIsCommutative(int a, int b) {
            Assertions.assertEquals(a + b, b + a);
        }

        static Stream<org.junit.jupiter.params.provider.Arguments> additionPairs() {
            return Stream.of(-1, 0, 1, 2, 10, 100).flatMap(a ->
                    Stream.of(-1, 0, 1, 2, 10, 100).map(b ->
                            org.junit.jupiter.params.provider.Arguments.of(a, b)
                    )
            );
        }
    }

    @Disabled("Demonstration of @Ignore at class level")
    public static class IgnoredClassExample {
        @Test
        public void willNotRun() {
            Assertions.fail("Should never run");
        }
    }
}

class JUnit4StyleExtensions {

    static abstract class ExternalResourceExtension implements org.junit.jupiter.api.extension.BeforeEachCallback, org.junit.jupiter.api.extension.AfterEachCallback, org.junit.jupiter.api.extension.BeforeAllCallback, org.junit.jupiter.api.extension.AfterAllCallback {

        protected void before() {}

        protected void after() {}

        @Override
        public void beforeEach(org.junit.jupiter.api.extension.ExtensionContext context) {
            before();
        }

        @Override
        public void afterEach(org.junit.jupiter.api.extension.ExtensionContext context) {
            after();
        }

        @Override
        public void beforeAll(org.junit.jupiter.api.extension.ExtensionContext context) {
            before();
        }

        @Override
        public void afterAll(org.junit.jupiter.api.extension.ExtensionContext context) {
            after();
        }
    }

    static abstract class TestWatcherExtension implements org.junit.jupiter.api.extension.TestWatcher {

        protected void starting(org.junit.jupiter.api.extension.ExtensionContext context) {}

        protected void failed(Throwable e, org.junit.jupiter.api.extension.ExtensionContext context) {}

        protected void succeeded(org.junit.jupiter.api.extension.ExtensionContext context) {}

        @Override
        public void testDisabled(org.junit.jupiter.api.extension.ExtensionContext context, java.util.Optional<String> reason) {}

        @Override
        public void testSuccessful(org.junit.jupiter.api.extension.ExtensionContext context) {
            succeeded(context);
        }

        @Override
        public void testAborted(org.junit.jupiter.api.extension.ExtensionContext context, Throwable cause) {}

        @Override
        public void testFailed(org.junit.jupiter.api.extension.ExtensionContext context, Throwable cause) {
            failed(cause, context);
        }
    }

    static class ErrorCollectorExtension implements org.junit.jupiter.api.extension.AfterEachCallback {

        private final java.util.List<AssertionError> failures = new java.util.ArrayList<>();

        public <T> void checkThat(String reason, T actual, org.hamcrest.Matcher<? super T> matcher) {
            try {
                MatcherAssert.assertThat(reason, actual, matcher);
            } catch (AssertionError e) {
                failures.add(e);
            }
        }

        @Override
        public void afterEach(org.junit.jupiter.api.extension.ExtensionContext context) {
            if (!failures.isEmpty()) {
                AssertionError aggregated = new AssertionError("ErrorCollector collected " + failures.size() + " failure(s)");
                for (AssertionError e : failures) {
                    aggregated.addSuppressed(e);
                }
                failures.clear();
                throw aggregated;
            }
        }
    }
}
