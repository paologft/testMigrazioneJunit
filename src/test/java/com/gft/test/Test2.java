package com.gft.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.suite.api.SelectClasses;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class Test2 {

    public interface FastTests {}
    public interface SlowTests {}
    public interface DatabaseTests {}

    private static final AtomicInteger BEFORE_CLASS_COUNTER = new AtomicInteger(0);

    @BeforeAll
    public static void beforeAllJUnit5() {
        BEFORE_CLASS_COUNTER.incrementAndGet();
    }

    @AfterAll
    public static void afterAllJUnit5() {
        // cleanup
    }

    private List<String> buffer;

    @BeforeEach
    public void setUpJUnit5() {
        buffer = new ArrayList<>();
        buffer.add("init");
    }

    @AfterEach
    public void tearDownJUnit5() {
        buffer.clear();
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
    public void test04_timeout_annotation() {
        assertTimeoutPreemptively(Duration.ofMillis(50), () -> {
            Thread.sleep(10L);
            assertTrue(true);
        });
    }

    @Test
    public void test05_expected_exception_annotation() {
        assertThrows(IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("boom");
        });
    }

    @Test
    public void test06_expected_exception_rule() {
        assertThrows(IllegalStateException.class, () -> {
            throw new IllegalStateException("bad state");
        });
    }

    @Test
    public void test07_temporary_folder_rule(@TempDir Path tempDir) throws IOException {
        File f = tempDir.resolve("demo.txt").toFile();
        assertTrue("temp file should exist", f.exists());
        assertThat(f.getName(), endsWith(".txt"));
    }

    @Test
    public void test08_error_collector() {
        assertAll("error collector",
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

    @ParameterizedTest(name = "{index}: parseInt({0}) = {1}")
    @MethodSource("data")
    public void parsesIntegers(String input, int expected) {
        assertEquals(expected, Integer.parseInt(input));
    }

    static List<Arguments> data() {
        return Arrays.asList(
                Arguments.of("0", 0),
                Arguments.of("7", 7),
                Arguments.of("42", 42)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 2, 10})
    public void absIsNonNegative(int n) {
        assumeTrue("skip min int edge if desired", n != Integer.MIN_VALUE);
        assertTrue(Math.abs(n) >= 0);
    }

    @ParameterizedTest
    @CsvSource({
            "1,2",
            "2,1",
            "3,3"
    })
    public void additionIsCommutative(int a, int b) {
        assertEquals(a + b, b + a);
    }

    @SelectClasses({Test2.class, ParameterizedExample.class, TheoriesExample.class})
    public static class AllTestsSuite {
        // no code
    }

    @Tag("FastTests")
    @SelectClasses({Test2.class, ParameterizedExample.class})
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
        // This class is kept for suite inclusion but contains no tests.
    }

    public static class TheoriesExample {
        // This class is kept for suite inclusion but contains no tests.
    }
}
