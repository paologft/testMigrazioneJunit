package com.gft.test;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.theories.*;
import org.junit.rules.*;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)               // -> JUnit5: @TestMethodOrder(...)
@Category(Test1.FastTests.class)      // -> JUnit5: @Tag("...")
public class Test1 {

    // --------- Categories (JUnit4) -> Tags (JUnit5) ----------
    public interface FastTests {}
    public interface SlowTests {}
    public interface DatabaseTests {}

    // --------- Static lifecycle (JUnit4) -> @BeforeAll/@AfterAll (JUnit5) ----------
    private static final AtomicInteger BEFORE_CLASS_COUNTER = new AtomicInteger(0);

    @BeforeClass
    public static void beforeAllJUnit4() {
        BEFORE_CLASS_COUNTER.incrementAndGet();
    }

    @AfterClass
    public static void afterAllJUnit4() {
        // cleanup
    }

    // --------- Instance lifecycle (JUnit4) -> @BeforeEach/@AfterEach (JUnit5) ----------
    private List<String> buffer;

    @Before
    public void setUpJUnit4() {
        buffer = new ArrayList<>();
        buffer.add("init");
    }

    @After
    public void tearDownJUnit4() {
        buffer.clear();
    }

    // --------- Rules (JUnit4) -> Extensions / assertions / TempDir (JUnit5) ----------
    @Rule
    public final TestName testName = new TestName();             // -> JUnit5: TestInfo injection

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();    // -> JUnit5: @TempDir

    @Rule
    public final ErrorCollector errors = new ErrorCollector();   // -> JUnit5: assertAll / multiple assertions

    @Rule
    public final ExpectedException thrown = ExpectedException.none(); // -> JUnit5: assertThrows

    @Rule
    public final Timeout globalTimeout = Timeout.millis(250);    // -> JUnit5: assertTimeout / @Timeout

    @Rule
    public final ExternalResource resource = new ExternalResource() { // -> JUnit5: @BeforeEach/@AfterEach or extensions
        @Override
        protected void before() {
            buffer.add("resource-before");
        }

        @Override
        protected void after() {
            buffer.add("resource-after");
        }
    };

    @Rule
    public final TestWatcher watcher = new TestWatcher() {       // -> JUnit5: TestWatcher extension
        @Override protected void starting(Description description) {
            // could log: description.getMethodName()
        }

        @Override protected void failed(Throwable e, Description description) {
            // could log failure
        }

        @Override protected void succeeded(Description description) {
            // could log success
        }
    };

    @Rule
    public final RuleChain chain = RuleChain
            .outerRule(new ExternalResource() {
                @Override protected void before() { buffer.add("chain-outer-before"); }
                @Override protected void after() { buffer.add("chain-outer-after"); }
            })
            .around(new ExternalResource() {
                @Override protected void before() { buffer.add("chain-inner-before"); }
                @Override protected void after() { buffer.add("chain-inner-after"); }
            });

    // --------- ClassRule (JUnit4) -> extensions / static fixtures ----------
    @ClassRule
    public static final ExternalResource classResource = new ExternalResource() {
        @Override protected void before() {
            // global once-per-class resource init
        }

        @Override protected void after() {
            // global cleanup
        }
    };

    // --------- Ignored tests/classes (JUnit4) -> @Disabled (JUnit5) ----------
    @Ignore("Demonstration of @Ignore at method level")          // -> JUnit5: @Disabled("...")
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
    @Test(timeout = 50L)
    public void test04_timeout_annotation() throws InterruptedException {
        Thread.sleep(10L);
        assertTrue(true);
    }

    // --------- @Test(expected=...) (JUnit4) -> assertThrows in JUnit5 ----------
    @Test(expected = IllegalArgumentException.class)
    public void test05_expected_exception_annotation() {
        throw new IllegalArgumentException("boom");
    }

    // --------- ExpectedException Rule (JUnit4) -> assertThrows in JUnit5 ----------
    @Test
    public void test06_expected_exception_rule() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(containsString("state"));
        throw new IllegalStateException("bad state");
    }

    // --------- TemporaryFolder Rule (JUnit4) -> @TempDir (JUnit5) ----------
    @Test
    public void test07_temporary_folder_rule() throws IOException {
        File f = tmp.newFile("demo.txt");
        assertTrue("temp file should exist", f.exists());
        assertThat(f.getName(), endsWith(".txt"));
    }

    // --------- ErrorCollector Rule (JUnit4) -> assertAll (JUnit5) ----------
    @Test
    public void test08_error_collector() {
        errors.checkThat("a", "a", is("a"));
        errors.checkThat("1+1", 1 + 1, is(2));
        errors.checkThat("contains init", buffer, hasItem("init"));
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
    public void test10_test_name_rule() {
        assertThat(testName.getMethodName(), startsWith("test10_"));
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Parameterized (JUnit4) -> @ParameterizedTest (JUnit5)
    // -------------------------------------------------------------------------------------------------------------
    @RunWith(Parameterized.class)
    public static class ParameterizedExample {

        @Parameterized.Parameters(name = "{index}: parseInt({0}) = {1}") // -> JUnit5 display names differ
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"0", 0},
                    {"7", 7},
                    {"42", 42}
            });
        }

        @Parameterized.Parameter(0)
        public String input;

        @Parameterized.Parameter(1)
        public int expected;

        @Before
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @Test
        public void parsesIntegers() {
            assertEquals(expected, Integer.parseInt(input));
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Nested showcase: Theories (JUnit4) -> usually reworked to parameterized tests or property-based in JUnit5
    // -------------------------------------------------------------------------------------------------------------
    @RunWith(Theories.class)
    public static class TheoriesExample {

        @DataPoints
        public static int[] numbers = new int[]{-1, 0, 1, 2, 10};

        @DataPoint
        public static int special = 100;

        @Theory
        public void absIsNonNegative(int n) {
            assumeTrue("skip min int edge if desired", n != Integer.MIN_VALUE);
            assertTrue(Math.abs(n) >= 0);
        }

        @Theory
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
    @Ignore("Demonstration of @Ignore at class level")
    public static class IgnoredClassExample {
        @Test
        public void willNotRun() {
            fail("Should never run");
        }
    }
}

