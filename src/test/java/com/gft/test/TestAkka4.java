package com.gft.test;

import akka.Done;
import akka.NotUsed;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import akka.stream.ActorAttributes;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.TestKit;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class TestAkka4 {

    private static ActorSystem system;
    private static LoggingAdapter log;


    private static Materializer materializer;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("uplift-migration-test");
        log = Logging.getLogger(system, "uplift-migration-test");

        materializer = Materializer.createMaterializer(system);
    }

    @AfterClass
    public static void teardown() {

        Duration timeout = Duration.create(10, TimeUnit.SECONDS);
        TestKit.shutdownActorSystem(system, timeout, true);

        system = null;
        materializer = null;
    }


    @Test
    public void stream_materializer_and_supervisionStrategy_should_be_migrated() throws Exception {
        List<Integer> input = Arrays.asList(10, 0, 5); // 0 provoca ArithmeticException


        Source<Integer, NotUsed> src = Source.from(input)
                .map(i -> 100 / i) // crash su i=0
                .withAttributes(ActorAttributes.supervisionStrategy(deciderResuming()));

        CompletionStage<List<Integer>> out =
                src.runWith(Sink.seq(), materializer);

        List<Integer> result = out.toCompletableFuture().get(5, TimeUnit.SECONDS);

        Assert.assertEquals(Arrays.asList(10, 20), result);
    }


    @Test
    public void classic_patterns_ask_pipe_gracefulStop_scheduler() throws Exception {
        ActorRef worker = system.actorOf(Props.create(WorkerActor.class), "worker-" + Instant.now().toEpochMilli());
        ActorRef target = system.actorOf(Props.create(TargetActor.class), "target-" + Instant.now().toEpochMilli());

        CompletionStage<Object> asked =
                PatternsCS.ask(worker, new WorkerActor.Compute(21), 1000);

        Integer answer = (Integer) asked.toCompletableFuture().get(3, TimeUnit.SECONDS);
        Assert.assertEquals(Integer.valueOf(42), answer);

        CompletionStage<Object> futureMsg =
                CompletableFuture.supplyAsync(() -> "hello-from-future");

        PatternsCS.pipe(futureMsg, system.dispatcher()).to(target);

        CountDownLatch latch = new CountDownLatch(1);
        system.scheduler().scheduleOnce(
                Duration.create(200, TimeUnit.MILLISECONDS),
                () -> {
                    worker.tell(new WorkerActor.Ping(latch), ActorRef.noSender());
                },
                system.dispatcher()
        );

        Assert.assertTrue("Ping should arrive", latch.await(2, TimeUnit.SECONDS));

        CompletionStage<Boolean> stopped =
                PatternsCS.gracefulStop(worker, Duration.create(2, TimeUnit.SECONDS), PoisonPill.getInstance());

        Assert.assertTrue(stopped.toCompletableFuture().get(3, TimeUnit.SECONDS));
    }

    @Test
    public void coordinatedShutdown_addTask_example() throws Exception {
        CoordinatedShutdown cs = CoordinatedShutdown.get(system);

        AtomicReference<String> marker = new AtomicReference<>("NOT-RUN");

        cs.addTask(
                CoordinatedShutdown.PhaseBeforeActorSystemTerminate(),
                "uplift-test-task",
                () -> CompletableFuture.supplyAsync(() -> {
                    marker.set("RUN@" + Instant.now());
                    return Done.getInstance();
                })
        );

        CompletionStage<Done> done = cs.runAll(CoordinatedShutdown.unknownReason());
        done.toCompletableFuture().get(5, TimeUnit.SECONDS);

        Assert.assertTrue(marker.get().startsWith("RUN@"));
    }


    @Test
    public void scalaFuture_await_interop() throws Exception {
        ActorRef worker = system.actorOf(Props.create(WorkerActor.class), "worker-scala-" + Instant.now().toEpochMilli());

        Future<Object> scalaFuture =
                Patterns.ask(worker, new WorkerActor.Compute(10), 1000);

        Object value = Await.result(scalaFuture, Duration.create(3, TimeUnit.SECONDS));
        Assert.assertEquals(20, value);
    }


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


    private static Function1<Throwable, Supervision.Directive> deciderResuming() {
        return exc -> {
            if (exc instanceof ArithmeticException) {
                return Supervision.resume();
            }
            return Supervision.stop();
        };
    }
}
