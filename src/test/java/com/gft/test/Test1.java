package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Assumptions.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@TestMethodOrder(MethodName.class)
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

    // --------- Ignored tests/classes (JUnit4) -> @Disabled (JUnit5) ----------
    @Disabled("Demonstration of @Ignore at method level")
    @Test
    public void test00_ignored() {
        fail("Should never run");
    }

    // --------- Basic assertions (JUnit4) ----------
    @Test
    public void test01_assertions_basic() {
        assertTrue("buffer should contain init", buffer.contains("init"));
        assertFalse("buffer should not contain X", buffer.contains("X"));
        assertNull(null);
        assertNotNull(buffer);

        assertEquals("size", 1, buffer.size());
        assertNotEquals("not equals", 1, 2);

        assertSame("same ref", buffer, buffer);
        assertNotSame("different ref", buffer, new ArrayList<String>());

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
        assumeTrue("Run only when property is set",
                Boolean.parseBoolean(System.getProperty("run.assumption.tests", "true")));

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
        assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("bad state");
        });
    }

    // --------- TemporaryFolder Rule (JUnit4) -> @TempDir (JUnit5) ----------
    @Test
    public void test07_temporary_folder_rule(@TempDir File tmpDir) throws IOException {
        File f = new File(tmpDir, "demo.txt");
        assertTrue("temp file should exist", f.exists());
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
    }

    // --------- Demonstrate fail + try/catch style often migrated to assertThrows ----------
    @Test
    public void test09_manual_exception_assertion() {
        try {
            Integer.parseInt("not-a-number");
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            assertThat(expected.getMessage(), containsString("not-a-number"));
        }
    }

    // --------- Demonstrate TestName Rule (JUnit4) -> TestInfo in JUnit5 ----------
    @Test
    public void test10_test_name_rule(TestInfo testInfo) {
        assertThat(testInfo.getDisplayName(), startsWith("test10_"));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Parameterized (JUnit4) -> @ParameterizedTest (JUnit5)
    // -------------------------------------------------------------------------------------------------------------
    public static class ParameterizedExample {

        @ParameterizedTest(name = "{index}: parseInt({0}) = {1}")
        @MethodSource("data")
        void parsesIntegers(String input, int expected) {
            assertEquals(expected, Integer.parseInt(input));
        }

        static Stream<Arguments> data() {
            return Stream.of(
                Arguments.of("0", 0),
                Arguments.of("7", 7),
                Arguments.of("42", 42)
            );
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Theories (JUnit4) -> usually reworked to parameterized tests or property-based in JUnit5
    // -------------------------------------------------------------------------------------------------------------
    public static class TheoriesExample {

        @ParameterizedTest
        @ValueSource(ints = {-1,0,1,2,10,100})
        void absIsNonNegative(int n) {
            assumeTrue("skip min int edge if desired", n != Integer.MIN_VALUE);
            assertTrue(Math.abs(n) >= 0);
        }

        @ParameterizedTest
        @MethodSource("additionPairs")
        void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }

        static Stream<Arguments> additionPairs() {
            return Stream.of(
                Arguments.of(1, 2),
                Arguments.of(2, 1),
                Arguments.of(3, 4),
                Arguments.of(4, 3)
            );
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
