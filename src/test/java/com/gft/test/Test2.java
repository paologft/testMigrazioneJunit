package com.gft.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.ExcludeTags;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@Tag("FastTests")
@TestMethodOrder(MethodName.class)
@Timeout(value = 250, unit = TimeUnit.MILLISECONDS)
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

    @TempDir
    Path tempDir;

    @RegisterExtension
    static InlineExtension inlineExtension = new InlineExtension();

    public static class InlineExtension implements BeforeEachCallback, AfterEachCallback, TestWatcher {
        @Override
        public void beforeEach(ExtensionContext context) {
            buffer.add("chain-outer-before");
            buffer.add("chain-inner-before");
            buffer.add("resource-before");
        }

        @Override
        public void afterEach(ExtensionContext context) {
            buffer.add("resource-after");
            buffer.add("chain-inner-after");
            buffer.add("chain-outer-after");
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            // no-op
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            // no-op
        }
    }

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

    @Test
    @org.junit.jupiter.api.Timeout(value=50, unit=java.util.concurrent.TimeUnit.MILLISECONDS)
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
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("bad state");
        });
        assertThat(exception.getMessage(), containsString("state"));
    }

    @Test
    public void test07_temporary_folder_rule() throws IOException {
        File f = Files.createFile(tempDir.resolve("demo.txt")).toFile();
        assertTrue("temp file should exist", f.exists());
        assertThat(f.getName(), endsWith(".txt"));
    }

    @Test
    public void test08_error_collector() {
        Assertions.assertAll(
            () -> assertThat("a", "a", is("a")),
            () -> assertThat("1+1", 1 + 1, is(2)),
            () -> assertThat("contains init", buffer, hasItem("init"))
        );
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

    @ParameterizedTest
    @MethodSource("data")
    public void parsesIntegers(String input, int expected) {
        assertEquals(expected, Integer.parseInt(input));
    }

    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"0", 0},
                {"7", 7},
                {"42", 42}
        });
    }

    @Suite
    @SelectClasses({Test2.class, ParameterizedExample.class, TheoriesExample.class})
    public static class AllTestsSuite {
        // no code
    }

    @Suite
    @SelectClasses({Test2.class, ParameterizedExample.class})
    @IncludeTags({"FastTests"})
    @ExcludeTags({"SlowTests"})
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

    public static class ParameterizedExample {

        @BeforeEach
        public void beforeEach() {
            // JUnit4 per-test setup
        }

        @ParameterizedTest
        @MethodSource("data")
        public void parsesIntegers(String input, int expected) {
            assertEquals(expected, Integer.parseInt(input));
        }

        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {"0", 0},
                    {"7", 7},
                    {"42", 42}
            });
        }
    }

    public static class TheoriesExample {

        static int[] numbersArray = new int[]{-1, 0, 1, 2, 10};

        static int special = 100;

        @ParameterizedTest
        @MethodSource("numbers")
        public void absIsNonNegative(int n) {
            assumeTrue("skip min int edge if desired", n != Integer.MIN_VALUE);
            assertTrue(Math.abs(n) >= 0);
        }

        @ParameterizedTest
        @MethodSource("additionPairs")
        public void additionIsCommutative(int a, int b) {
            assertEquals(a + b, b + a);
        }

        public static Stream<Integer> numbers() {
            return Arrays.stream(numbersArray);
        }

        public static Stream<Arguments> additionPairs() {
            int[] nums = numbersArray;
            return IntStream.range(0, nums.length).boxed()
                    .flatMap(i -> IntStream.range(0, nums.length)
                            .mapToObj(j -> Arguments.of(nums[i], nums[j])));
        }
    }
}
