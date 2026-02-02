package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.ExcludeTags;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)               // -> JUnit5: @TestMethodOrder(...)
@Category(Test1.FastTests.class)      // -> JUnit5: @Tag("...")
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("FastTests")
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
        buffer.add("chain-outer-before");
        buffer.add("chain-inner-before");
        buffer.add("resource-before");
        buffer.add("init");
    }

    @AfterEach
    public void tearDownJUnit4() {
        buffer.add("resource-after");
        buffer.add("chain-inner-after");
        buffer.add("chain-outer-after");
        buffer.clear();
    }

    // --------- Rules (JUnit4) -> Extensions / assertions / TempDir (JUnit5) ----------
    // Removed @Rule fields and replaced with JUnit 5 equivalents

    // --------- Ignored tests/classes (JUnit4) -> @Disabled (JUnit5) ----------
    @Disabled("Demonstration of @Ignore at method level")          // -> JUnit5: @Disabled("...")
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
    @Timeout(value = 50, unit = TimeUnit.MILLISECONDS)
    public void test04_timeout_annotation() throws InterruptedException {
        Thread.sleep(10L);
        assertTrue(true);
    }

    // --------- @Test(expected=...) (JUnit4) -> assertThrows in JUnit5 ----------
    @Test
    public void test05_expected_exception_annotation() {
        throw new IllegalArgumentException("boom");
    }

    // --------- ExpectedException Rule (JUnit4) -> assertThrows in JUnit5 ----------
    @Test
    public void test06_expected_exception_rule() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("bad state");
        });
        assertThat(e.getMessage(), containsString("state"));
    }

    // --------- TemporaryFolder Rule (JUnit4) -> @TempDir (JUnit5) ----------
    @Test
    public void test07_temporary_folder_rule(@TempDir File tempDir) throws IOException {
        File f = new File(tempDir, "demo.txt");
        f.createNewFile();
        assertTrue("temp file should exist", f.exists());
        assertThat(f.getName(), endsWith(".txt"));
    }

    // --------- ErrorCollector Rule (JUnit4) -> assertAll (JUnit5) ----------
    @Test
    public void test08_error_collector() {
        assertAll("error collector",
                () -> assertThat("a", "a", is("a")),
                () -> assertThat("1+1", 1 + 1, is(2)),
                () -> assertThat("contains init", buffer, hasItem("init"))
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
        @CsvSource({
                "0,0",
                "7,7",
                "42,42"
        })
        public void parsesIntegers(String input, int expected) {
            assertEquals(expected, Integer.parseInt(input));
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Theories (JUnit4) -> parameterized tests (JUnit5)
    // -------------------------------------------------------------------------------------------------------------
    public static class TheoriesExample {

        @ParameterizedTest
        @ValueSource(ints = {-1, 0, 1, 2, 10})
        public void absIsNonNegative(int n) {
            assumeTrue("skip min int edge if desired", n != Integer.MIN_VALUE);
            assertTrue(Math.abs(n) >= 0);
        }

        @ParameterizedTest
        @MethodSource("allPairs")
        public void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }

        static Stream<Arguments> allPairs() {
            int[] numbers = new int[]{-1, 0, 1, 2, 10};
            return Arrays.stream(numbers).boxed()
                    .flatMap(a -> Arrays.stream(numbers).mapToObj(b -> Arguments.of(a, b)));
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Suites + Categories (JUnit4) -> JUnit5: suites via platform suite engine / tags filtering
    // -------------------------------------------------------------------------------------------------------------
    @Suite
    @SelectClasses({
            Test1.class,
            ParameterizedExample.class,
            TheoriesExample.class
    })
    public static class AllTestsSuite {
        // no code
    }

    @Suite
    @SelectClasses({
            Test1.class,
            ParameterizedExample.class
    })
    @IncludeTags("FastTests")
    @ExcludeTags("SlowTests")
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
