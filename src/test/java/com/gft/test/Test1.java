package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)               // -> JUnit5: @TestMethodOrder(...)
@Tag("FastTests")      // -> JUnit5: @Tag("...")
@ResourceLock(Resources.SYSTEM_PROPERTIES)
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
    }

    @AfterEach
    public void tearDownJUnit4() {
        buffer.clear();
    }

    // --------- Rules (JUnit4) -> Extensions / assertions / TempDir (JUnit5) ----------
    private String testName;             // -> JUnit5: TestInfo injection

    @TempDir
    public Path tmp;    // -> JUnit5: @TempDir

    @BeforeEach
    public void captureTestName(TestInfo testInfo) {
        this.testName = testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("");
    }

    @BeforeEach
    public void resourceBefore() { // -> JUnit5: @BeforeEach/@AfterEach or extensions
        buffer.add("resource-before");
    }

    @AfterEach
    public void resourceAfter() { // -> JUnit5: @BeforeEach/@AfterEach or extensions
        buffer.add("resource-after");
    }

    @BeforeEach
    public void chainOuterBefore() {
        buffer.add("chain-outer-before");
    }

    @BeforeEach
    public void chainInnerBefore() {
        buffer.add("chain-inner-before");
    }

    @AfterEach
    public void chainInnerAfter() {
        buffer.add("chain-inner-after");
    }

    @AfterEach
    public void chainOuterAfter() {
        buffer.add("chain-outer-after");
    }

    // --------- ClassRule (JUnit4) -> extensions / static fixtures ----------
    @BeforeAll
    public static void classResourceBefore() {
        // global once-per-class resource init
    }

    @AfterAll
    public static void classResourceAfter() {
        // global cleanup
    }

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
        assertThat("buffer has init", buffer, hasItem("init"));
        assertThat("counter", BEFORE_CLASS_COUNTER.get(), greaterThanOrEqualTo(1));
        assertThat("string", "hello", allOf(startsWith("he"), endsWith("lo")));
    }

    // --------- Assumptions (JUnit4) -> org.junit.jupiter.api.Assumptions ----------
    @Test
    public void test03_assumptions() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("run.assumption.tests", "true")),
                "Run only when property is set");

        assumeThat(System.getProperty("os.name", "").toLowerCase(Locale.ROOT),
                not(containsString("unknown")));

        // if we got here, the assumptions held
        assertTrue(true);
    }

    // --------- @Test(timeout=...) (JUnit4) -> @Timeout or assertTimeout in JUnit5 ----------
    @Test
    public void test04_timeout_annotation() {
        assertTimeout(Duration.ofMillis(50L), () -> {
            Thread.sleep(10L);
            assertTrue(true);
        });
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
        assertThat(ex.getMessage(), containsString("state"));
    }

    // --------- TemporaryFolder Rule (JUnit4) -> @TempDir (JUnit5) ----------
    @Test
    public void test07_temporary_folder_rule() throws IOException {
        File f = tmp.resolve("demo.txt").toFile();
        assertTrue(f.createNewFile(), "temp file should exist");
        assertTrue(f.exists(), "temp file should exist");
        assertThat(f.getName(), endsWith(".txt"));
    }

    // --------- ErrorCollector Rule (JUnit4) -> assertAll (JUnit5) ----------
    @Test
    public void test08_error_collector() {
        assertAll(
                () -> assertThat("a", "a", is("a")),
                () -> assertThat("1+1", 1 + 1, is(2)),
                () -> assertThat("contains init", buffer, hasItem("init"))
        );
        // test continues even if a check fails
    }

    // --------- Demonstrate fail + try/catch style often migrated to assertThrows ----------
    @Test
    public void test09_manual_exception_assertion() {
        NumberFormatException expected = assertThrows(NumberFormatException.class, () -> Integer.parseInt("not-a-number"));
        assertThat(expected.getMessage(), containsString("not-a-number"));
    }

    // --------- Demonstrate TestName Rule (JUnit4) -> TestInfo in JUnit5 ----------
    @Test
    public void test10_test_name_rule() {
        assertThat(testName, startsWith("test10_"));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Parameterized (JUnit4) -> @ParameterizedTest (JUnit5)
    // -------------------------------------------------------------------------------------------------------------
    public static class ParameterizedExample {

        public static Stream<Object[]> data() {
            return Stream.of(
                    new Object[]{"0", 0},
                    new Object[]{"7", 7},
                    new Object[]{"42", 42}
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

        static Stream<Integer> numbers() {
            return Stream.of(-1, 0, 1, 2, 10, 100);
        }

        @ParameterizedTest
        @MethodSource("numbers")
        public void absIsNonNegative(int n) {
            assumeTrue(n != Integer.MIN_VALUE, "skip min int edge if desired");
            assertTrue(Math.abs(n) >= 0);
        }

        static Stream<int[]> additionPairs() {
            int[] values = new int[]{-1, 0, 1, 2, 10, 100};
            List<int[]> pairs = new ArrayList<>();
            for (int a : values) {
                for (int b : values) {
                    pairs.add(new int[]{a, b});
                }
            }
            return pairs.stream();
        }

        @ParameterizedTest
        @MethodSource("additionPairs")
        public void additionIsCommutative(int[] pair) {
            int a = pair[0];
            int b = pair[1];
            assertEquals(a + b, b + a);
        }
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
