package com.gft.test;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Category;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.*;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(Test2.FastTests.class)
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

    @Rule
    public final TestName testName = new TestName();

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public final ErrorCollector errors = new ErrorCollector();

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final Timeout globalTimeout = Timeout.millis(250);

    @Rule
    public final ExternalResource resource = new ExternalResource() {
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
    public final TestWatcher watcher = new TestWatcher() {
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

    @ClassRule
    public static final ExternalResource classResource = new ExternalResource() {
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
        fail("Should never run");
    }

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

    @Test
    public void test02_hamcrest_assertThat() {
        assertThat("buffer has init", buffer, hasItem("init"));
        assertThat("counter", BEFORE_CLASS_COUNTER.get(), greaterThanOrEqualTo(1));
        assertThat("string", "hello", allOf(startsWith("he"), endsWith("lo")));
    }

    @Test
    public void test03_assumptions() {
        assumeTrue("Run only when property is set",
                Boolean.parseBoolean(System.getProperty("run.assumption.tests", "true")));

        assumeThat(System.getProperty("os.name", "").toLowerCase(Locale.ROOT),
                not(containsString("unknown")));

        // if we got here, the assumptions held
        assertTrue(true);
    }

    @Test(timeout = 50L)
    public void test04_timeout_annotation() throws InterruptedException {
        Thread.sleep(10L);
        assertTrue(true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test05_expected_exception_annotation() {
        throw new IllegalArgumentException("boom");
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
        File f = tmp.newFile("demo.txt");
        assertTrue("temp file should exist", f.exists());
        assertThat(f.getName(), endsWith(".txt"));
    }

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
            fail("Expected NumberFormatException");
        } catch (NumberFormatException expected) {
            assertThat(expected.getMessage(), containsString("not-a-number"));
        }
    }

    @Test
    public void test10_test_name_rule() {
        assertThat(testName.getMethodName(), startsWith("test10_"));
    }


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

        @BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @Test
        public void parsesIntegers() {
            assertEquals(expected, Integer.parseInt(input));
        }
    }

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

    @RunWith(Suite.class)
    @Suite.SuiteClasses({
            Test2.class,
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
            Test2.class,
            ParameterizedExample.class
    })
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
}
