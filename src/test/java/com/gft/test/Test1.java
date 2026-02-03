package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.Disabled;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)               // -> JUnit5: @TestMethodOrder(...)
@Tag("FastTests")      // -> JUnit5: @Tag("...")
public class Test1 {

    // --------- Categories (JUnit4) -> Tags (JUnit5) ----------
    public interface FastTests {}
    public interface SlowTests {}
    public interface DatabaseTests {}

    // --------- Static lifecycle (JUnit4) -> @BeforeAll/@AfterAll (JUnit5) ----------
    private static final AtomicInteger BEFORE_CLASS_COUNTER = new AtomicInteger(0);

    @BeforeAll
    public static void beforeAllJUnit4() {
        BEFORE_CLASS_COUNTER.incrementAndGet();
    }

    @AfterAll
    public static void afterAllJUnit4() {
        // cleanup
    }

    // --------- Instance lifecycle (JUnit4) -> @BeforeEach/@AfterEach (JUnit5) ----------
    private List<String> buffer;

    @BeforeEach
    public void setUpJUnit4() {
        buffer = new ArrayList<>();
        buffer.add("init");
        buffer.add("resource-before");
        buffer.add("chain-outer-before");
        buffer.add("chain-inner-before");
    }

    @AfterEach
    public void tearDownJUnit4() {
        buffer.add("chain-inner-after");
        buffer.add("chain-outer-after");
        buffer.add("resource-after");
        buffer.clear();
    }

    // --------- Rules (JUnit4) -> Extensions / assertions / TempDir (JUnit5) ----------
    // @Rule
    // public final TestName testName = new TestName();             // -> JUnit5: TestInfo injection

    // @Rule
    // public final TemporaryFolder tmp = new TemporaryFolder();    // -> JUnit5: @TempDir

    // @Rule
    // public final ErrorCollector errors = new ErrorCollector();   // -> JUnit5: assertAll / multiple assertions

    // @Rule
    // public final ExpectedException thrown = ExpectedException.none(); // -> JUnit5: assertThrows

    // @Rule
    // public final Timeout globalTimeout = Timeout.millis(250);    // -> JUnit5: assertTimeout / @Timeout

    // @Rule
    // public final ExternalResource resource = new ExternalResource() { // -> JUnit5: @BeforeEach/@AfterEach or extensions
    //     @Override
    //     protected void before() {
    //         buffer.add("resource-before");
    //     }
    //
    //     @Override
    //     protected void after() {
    //         buffer.add("resource-after");
    //     }
    // };

    // @Rule
    // public final TestWatcher watcher = new TestWatcher() {       // -> JUnit5: TestWatcher extension
    //     @Override protected void starting(Description description) {
    //         // could log: description.getMethodName()
    //     }
    //
    //     @Override protected void failed(Throwable e, Description description) {
    //         // could log failure
    //     }
    //
    //     @Override protected void succeeded(Description description) {
    //         // could log success
    //     }
    // };

    // @Rule
    // public final RuleChain chain = RuleChain
    //         .outerRule(new ExternalResource() {
    //             @Override protected void before() { buffer.add("chain-outer-before"); }
    //             @Override protected void after() { buffer.add("chain-outer-after"); }
    //         })
    //         .around(new ExternalResource() {
    //             @Override protected void before() { buffer.add("chain-inner-before"); }
    //             @Override protected void after() { buffer.add("chain-inner-after"); }
    //         });

    // --------- ClassRule (JUnit4) -> extensions / static fixtures ----------
    // @ClassRule
    // public static final ExternalResource classResource = new ExternalResource() {
    //     @Override protected void before() {
    //         // global once-per-class resource init
    //     }
    //
    //     @Override protected void after() {
    //         // global cleanup
    //     }
    // };

    // --------- Ignored tests/classes (JUnit4) -> @Disabled (JUnit5) ----------
    @Disabled("Demonstration of @Ignore at method level")          // -> JUnit5: @Disabled("...")
    @Test
    public void test00_ignored() {
        fail("Should never run");
    }

    // --------- Basic assertions (JUnit4) ----------
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

    // --------- Hamcrest via org.junit.Assert.assertThat (JUnit4) -> in JUnit5 use Hamcrest directly or AssertJ ----------
    @Test
    public void test02_hamcrest_assertThat() {
        org.hamcrest.MatcherAssert.assertThat("buffer has init", buffer, hasItem("init"));
        org.hamcrest.MatcherAssert.assertThat("counter", BEFORE_CLASS_COUNTER.get(), greaterThanOrEqualTo(1));
        org.hamcrest.MatcherAssert.assertThat("string", "hello", allOf(startsWith("he"), endsWith("lo")));
    }

    // --------- Assumptions (JUnit4) -> org.junit.jupiter.api.Assumptions ----------
    @Test
    public void test03_assumptions() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("run.assumption.tests", "true")),
                () -> "Run only when property is set");

        assumeThat(System.getProperty("os.name", "").toLowerCase(Locale.ROOT),
                not(containsString("unknown")));

        // if we got here, the assumptions held
        assertTrue(true);
    }

    // --------- @Test(timeout=...) (JUnit4) -> @Timeout or assertTimeout in JUnit5 ----------
    @Test
    public void test04_timeout_annotation() throws InterruptedException {
        Thread.sleep(10L);
        assertTrue(true);
    }

    // --------- @Test(expected=...) (JUnit4) -> assertThrows in JUnit5 ----------
    @Test
    public void test05_expected_exception_annotation() {
        assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("boom");
        });
    }

    // --------- ExpectedException Rule (JUnit4) -> assertThrows in JUnit5 ----------
    @Test
    public void test06_expected_exception_rule() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("bad state");
        });
        org.hamcrest.MatcherAssert.assertThat(ex.getMessage(), containsString("state"));
    }

    // --------- TemporaryFolder Rule (JUnit4) -> @TempDir (JUnit5) ----------
    @Test
    public void test07_temporary_folder_rule() throws IOException {
        Path f = Files.createTempFile("demo", ".txt");
        File file = f.toFile();
        assertTrue(file.exists(), "temp file should exist");
        org.hamcrest.MatcherAssert.assertThat(file.getName(), endsWith(".txt"));
    }

    // --------- ErrorCollector Rule (JUnit4) -> assertAll (JUnit5) ----------
    @Test
    public void test08_error_collector() {
        assertAll(
                () -> org.hamcrest.MatcherAssert.assertThat("a", "a", is("a")),
                () -> org.hamcrest.MatcherAssert.assertThat("1+1", 1 + 1, is(2)),
                () -> org.hamcrest.MatcherAssert.assertThat("contains init", buffer, hasItem("init"))
        );
        // test continues even if a check fails
    }

    // --------- Demonstrate fail + try/catch style often migrated to assertThrows ----------
    @Test
    public void test09_manual_exception_assertion() {
        try {
            Integer.parseInt("not-a-number");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            org.hamcrest.MatcherAssert.assertThat(expected.getMessage(), containsString("not-a-number"));
        }
    }

    // --------- Demonstrate TestName Rule (JUnit4) -> TestInfo in JUnit5 ----------
    @Test
    public void test10_test_name_rule(TestInfo testInfo) {
        org.hamcrest.MatcherAssert.assertThat(testInfo.getTestMethod().get().getName(), startsWith("test10_"));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Parameterized (JUnit4) -> @ParameterizedTest (JUnit5)
    // -------------------------------------------------------------------------------------------------------------
    public static class ParameterizedExample {

        public static Stream<Arguments> data() {
            return Stream.of(
                    Arguments.of("0", 0),
                    Arguments.of("7", 7),
                    Arguments.of("42", 42)
            );
        }

        @BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @ParameterizedTest(name = "{index}: parseInt({0}) = {1}") // -> JUnit5 display names differ
        @MethodSource("data")
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

        @ParameterizedTest
        @MethodSource("numbers")
        public void absIsNonNegative(int n) {
            assumeTrue(n != Integer.MIN_VALUE, () -> "skip min int edge if desired");
            assertTrue(Math.abs(n) >= 0);
        }

        public static Stream<Arguments> additionPairs() {
            int[] vals = new int[]{-1, 0, 1, 2, 10, 100};
            List<Arguments> args = new ArrayList<>();
            for (int a : vals) {
                for (int b : vals) {
                    args.add(Arguments.of(a, b));
                }
            }
            return args.stream();
        }

        @ParameterizedTest
        @MethodSource("additionPairs")
        public void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Suites + Categories (JUnit4) -> JUnit5: suites via platform suite engine / tags filtering
    // -------------------------------------------------------------------------------------------------------------
    public static class AllTestsSuite {
        // no code
    }

    public static class FastOnlySuite {
        // no code
    }

    // -------------------------------------------------------------------------------------------------------------
    // Class-level Ignore (JUnit4) -> @Disabled in JUnit5
    // -------------------------------------------------------------------------------------------------------------
    @Disabled("Demonstration of @Ignore at class level")
    public static class IgnoredClassExample {
        @Test
        public void willNotRun() {
            fail("Should never run");
        }
    }
}
