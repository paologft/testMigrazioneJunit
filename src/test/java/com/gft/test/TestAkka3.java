package com.gft.test;

import akka.Done;
import akka.NotUsed;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import akka.stream.*;
import akka.stream.javadsl.*;

import org.junit.*;

import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test class intentionally written using Akka 2.5.x style APIs and idioms.
 *
 * === OBIETTIVO ===
 * Contiene gli elementi che tipicamente devono essere migrati passando ad Akka 2.6.x,
 * in modo da testare un tool (Uplift) che riscrive classi Java.
 *
 * === MIGRATION TARGETS (principali) ===
 * 1) akka.stream.ActorMaterializer / ActorMaterializerSettings: deprecati in 2.6 (system-wide materializer).
 * 2) Uso "globale" della supervisionStrategy via ActorMaterializerSettings: in 2.6 si preferiscono Attributes.
 * 3) Esecuzione stream con (materializer esplicito) invece di system (o SystemMaterializer).
 * 4) CoordinatedShutdown task registration (tipicamente si modernizza l’uso).
 * 5) Ask/Pipe/GracefulStop/Scheduler patterns (spesso normalizzati).
 *
 * Nota: La classe compila e gira in Akka 2.5.x; in 2.6.x produrrà warning/deprecation,
 * utile per verificare la migrazione automatica.
 */
public class TestAkka3 {

    private static ActorSystem system;
    private static LoggingAdapter log;

    // --- MIGRATION TARGET: ActorMaterializer + ActorMaterializerSettings (2.6 deprecations) ---
    private static Materializer materializer;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("uplift-migration-test");
        log = Logging.getLogger(system, "uplift-migration-test");

        materializer = SystemMaterializer.get(system).materializer();
    }

    @AfterClass
    public static void teardown() {

        Duration timeout = Duration.create(10, TimeUnit.SECONDS);
        akka.testkit.TestKit.shutdownActorSystem(system, timeout, true);

        system = null;
        materializer = null;
    }

    /**
     * Test "stream" costruito per colpire:
     * - ActorMaterializer esplicito (da rimuovere in 2.6)
     * - ActorMaterializerSettings + supervisionStrategy (da spostare su Attributes)
     * - runWith(..., materializer) (in 2.6 spesso diventa runWith(..., system) o SystemMaterializer)
     */
    @Test
    public void stream_materializer_and_supervisionStrategy_should_be_migrated() throws Exception {
        List<Integer> input = Arrays.asList(10, 0, 5); // 0 provoca ArithmeticException

        // MIGRATION TARGET: supervision strategy "old style" (settings) + "new style" (attributes)
        // In Akka 2.6 si spinge verso Attributes (ActorAttributes.supervisionStrategy). [1](https://doc.akka.io/japi/akka-core/current/akka/stream/ActorMaterializer.html)
        Source<Integer, NotUsed> src = Source.from(input)
                .map(i -> 100 / i) // crash su i=0
                .withAttributes(ActorAttributes.supervisionStrategy(deciderResuming()));

        // MIGRATION TARGET: runWith(sink, materializer) => spesso: runWith(sink, system) o SystemMaterializer.get(system).materializer()
        // ActorMaterializer deprecato in 2.6. [1](https://doc.akka.io/japi/akka-core/current/akka/stream/ActorMaterializer.html)[2](https://github.com/akka/akka-http/issues/3128)
        CompletionStage<List<Integer>> out =
                src.runWith(Sink.seq(), materializer);

        List<Integer> result = out.toCompletableFuture().get(5, TimeUnit.SECONDS);

        // Con Resume, l'elemento che causa error viene saltato. Risultato: [10, 20] => [100/10, 100/5]
        Assert.assertEquals(Arrays.asList(10, 20), result);
    }

    /**
     * Test per colpire pattern classici:
     * - ask (PatternsCS)
     * - pipeTo (PatternsCS.pipe)
     * - gracefulStop
     * - scheduler scheduleOnce
     */
    @Test
    public void classic_patterns_ask_pipe_gracefulStop_scheduler() throws Exception {
        ActorRef worker = system.actorOf(Props.create(WorkerActor.class), "worker-" + Instant.now().toEpochMilli());
        ActorRef target = system.actorOf(Props.create(TargetActor.class), "target-" + Instant.now().toEpochMilli());

        // --- ask pattern (CompletionStage) ---
        CompletionStage<Object> asked =
                PatternsCS.ask(worker, new WorkerActor.Compute(21), 1000);

        Integer answer = (Integer) asked.toCompletableFuture().get(3, TimeUnit.SECONDS);
        Assert.assertEquals(Integer.valueOf(42), answer);

        // --- pipeTo pattern ---
        CompletionStage<Object> futureMsg =
                CompletableFuture.supplyAsync(() -> "hello-from-future");

        PatternsCS.pipe(futureMsg, system.dispatcher()).to(target);

        // --- scheduler scheduleOnce ---
        CountDownLatch latch = new CountDownLatch(1);
        system.scheduler().scheduleOnce(
                Duration.create(200, TimeUnit.MILLISECONDS),
                () -> {
                    worker.tell(new WorkerActor.Ping(latch), ActorRef.noSender());
                },
                system.dispatcher()
        );

        Assert.assertTrue("Ping should arrive", latch.await(2, TimeUnit.SECONDS));

        // --- gracefulStop ---
        CompletionStage<Boolean> stopped =
                PatternsCS.gracefulStop(worker, Duration.create(2, TimeUnit.SECONDS), PoisonPill.getInstance());

        Assert.assertTrue(stopped.toCompletableFuture().get(3, TimeUnit.SECONDS));
    }

    /**
     * Test per includere CoordinatedShutdown usage (spesso presente nei progetti reali).
     * CoordinatedShutdown permette di aggiungere task a fasi di shutdown. [3](https://doc.akka.io/libraries/akka-core/current/coordinated-shutdown.html)[4](https://doc.akka.io/japi/akka/2.6/akka/actor/CoordinatedShutdown.html)
     */
    @Test
    public void coordinatedShutdown_addTask_example() throws Exception {
        CoordinatedShutdown cs = CoordinatedShutdown.get(system);

        AtomicReference<String> marker = new AtomicReference<>("NOT-RUN");

        // Task Java API: Supplier<CompletionStage<Done>> [4](https://doc.akka.io/japi/akka/2.6/akka/actor/CoordinatedShutdown.html)
        cs.addTask(
                CoordinatedShutdown.PhaseBeforeActorSystemTerminate(),
                "uplift-test-task",
                () -> CompletableFuture.supplyAsync(() -> {
                    marker.set("RUN@" + Instant.now());
                    return Done.getInstance();
                })
        );

        // Trigger run esplicito
        CompletionStage<Done> done = cs.runAll(CoordinatedShutdown.unknownReason());
        done.toCompletableFuture().get(5, TimeUnit.SECONDS);

        Assert.assertTrue(marker.get().startsWith("RUN@"));
    }

    /**
     * Test che include anche scala.concurrent.Future / Await (ibrido Java/Scala),
     * utile in progetti mixed Java+Scala (Scala 2.12).
     */
    @Test
    public void scalaFuture_await_interop() throws Exception {
        // Future Scala prodotto da Patterns.ask (Scala Future)
        ActorRef worker = system.actorOf(Props.create(WorkerActor.class), "worker-scala-" + Instant.now().toEpochMilli());

        Future<Object> scalaFuture =
                Patterns.ask(worker, new WorkerActor.Compute(10), 1000);

        Object value = Await.result(scalaFuture, Duration.create(3, TimeUnit.SECONDS));
        Assert.assertEquals(20, value);
    }

    // ------------------------------------------------------------
    // SUPPORT ACTORS (inner classes) - keep everything in one file
    // ------------------------------------------------------------

    public static class WorkerActor extends AbstractActor {
        private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

        public static final class Compute {
            public final int x;
            public Compute(int x) { this.x = x; }
        }

        public static final class Ping {
            public final CountDownLatch latch;
            public Ping(CountDownLatch latch) { this.latch = latch; }
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .match(Compute.class, msg -> {
                        log.info("Compute: {}", msg.x);
                        // risponde x*2
                        getSender().tell(msg.x * 2, getSelf());
                    })
                    .match(Ping.class, msg -> {
                        msg.latch.countDown();
                    })
                    .matchAny(o -> {
                        log.warning("Unknown: {}", o);
                    })
                    .build();
        }
    }

    public static class TargetActor extends AbstractActor {
        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, s -> {
                        // intentionally empty: only to receive piped message
                    })
                    .build();
        }
    }

    // ------------------------------------------------------------
    // SUPERVISION DECIDER (migration-related when used via settings)
    // ------------------------------------------------------------

    private static Function1<Throwable, Supervision.Directive> deciderResuming() {
        return exc -> {
            if (exc instanceof ArithmeticException) {
                return Supervision.resume();
            }
            return Supervision.stop();
        };
    }
}
