package com.gft.test;

import org.junit.experimental.categories.Category;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.theories.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)               // -> JUnit5: @TestMethodOrder(...)
@Category(Test1.FastTests.class)      // -> JUnit5: @Tag("...")
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

    // --------- ClassRule (JUnit4) -> extensions / static fixtures ----------

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
                "Run only when property is set");

        org.hamcrest.MatcherAssert.assertThat(System.getProperty("os.name", "").toLowerCase(Locale.ROOT),
                not(containsString("unknown")));

        // if we got here, the assumptions held
        assertTrue(true);
    }

    // --------- @Test(timeout=...) (JUnit4) -> @Timeout or assertTimeout in JUnit5 ----------
    @Test
    public void test04_timeout_annotation() throws InterruptedException {
        assertTimeout(java.time.Duration.ofMillis(50L), () -> {
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
        org.hamcrest.MatcherAssert.assertThat(ex.getMessage(), containsString("state"));
    }

    // --------- TemporaryFolder Rule (JUnit4) -> @TempDir (JUnit5) ----------
    @Test
    public void test07_temporary_folder_rule(@TempDir File tmp) throws IOException {
        File f = new File(tmp, "demo.txt");
        assertTrue(f.createNewFile(), "temp file should exist");
        assertTrue(f.exists(), "temp file should exist");
        org.hamcrest.MatcherAssert.assertThat(f.getName(), endsWith(".txt"));
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

        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"0", 0},
                    {"7", 7},
                    {"42", 42}
            });
        }

        @BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @org.junit.jupiter.params.ParameterizedTest(name = "{index}: parseInt({0}) = {1}") // -> JUnit5 display names differ
        @org.junit.jupiter.params.provider.MethodSource("data")
        public void parsesIntegers(String input, int expected) {
            assertEquals(expected, Integer.parseInt(input));
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Theories (JUnit4) -> usually reworked to parameterized tests or property-based in JUnit5
    // -------------------------------------------------------------------------------------------------------------
    public static class TheoriesExample {

        public static int[] numbers = new int[]{-1, 0, 1, 2, 10};

        public static int special = 100;

        public static int[] absIsNonNegative() {
            int[] combined = Arrays.copyOf(numbers, numbers.length + 1);
            combined[combined.length - 1] = special;
            return combined;
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource
        public void absIsNonNegative(int n) {
            assumeTrue(n != Integer.MIN_VALUE, "skip min int edge if desired");
            assertTrue(Math.abs(n) >= 0);
        }

        public static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> additionIsCommutative() {
            List<Integer> all = new ArrayList<>();
            for (int n : numbers) {
                all.add(n);
            }
            all.add(special);
            List<org.junit.jupiter.params.provider.Arguments> args = new ArrayList<>();
            for (int a : all) {
                for (int b : all) {
                    args.add(org.junit.jupiter.params.provider.Arguments.of(a, b));
                }
            }
            return args.stream();
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource
        public void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Suites + Categories (JUnit4) -> JUnit5: suites via platform suite engine / tags filtering
    // -------------------------------------------------------------------------------------------------------------
    @RunWith(Suite.class)
    @Suite.SuiteClasses({
            Test1.class,
            ParameterizedExample.class,
            TheoriesExample.class
    })
    public static class AllTestsSuite {
        // no code
    }

    @RunWith(Categories.class)
    @Categories.IncludeCategory(FastTests.class)
    @Categories.ExcludeCategory(SlowTests.class)
    @Suite.SuiteClasses({
            Test1.class,
            ParameterizedExample.class
    })
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
