package com.gft.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.MethodName.class)
@org.junit.jupiter.api.Tag("FastTests")
@org.junit.jupiter.api.Timeout(value=250L, unit=TimeUnit.MILLISECONDS)
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

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    @RegisterExtension
    final InlineExternalResource resource = new InlineExternalResource(() -> buffer.add("resource-before"), () -> buffer.add("resource-after"));

    @RegisterExtension
    final InlineWatcher watcher = new InlineWatcher();

    @RegisterExtension
    final InlineRuleChain chain = new InlineRuleChain(
            () -> buffer.add("chain-outer-before"),
            () -> buffer.add("chain-outer-after"),
            () -> buffer.add("chain-inner-before"),
            () -> buffer.add("chain-inner-after")
    );

    @RegisterExtension
    static final InlineClassResource classResource = new InlineClassResource();

    @Disabled("Demonstration of @Ignore at method level")
    @Test
    public void test00_ignored() {
        fail("Should never run");
    }

    @Test
    public void test01_assertions_basic() {
        assertTrue(buffer.contains("init"), "buffer should contain init");
        assertFalse(buffer.contains("X"), "buffer should not contain X");
        assertNull(null);
        assertNotNull(buffer);

        assertEquals(1, buffer.size(), "size");
        assertNotEquals(1, 2, "not equals");

        assertSame(buffer, buffer, "same ref");
        assertNotSame(buffer, new ArrayList<String>(), "different ref");

        assertArrayEquals(new int[]{1,2,3}, new int[]{1,2,3});
    }

    @Test
    public void test02_hamcrest_assertThat() {
        assertThat("buffer has init", buffer, hasItem("init"));
        assertThat("counter", BEFORE_CLASS_COUNTER.get(), greaterThanOrEqualTo(1));
        assertThat("string", "hello", allOf(startsWith("he"), endsWith("lo")));
    }

    @Test
    public void test03_assumptions() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("run.assumption.tests", "true")),
                "Run only when property is set");

        assumeThat(System.getProperty("os.name", "").toLowerCase(Locale.ROOT),
                not(containsString("unknown")));

        // if we got here, the assumptions held
        assertTrue(true);
    }

    @org.junit.jupiter.api.Timeout(value=50L, unit=java.util.concurrent.TimeUnit.MILLISECONDS)
    @Test
    public void test04_timeout_annotation() throws InterruptedException {
        Thread.sleep(10L);
        assertTrue(true);
    }

    @Test
    public void test05_expected_exception_annotation() {
        assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("boom");
        });
    }

    @Test
    public void test06_expected_exception_rule() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("bad state");
        });
        assertThat(ex.getMessage(), containsString("state"));
    }

    @Test
    public void test07_temporary_folder_rule() throws IOException {
        File f = Files.createFile(tempDir.resolve("demo.txt")).toFile();
        assertTrue(f.exists(), "temp file should exist");
        assertThat(f.getName(), endsWith(".txt"));
    }

    @Test
    public void test08_error_collector() {
        assertAll(
                () -> assertThat("a", "a", is("a")),
                () -> assertThat("1+1", 1 + 1, is(2)),
                () -> assertThat("contains init", buffer, hasItem("init"))
        );
        // test continues even if a check fails
    }

    @Test
    public void test09_manual_exception_assertion() {
        try {
            Integer.parseInt("not-a-number");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            assertThat(expected.getMessage(), containsString("not-a-number"));
        }
    }

    @Test
    public void test10_test_name_rule(TestInfo testInfo) {
        assertThat(testInfo.getTestMethod().get().getName(), startsWith("test10_"));
    }


    public static class ParameterizedExample {

        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"0", 0},
                    {"7", 7},
                    {"42", 42}
            });
        }

        static Stream<org.junit.jupiter.params.provider.Arguments> dataStream() {
            return StreamSupport.stream(data().spliterator(), false)
                    .map(a -> org.junit.jupiter.params.provider.Arguments.of(a[0], a[1]));
        }

        @BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("dataStream")
        public void parsesIntegers(String input, int expected) {
            assertEquals(expected, Integer.parseInt(input));
        }
    }

    public static class TheoriesExample {

        public static int[] numbers = new int[]{-1, 0, 1, 2, 10};

        public static int special = 100;

        static Stream<Integer> numbersStream() {
            return Stream.concat(Arrays.stream(numbers).boxed(), Stream.of(special));
        }

        static Stream<org.junit.jupiter.params.provider.Arguments> cartesianNumbersStream() {
            List<Integer> values = numbersStream().collect(Collectors.toList());
            return values.stream().flatMap(a -> values.stream().map(b -> org.junit.jupiter.params.provider.Arguments.of(a, b)));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("numbersStream")
        public void absIsNonNegative(int n) {
            assumeTrue(n != Integer.MIN_VALUE, "skip min int edge if desired");
            assertTrue(Math.abs(n) >= 0);
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("cartesianNumbersStream")
        public void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }
    }

    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.SelectClasses({
            Test2.class,
            ParameterizedExample.class,
            TheoriesExample.class
    })
    public static class AllTestsSuite {
        // no code
    }

    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.SelectClasses({
            Test2.class,
            ParameterizedExample.class
    })
    @org.junit.platform.suite.api.IncludeTags({"FastTests"})
    @org.junit.platform.suite.api.ExcludeTags({"SlowTests"})
    public static class FastOnlySuite {
        // no code
    }


    @Disabled("Demonstration of @Ignore at class level")
    public static class IgnoredClassExample {
        @Test
        public void willNotRun() {
            fail("Should never run");
        }
    }

    static class InlineExternalResource implements BeforeEachCallback, AfterEachCallback {
        private final Runnable before;
        private final Runnable after;

        InlineExternalResource(Runnable before, Runnable after) {
            this.before = before;
            this.after = after;
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            before.run();
        }

        @Override
        public void afterEach(ExtensionContext context) {
            after.run();
        }
    }

    static class InlineRuleChain implements BeforeEachCallback, AfterEachCallback {
        private final Runnable outerBefore;
        private final Runnable outerAfter;
        private final Runnable innerBefore;
        private final Runnable innerAfter;

        InlineRuleChain(Runnable outerBefore, Runnable outerAfter, Runnable innerBefore, Runnable innerAfter) {
            this.outerBefore = outerBefore;
            this.outerAfter = outerAfter;
            this.innerBefore = innerBefore;
            this.innerAfter = innerAfter;
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            outerBefore.run();
            innerBefore.run();
        }

        @Override
        public void afterEach(ExtensionContext context) {
            innerAfter.run();
            outerAfter.run();
        }
    }

    static class InlineClassResource implements BeforeAllCallback, AfterAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            // global once-per-class resource init
        }

        @Override
        public void afterAll(ExtensionContext context) {
            // global cleanup
        }
    }

    static class InlineWatcher implements TestWatcher {
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            // could log failure
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            // could log success
        }
    }

    static final class StreamSupport {
        private StreamSupport() {}

        static <T> Stream<T> stream(java.util.Spliterator<T> spliterator, boolean parallel) {
            return java.util.stream.StreamSupport.stream(spliterator, parallel);
        }
    }
}
