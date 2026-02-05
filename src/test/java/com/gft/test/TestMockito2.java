package com.gft.test;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;

@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.MethodName.class)
@PowerMockIgnore({
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.crypto.*",
        "org.xml.*",
        "org.w3c.*"
})
@SuppressStaticInitializationFor("com.example.migrationfixture.PowerMockMockitoJUnit4MigrationFixtureTest$FinalUtil")
@PrepareForTest({
        TestMockito2.FinalUtil.class,
        TestMockito2.ServiceUnderTest.class,
        TestMockito2.Collaborator.class,
        Date.class
})
@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@org.junit.jupiter.api.Timeout(2)
public class TestMockito2 {

    @Mock
    private Collaborator collaboratorMock;

    @Spy
    private Helper helperSpy = new Helper();

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @InjectMocks
    private ServiceUnderTest sutWithInjectMocks;

    @TempDir
    Path tempDir;

    @RegisterExtension
    public static org.junit.jupiter.api.extension.Extension classResource = new org.junit.jupiter.api.extension.BeforeAllCallback() {
        @Override
        public void beforeAll(org.junit.jupiter.api.extension.ExtensionContext context) {
            // placeholder
        }
    };

    @BeforeAll
    public static void beforeAllJUnit4() {
        // placeholder
    }

    @AfterAll
    public static void afterAllJUnit4() {
        // placeholder
    }

    @BeforeEach
    public void setUpJUnit4() {
        // initMocks è un classico pattern JUnit4 da migrare
    }

    @AfterEach
    public void tearDownJUnit4() {
        // placeholder
    }

    @Test
    public void test01_mockStatic_and_verifyStatic_and_tempFolder() throws Exception {
        File tmp = Files.createFile(tempDir.resolve("fixture.txt")).toFile();
        assertTrue(tmp.exists());

        // TODO: migrate PowerMock mockStatic/verifyStatic to Mockito.mockStatic when possible
        mockStatic(FinalUtil.class);

        when(FinalUtil.decorate(anyString())).thenReturn("DECORATED");
        when(FinalUtil.now()).thenReturn("FAKE_NOW");

        String res = sutWithInjectMocks.process("input");

        MatcherAssert.assertThat(res, containsString("DECORATED"));
        MatcherAssert.assertThat(FinalUtil.now(), is("FAKE_NOW"));

        // PowerMock verifyStatic
        verifyStatic(FinalUtil.class, times(1));
        FinalUtil.decorate(anyString());
    }

    @Test
    public void test02_whenNew_constructor_and_privateMethod_stubbing() throws Exception {
        Collaborator created = mock(Collaborator.class);
        when(created.callRemote(anyString())).thenReturn("REMOTE_OK");

        // TODO: migrate PowerMock whenNew to Mockito.mockConstruction when possible
        whenNew(Collaborator.class)
                .withArguments("prod-endpoint")
                .thenReturn(created);

        ServiceUnderTest real = new ServiceUnderTest();
        ServiceUnderTest spySut = PowerMockito.spy(real);

        doReturn("PRIVATE_OVERRIDDEN")
                .when(spySut, "secretTransform", anyString());

        String out = spySut.process("abc");

        MatcherAssert.assertThat(out, containsString("PRIVATE_OVERRIDDEN"));

        verifyPrivate(spySut, times(1))
                .invoke("secretTransform", anyString());
    }

    @Test
    public void test03_whitebox_setInternalState_and_invokePrivate() throws Exception {
        ServiceUnderTest sut = new ServiceUnderTest();

        Collaborator c = mock(Collaborator.class);
        when(c.callRemote(anyString())).thenReturn("X");

        Whitebox.setInternalState(sut, "collaborator", c);

        String secret = Whitebox.invokeMethod(sut, "secretTransform", "hello");
        MatcherAssert.assertThat(secret, startsWith("S:"));

        String out = sut.process("hello");
        MatcherAssert.assertThat(out, containsString("DEC(")); // decorate reale (se non mockato)
    }

    @Test
    public void test04_expectedException_rule() {
        ServiceUnderTest sut = new ServiceUnderTest();

        IllegalStateException ex = assertThrows(IllegalStateException.class, sut::failFast);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    public void test05_testExpectedAttribute_junit4() {
        assertThrows(NullPointerException.class, () -> {
            String s = null;
            // NPE intenzionale
            s.length();
        });
    }

    @Disabled("Fixture: test intenzionalmente ignorato per verificare migrazione @Disabled")
    @Test
    public void test06_ignore_annotation() {
        fail("Non dovrebbe essere eseguito");
    }

    @Test
    public void test07_assume_junit4() {
        assumeTrue("fixture assume", System.getProperty("java.version") != null);
        assertTrue(true);
    }

    @Test
    public void test08_mockito_answer_captor_inorder() {
        Collaborator c = mock(Collaborator.class);

        when(c.callRemote(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                return "ANS(" + args[0] + ")";
            }
        });

        ServiceUnderTest sut = new ServiceUnderTest();
        Whitebox.setInternalState(sut, "collaborator", c);

        String out = sut.process("ping");

        verify(c).callRemote(stringCaptor.capture());
        MatcherAssert.assertThat(stringCaptor.getValue(), is("ping"));

        // InOrder
        InOrder inOrder = inOrder(c);
        inOrder.verify(c).callRemote(anyString());

        MatcherAssert.assertThat(out, containsString("ANS(ping)"));
    }

    @Test
    public void test09_whenNew_on_JDK_type_Date() throws Exception {
        Date fake = mock(Date.class);
        when(fake.getTime()).thenReturn(123L);

        // TODO: migrate PowerMock whenNew to Mockito.mockConstruction when possible
        whenNew(Date.class).withNoArguments().thenReturn(fake);

        ServiceUnderTest sut = new ServiceUnderTest();
        long t = sut.currentTime();

        assertEquals(123L, t);
    }

    @Test
    public void test10_spy_doReturn_doThrow() {
        doReturn("OVERRIDE").when(helperSpy).normalize("  a  ");

        assertEquals("OVERRIDE", helperSpy.normalize("  a  "));

        doThrow(new RuntimeException("spy boom")).when(helperSpy).explode();

        try {
            helperSpy.explode();
            fail("Expected exception not thrown");
        } catch (RuntimeException ex) {
            MatcherAssert.assertThat(ex.getMessage(), is("spy boom"));
        }
    }

    static class FinalUtil {
        static {
            String v = System.getProperty("fixture.static", "ok");
            if ("fail".equals(v)) {
                throw new RuntimeException("static init fail");
            }
        }

        static String now() {
            return "REAL_NOW";
        }

        static String decorate(String in) {
            return "DEC(" + in + ")";
        }

        private FinalUtil() { }
    }

    static class Collaborator {
        private final String endpoint;

        Collaborator(String endpoint) {
            this.endpoint = endpoint;
        }

        String callRemote(String payload) {
            return "REMOTE(" + endpoint + "):" + payload;
        }
    }

    static class Helper {
        String normalize(String s) {
            return s == null ? null : s.trim().toLowerCase();
        }

        void explode() {
            // no-op
        }
    }

    static class ServiceUnderTest {
        private Collaborator collaborator = new Collaborator("prod-endpoint");

        String process(String input) {
            String normalized = new Helper().normalize(input);
            String remote = collaborator.callRemote(normalized);

            // metodo privato + static util (target per PowerMock)
            String secret = secretTransform(remote);
            return FinalUtil.decorate(secret);
        }

        private String secretTransform(String s) {
            return "S:" + s;
        }

        long currentTime() {
            // target per whenNew(Date.class)
            return new Date().getTime();
        }

        void failFast() {
            throw new IllegalStateException("boom");
        }
    }
}
