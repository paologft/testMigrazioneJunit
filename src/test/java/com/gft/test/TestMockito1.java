package com.gft.test;

import org.junit.*;
import org.junit.rules.*;
import org.junit.runners.MethodSorters;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;

import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * FIXTURE DI MIGRAZIONE:
 * - JUnit4 lifecycle: @BeforeClass/@AfterClass, @Before/@After
 * - JUnit4 Rules: ExpectedException, TemporaryFolder, Timeout, TestName
 * - JUnit4 annotations: @Ignore, @Test(expected=...), Assume
 * - Runner PowerMock: @RunWith(PowerMockRunner.class)
 * - PowerMock features: @PrepareForTest, @PowerMockIgnore, @SuppressStaticInitializationFor
 * - PowerMockito: mockStatic/verifyStatic, whenNew, spy + stub private method, verifyPrivate
 * - Whitebox: setInternalState/invokeMethod
 * - Mockito: @Mock, @Spy, @Captor, @InjectMocks, MockitoAnnotations.initMocks, Answer, ArgumentCaptor, InOrder
 * - Hamcrest assertions
 */
@ExtendWith(MockitoExtension.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // altro elemento da migrare (JUnit5 non usa FixMethodOrder)
public class TestMockito1 {

    // --- Mockito annotations (da migrare verso @ExtendWith(MockitoExtension.class) + @Mock ecc.) ---
    @Mock
    private Collaborator collaboratorMock;

    @Spy
    private Helper helperSpy = new Helper();

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @InjectMocks
    private ServiceUnderTest sutWithInjectMocks; // verrà comunque rimpiazzato in alcuni test con spy/new

    // --- JUnit4 Rules (in JUnit5 diventano extension o meccanismi diversi) ---
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

    // --- Lifecycle JUnit4 ---
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

    /**
     * 1) Mock static + verifyStatic (PowerMock)
     * 2) Uso di Hamcrest assertThat
     * 3) Uso di TemporaryFolder rule
     */
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

    /**
     * 1) Intercettare costruttore con whenNew (PowerMock)
     * 2) Spy su SUT e stub di metodo privato (PowerMock)
     * 3) verifyPrivate (PowerMock)
     */
    @org.junit.jupiter.api.Test
    public void test02_whenNew_constructor_and_privateMethod_stubbing() throws Exception {
        try (MockedConstruction<Collaborator> mc = Mockito.mockConstruction(Collaborator.class,
                (mock, context) -> when(mock.callRemote(anyString())).thenReturn("REMOTE_OK"))) {

            assertEquals("prod-endpoint", mc.contexts().get(0).arguments().get(0));

            ServiceUnderTest real = new ServiceUnderTest();
            ServiceUnderTest spySut = PowerMockito.spy(real);

            // TODO: migrare stub metodo privato "secretTransform"
            // doReturn("PRIVATE_OVERRIDDEN")
            //         .when(spySut, "secretTransform", anyString());

            String out = spySut.process("abc");

            assertThat(out, containsString("DEC("));

            // TODO: migrare verifyPrivate
            // verifyPrivate(spySut, times(1))
            //         .invoke("secretTransform", anyString());
        }
    }

    /**
     * 1) Whitebox.setInternalState per settare campo privato
     * 2) Whitebox.invokeMethod per invocare metodo privato
     */
    @org.junit.jupiter.api.Test
    public void test03_whitebox_setInternalState_and_invokePrivate() throws Exception {
        ServiceUnderTest sut = new ServiceUnderTest();

        Collaborator c = mock(Collaborator.class);
        when(c.callRemote(anyString())).thenReturn("X");

        // Set campo privato senza setter
        Whitebox.setInternalState(sut, "collaborator", c);

        // Invoke private method
        String secret = Whitebox.invokeMethod(sut, "secretTransform", "hello");
        assertThat(secret, startsWith("S:"));

        String out = sut.process("hello");
        assertThat(out, containsString("DEC(")); // decorate reale (se non mockato)
    }

    /**
     * 1) Rule ExpectedException (JUnit4)
     */
    @org.junit.jupiter.api.Test
    public void test04_expectedException_rule() {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("boom");

        ServiceUnderTest sut = new ServiceUnderTest();
        sut.failFast();
    }

    /**
     * 1) @Test(expected=...) (JUnit4)
     */
    @org.junit.jupiter.api.Test
    public void test05_testExpectedAttribute_junit4() {
        assertThrows(NullPointerException.class, () -> {
            String s = null;
            // NPE intenzionale
            s.length();
        });
    }

    /**
     * 1) @Ignore (JUnit4)
     */
    @org.junit.jupiter.api.Disabled("Fixture: test intenzionalmente ignorato per verificare migrazione @Disabled")
    @org.junit.jupiter.api.Test
    public void test06_ignore_annotation() {
        fail("Non dovrebbe essere eseguito");
    }

    /**
     * 1) Assume (JUnit4) -> in JUnit5 diventa Assumptions.assumeTrue
     */
    @org.junit.jupiter.api.Test
    public void test07_assume_junit4() {
        assumeTrue(System.getProperty("java.version") != null, "fixture assume");
        assertTrue(true);
    }

    /**
     * 1) Mockito Answer custom
     * 2) ArgumentCaptor
     * 3) InOrder
     */
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

    /**
     * 1) whenNew su classi JDK (Date) - pattern frequente in legacy
     */
    @org.junit.jupiter.api.Test
    public void test09_whenNew_on_JDK_type_Date() throws Exception {
        try (MockedConstruction<Date> mc = Mockito.mockConstruction(Date.class,
                (mock, context) -> when(mock.getTime()).thenReturn(123L))) {

            ServiceUnderTest sut = new ServiceUnderTest();
            long t = sut.currentTime();

            assertEquals(123L, t);
        }
    }

    /**
     * 1) Uso di helperSpy (Mockito @Spy)
     * 2) doThrow/doReturn pattern classico
     */
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

    // --------------------------------------------------------------------------------------------
    // "Production-like" minimal classes (inserite qui per rendere la fixture autosufficiente)
    // --------------------------------------------------------------------------------------------

    /**
     * Classe con static initializer (spesso problematico e gestito con @SuppressStaticInitializationFor)
     */
    static class FinalUtil {
        static {
            // static init "fastidioso": in progetti reali può leggere config, inizializzare framework, ecc.
            // Qui è innocuo, ma serve come target di suppression/migrazione.
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
        // campo privato: ottimo target per Whitebox.setInternalState
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
