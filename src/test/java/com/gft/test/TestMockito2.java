package com.gft.test;

import org.junit.rules.*;
import org.junit.runners.MethodSorters;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class TestMockito2 {

    @Mock
    private Collaborator collaboratorMock;

    @Spy
    private Helper helperSpy = new Helper();

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @InjectMocks
    private ServiceUnderTest sutWithInjectMocks;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public Timeout globalTimeout = Timeout.seconds(2);

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static ExternalResource classResource = new ExternalResource() {
        @Override
        protected void before() {
            // placeholder
        }

        @Override
        protected void after() {
            // placeholder
        }
    };

    @org.junit.jupiter.api.BeforeAll
    public static void beforeAllJUnit4() {
        // placeholder
    }

    @org.junit.jupiter.api.AfterAll
    public static void afterAllJUnit4() {
        // placeholder
    }

    @org.junit.jupiter.api.BeforeEach
    public void setUpJUnit4() {
        // initMocks è un classico pattern JUnit4 da migrare
    }

    @org.junit.jupiter.api.AfterEach
    public void tearDownJUnit4() {
        // placeholder
    }

    @org.junit.jupiter.api.Test
    public void test01_mockStatic_and_verifyStatic_and_tempFolder() throws Exception {
        File tmp = temporaryFolder.newFile("fixture.txt");
        assertTrue(tmp.exists());

        try (MockedStatic<FinalUtil> finalUtilMockedStatic = Mockito.mockStatic(FinalUtil.class)) {
            finalUtilMockedStatic.when(() -> FinalUtil.decorate(anyString())).thenReturn("DECORATED");
            finalUtilMockedStatic.when(FinalUtil::now).thenReturn("FAKE_NOW");

            String res = sutWithInjectMocks.process("input");

            assertThat(res, containsString("DECORATED"));
            assertThat(FinalUtil.now(), is("FAKE_NOW"));

            finalUtilMockedStatic.verify(() -> FinalUtil.decorate(anyString()), times(1));
        }
    }

    @org.junit.jupiter.api.Test
    public void test02_whenNew_constructor_and_privateMethod_stubbing() throws Exception {
        Collaborator created = mock(Collaborator.class);
        when(created.callRemote(anyString())).thenReturn("REMOTE_OK");

        try (MockedConstruction<Collaborator> mc = Mockito.mockConstruction(Collaborator.class,
                (mock, context) -> when(mock.callRemote(anyString())).thenReturn("REMOTE_OK"))) {

            assertEquals("prod-endpoint", mc.contexts().get(0).arguments().get(0));

            ServiceUnderTest real = new ServiceUnderTest();
            ServiceUnderTest spySut = PowerMockito.spy(real);

            // TODO migrate PowerMockito private method stubbing:
            // doReturn("PRIVATE_OVERRIDDEN")
            //         .when(spySut, "secretTransform", anyString());

            String out = spySut.process("abc");

            assertThat(out, containsString("PRIVATE_OVERRIDDEN"));

            // TODO migrate PowerMockito private method verification:
            // verifyPrivate(spySut, times(1))
            //         .invoke("secretTransform", anyString());
        }
    }

    @org.junit.jupiter.api.Test
    public void test03_whitebox_setInternalState_and_invokePrivate() throws Exception {
        ServiceUnderTest sut = new ServiceUnderTest();

        Collaborator c = mock(Collaborator.class);
        when(c.callRemote(anyString())).thenReturn("X");

        Whitebox.setInternalState(sut, "collaborator", c);

        String secret = Whitebox.invokeMethod(sut, "secretTransform", "hello");
        assertThat(secret, startsWith("S:"));

        String out = sut.process("hello");
        assertThat(out, containsString("DEC(")); // decorate reale (se non mockato)
    }

    @org.junit.jupiter.api.Test
    public void test04_expectedException_rule() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("boom");

        ServiceUnderTest sut = new ServiceUnderTest();
        sut.failFast();
    }

    @org.junit.jupiter.api.Test
    public void test05_testExpectedAttribute_junit4() {
        assertThrows(NullPointerException.class, () -> {
            String s = null;
            // NPE intenzionale
            s.length();
        });
    }

    @org.junit.jupiter.api.Disabled("Fixture: test intenzionalmente ignorato per verificare migrazione @Disabled")
    @org.junit.jupiter.api.Test
    public void test06_ignore_annotation() {
        fail("Non dovrebbe essere eseguito");
    }

    @org.junit.jupiter.api.Test
    public void test07_assume_junit4() {
        assumeTrue("fixture assume", System.getProperty("java.version") != null);
        assertTrue(true);
    }

    @org.junit.jupiter.api.Test
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
        assertThat(stringCaptor.getValue(), is("ping"));

        // InOrder
        InOrder inOrder = inOrder(c);
        inOrder.verify(c).callRemote(anyString());

        assertThat(out, containsString("ANS(ping)"));
    }

    @org.junit.jupiter.api.Test
    public void test09_whenNew_on_JDK_type_Date() throws Exception {
        try (MockedConstruction<Date> mc = Mockito.mockConstruction(Date.class,
                (mock, context) -> when(mock.getTime()).thenReturn(123L))) {

            ServiceUnderTest sut = new ServiceUnderTest();
            long t = sut.currentTime();

            assertEquals(123L, t);
        }
    }

    @org.junit.jupiter.api.Test
    public void test10_spy_doReturn_doThrow() {
        doReturn("OVERRIDE").when(helperSpy).normalize("  a  ");

        assertEquals("OVERRIDE", helperSpy.normalize("  a  "));

        doThrow(new RuntimeException("spy boom")).when(helperSpy).explode();

        try {
            helperSpy.explode();
            fail("Expected exception not thrown");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), is("spy boom"));
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
