package com.gft.test;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.MethodName.class)               // -> JUnit5: @TestMethodOrder(...)
@org.junit.jupiter.api.Tag("FastTests")      // -> JUnit5: @Tag("...")
@org.junit.jupiter.api.Timeout(value=250L, unit=TimeUnit.MILLISECONDS)
public class Test1 {

    // --------- Categories (JUnit4) -> Tags (JUnit5) ----------
    public interface FastTests {}
    public interface SlowTests {}
    public interface DatabaseTests {}

    // --------- Static lifecycle (JUnit4) -> @BeforeAll/@AfterAll (JUnit5) ----------
    private static final AtomicInteger BEFORE_CLASS_COUNTER = new AtomicInteger(0);

    @org.junit.jupiter.api.BeforeAll
    public static void beforeAllJUnit4() {
        BEFORE_CLASS_COUNTER.incrementAndGet();
    }

    @org.junit.jupiter.api.AfterAll
    public static void afterAllJUnit4() {
        // cleanup
    }

    // --------- Instance lifecycle (JUnit4) -> @BeforeEach/@AfterEach (JUnit5) ----------
    private List<String> buffer;

    @org.junit.jupiter.api.BeforeEach
    public void setUpJUnit4() {
        buffer = new ArrayList<>();
        buffer.add("init");
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDownJUnit4() {
        buffer.clear();
    }

    @org.junit.jupiter.api.io.TempDir
    Path tempDir;

    @RegisterExtension
    public final InlineResourceExtension resourceExtension = new InlineResourceExtension(this);

    @RegisterExtension
    public final InlineRuleChainExtension chainExtension = new InlineRuleChainExtension(this);

    @RegisterExtension
    public final InlineClassResourceExtension classResourceExtension = new InlineClassResourceExtension();

    @RegisterExtension
    public final InlineTestWatcherExtension watcherExtension = new InlineTestWatcherExtension();

    // --------- Ignored tests/classes (JUnit4) -> @Disabled (JUnit5) ----------
    @org.junit.jupiter.api.Disabled("Demonstration of @Ignore at method level")          // -> JUnit5: @Disabled("...")
    @org.junit.jupiter.api.Test
    public void test00_ignored() {
        fail("Should never run");
    }

    // --------- Basic assertions (JUnit4) ----------
    @org.junit.jupiter.api.Test
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

    // --------- Hamcrest via org.junit.Assert.assertThat (JUnit4) -> in JUnit5 use Hamcrest directly or AssertJ ----------
    @org.junit.jupiter.api.Test
    public void test02_hamcrest_assertThat() {
        assertThat("buffer has init", buffer, hasItem("init"));
        assertThat("counter", BEFORE_CLASS_COUNTER.get(), greaterThanOrEqualTo(1));
        assertThat("string", "hello", allOf(startsWith("he"), endsWith("lo")));
    }

    // --------- Assumptions (JUnit4) -> org.junit.jupiter.api.Assumptions ----------
    @org.junit.jupiter.api.Test
    public void test03_assumptions() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("run.assumption.tests", "true")), "Run only when property is set");

        assumeThat(System.getProperty("os.name", "").toLowerCase(Locale.ROOT),
                not(containsString("unknown")));

        // if we got here, the assumptions held
        assertTrue(true);
    }

    // --------- @Test(timeout=...) (JUnit4) -> @Timeout or assertTimeout in JUnit5 ----------
    @org.junit.jupiter.api.Timeout(value=50L, unit=TimeUnit.MILLISECONDS)
    @org.junit.jupiter.api.Test
    public void test04_timeout_annotation() throws InterruptedException {
        Thread.sleep(10L);
        assertTrue(true);
    }

    // --------- @Test(expected=...) (JUnit4) -> assertThrows in JUnit5 ----------
    @org.junit.jupiter.api.Test
    public void test05_expected_exception_annotation() {
        assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("boom");
        });
    }

    // --------- ExpectedException Rule (JUnit4) -> assertThrows in JUnit5 ----------
    @org.junit.jupiter.api.Test
    public void test06_expected_exception_rule() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("bad state");
        });
        assertThat(ex.getMessage(), containsString("state"));
    }

    // --------- TemporaryFolder Rule (JUnit4) -> @TempDir (JUnit5) ----------
    @org.junit.jupiter.api.Test
    public void test07_temporary_folder_rule() throws IOException {
        File f = Files.createFile(tempDir.resolve("demo.txt")).toFile();
        assertTrue(f.exists(), "temp file should exist");
        assertThat(f.getName(), endsWith(".txt"));
    }

    // --------- ErrorCollector Rule (JUnit4) -> assertAll (JUnit5) ----------
    @org.junit.jupiter.api.Test
    public void test08_error_collector() {
        assertAll(
                () -> assertThat("a", "a", is("a")),
                () -> assertThat("1+1", 1 + 1, is(2)),
                () -> assertThat("contains init", buffer, hasItem("init"))
        );
        // test continues even if a check fails
    }

    // --------- Demonstrate fail + try/catch style often migrated to assertThrows ----------
    @org.junit.jupiter.api.Test
    public void test09_manual_exception_assertion() {
        try {
            Integer.parseInt("not-a-number");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            assertThat(expected.getMessage(), containsString("not-a-number"));
        }
    }

    // --------- Demonstrate TestName Rule (JUnit4) -> TestInfo in JUnit5 ----------
    @org.junit.jupiter.api.Test
    public void test10_test_name_rule(TestInfo testInfo) {
        assertThat(testInfo.getTestMethod().get().getName(), startsWith("test10_"));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Parameterized (JUnit4) -> @ParameterizedTest (JUnit5)
    // -------------------------------------------------------------------------------------------------------------
    public static class ParameterizedExample {

        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"0", 0},
                    {"7", 7},
                    {"42", 42}
            });
        }

        @org.junit.jupiter.api.BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("data")
        public void parsesIntegers(String input, int expected) {
            assertEquals(expected, Integer.parseInt(input));
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Theories (JUnit4) -> usually reworked to parameterized tests or property-based in JUnit5
    // -------------------------------------------------------------------------------------------------------------
    public static class TheoriesExample {

        public static Stream<Integer> numbers() {
            return Stream.of(-1, 0, 1, 2, 10, 100);
        }

        public static Stream<org.junit.jupiter.params.provider.Arguments> numberPairs() {
            List<Integer> vals = Arrays.asList(-1, 0, 1, 2, 10, 100);
            return vals.stream().flatMap(a -> vals.stream().map(b -> org.junit.jupiter.params.provider.Arguments.of(a, b)));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("numbers")
        public void absIsNonNegative(int n) {
            assumeTrue(n != Integer.MIN_VALUE, "skip min int edge if desired");
            assertTrue(Math.abs(n) >= 0);
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("numberPairs")
        public void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Suites + Categories (JUnit4) -> JUnit5: suites via platform suite engine / tags filtering
    // -------------------------------------------------------------------------------------------------------------
    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.SelectClasses({
            Test1.class,
            ParameterizedExample.class,
            TheoriesExample.class
    })
    public static class AllTestsSuite {
        // no code
    }

    @org.junit.platform.suite.api.Suite
    @org.junit.platform.suite.api.SelectClasses({
            Test1.class,
            ParameterizedExample.class
    })
    @org.junit.platform.suite.api.IncludeTags({"FastTests"})
    @org.junit.platform.suite.api.ExcludeTags({"SlowTests"})
    public static class FastOnlySuite {
        // no code
    }

    // -------------------------------------------------------------------------------------------------------------
    // Class-level Ignore (JUnit4) -> @Disabled in JUnit5
    // -------------------------------------------------------------------------------------------------------------
    @org.junit.jupiter.api.Disabled("Demonstration of @Ignore at class level")
    public static class IgnoredClassExample {
        @org.junit.jupiter.api.Test
        public void willNotRun() {
            fail("Should never run");
        }
    }

    public static class InlineClassResourceExtension implements BeforeAllCallback, AfterAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            // global once-per-class resource init
        }

        @Override
        public void afterAll(ExtensionContext context) {
            // global cleanup
        }
    }

    public static class InlineResourceExtension implements BeforeEachCallback, AfterEachCallback {
        private final Test1 owner;

        public InlineResourceExtension(Test1 owner) {
            this.owner = owner;
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            owner.buffer.add("resource-before");
        }

        @Override
        public void afterEach(ExtensionContext context) {
            owner.buffer.add("resource-after");
        }
    }

    public static class InlineRuleChainExtension implements BeforeEachCallback, AfterEachCallback {
        private final Test1 owner;

        public InlineRuleChainExtension(Test1 owner) {
            this.owner = owner;
        }

        @Override
        public void beforeEach(ExtensionContext context) {
            owner.buffer.add("chain-outer-before");
            owner.buffer.add("chain-inner-before");
        }

        @Override
        public void afterEach(ExtensionContext context) {
            owner.buffer.add("chain-inner-after");
            owner.buffer.add("chain-outer-after");
        }
    }

    public static class InlineTestWatcherExtension implements org.junit.jupiter.api.extension.TestWatcher {
        @Override
        public void testSuccessful(ExtensionContext context) {
            // could log success
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            // could log failure
        }
    }
}
