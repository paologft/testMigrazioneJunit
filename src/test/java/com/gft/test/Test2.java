package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * JUnit4 "kitchen sink" test class meant to stress a JUnit4->JUnit5 migrator.
 * Contains most features that typically require migration changes.
 */
@TestMethodOrder(MethodName.class)
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

    private String testName;

    @TempDir
    Path tmp;

    @BeforeEach
    public void setUpJUnit4(TestInfo testInfo) {
        testName = testInfo.getTestMethod().map(m -> m.getName()).orElse(null);
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

    @Test
    @Timeout(value = 50, unit = java.util.concurrent.TimeUnit.MILLISECONDS)
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
        File f = tmp.resolve("demo.txt").toFile();
        assertTrue(f.createNewFile(), "temp file should be created");
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
    public void test10_test_name_rule() {
        assertThat(testName, startsWith("test10_"));
    }


    public static class ParameterizedExample {

        @BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @ParameterizedTest
        @CsvSource({
                "0,0",
                "7,7",
                "42,42"
        })
        public void parsesIntegers(String input, int expected) {
            assertEquals(expected, Integer.parseInt(input));
        }
    }

    public static class TheoriesExample {

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(ints = {-1, 0, 1, 2, 10})
        public void absIsNonNegative(int n) {
            assumeTrue(n != Integer.MIN_VALUE, "skip min int edge if desired");
            assertTrue(Math.abs(n) >= 0);
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource("additionArgs")
        public void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }

        static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> additionArgs() {
            int[] nums = new int[]{-1, 0, 1, 2, 10, 100};
            java.util.List<org.junit.jupiter.params.provider.Arguments> args = new java.util.ArrayList<>();
            for (int a : nums) {
                for (int b : nums) {
                    args.add(org.junit.jupiter.params.provider.Arguments.arguments(a, b));
                }
            }
            return args.stream();
        }
    }

    @Suite
    @SelectClasses({
            Test2.class,
            ParameterizedExample.class,
            TheoriesExample.class
    })
    public static class AllTestsSuite {
        // no code
    }

    @Suite
    @SelectClasses({
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
