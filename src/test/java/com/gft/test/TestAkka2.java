package com.gft.test;

import akka.actor.*;
import akka.japi.pf.PFBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.PatternsCS;
import akka.stream.ActorAttributes;
import akka.stream.Materializer;
import akka.stream.Supervision;
import akka.stream.SystemMaterializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.testkit.javadsl.TestKit;
import akka.util.Timeout;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.Function1;
import scala.PartialFunction;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestAkka2 {

    private static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("uplift-migration-fixture");
    }

    @AfterClass
    public static void tearDown() {
        TestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void shouldRunStreamUsingActorMaterializer_andMaterializerSettings() throws Exception {

        Function1<Throwable, Supervision.Directive> decider;
        decider =exc -> {
            if (exc instanceof ArithmeticException) return Supervision.resume();
            return Supervision.stop();
        };

        final Materializer materializer = SystemMaterializer.get(system).materializer();


        PartialFunction<Throwable, Integer> recoverPf =
                new PFBuilder<Throwable, Integer>()
                        .match(ArithmeticException.class, ex -> -999)
                        .build();


        CompletionStage<List<Integer>> result =
                Source.range(1, 5)
                        .map(i -> 10 / (i - 3))
                        .withAttributes(ActorAttributes.supervisionStrategy(decider))
                        .recover(recoverPf)
                        .runWith(Sink.seq(), materializer);

        List<Integer> values = result.toCompletableFuture().get(3, TimeUnit.SECONDS);
        assertNotNull(values);
        assertTrue(values.contains(-999));

    }

    @Test
    public void shouldUseSchedulerSchedule_periodicLegacyApi() {
        new TestKit(system) {{
            ActorRef target = system.actorOf(LegacySchedulerActor.props(getRef()), "legacy-scheduler-actor-" + System.nanoTime());

            expectMsgClass(Duration.create(2, TimeUnit.SECONDS), LegacySchedulerActor.Tick.class);

            system.stop(target);
        }};
    }

    public static class LegacySchedulerActor extends AbstractActor {
        public static class Tick { }

        private final ActorRef replyTo;
        private Cancellable cancellable;

        public static Props props(ActorRef replyTo) {
            return Props.create(LegacySchedulerActor.class, () -> new LegacySchedulerActor(replyTo));
        }

        public LegacySchedulerActor(ActorRef replyTo) {
            this.replyTo = replyTo;
        }

        @Override
        public void preStart() {
            ExecutionContextExecutor dispatcher = context().system().dispatcher();

            FiniteDuration initialDelay = Duration.create(100, TimeUnit.MILLISECONDS);
            FiniteDuration interval = Duration.create(200, TimeUnit.MILLISECONDS);

            cancellable = context().system().scheduler().scheduleWithFixedDelay(
                    initialDelay,
                    interval,
                    self(),
                    new Tick(),
                    dispatcher,
                    self()
            );
        }

        @Override
        public void postStop() {
            if (cancellable != null && !cancellable.isCancelled()) {
                cancellable.cancel();
            }
        }

        @Override
        public Receive createReceive() {
            return ReceiveBuilder.create()
                    .match(Tick.class, t -> replyTo.tell(t, self()))
                    .build();
        }
    }

    @Test
    public void shouldUseTimers_startPeriodicTimer_legacyApi() {
        new TestKit(system) {{
            ActorRef timerActor = system.actorOf(LegacyTimerActor.props(getRef()), "legacy-timer-actor-" + System.nanoTime());

            expectMsgClass(Duration.create(2, TimeUnit.SECONDS), LegacyTimerActor.TimerTick.class);

            system.stop(timerActor);
        }};
    }

    public static class LegacyTimerActor extends AbstractActorWithTimers {

        public static class Start { }
        public static class TimerTick { }

        private static final Object TIMER_KEY = "legacy-timer-key";
        private final ActorRef replyTo;

        public static Props props(ActorRef replyTo) {
            return Props.create(LegacyTimerActor.class, () -> new LegacyTimerActor(replyTo));
        }

        public LegacyTimerActor(ActorRef replyTo) {
            this.replyTo = replyTo;
        }

        @Override
        public void preStart() {
            // auto-start
            self().tell(new Start(), self());
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(Start.class, s -> {
                        FiniteDuration interval = Duration.create(150, TimeUnit.MILLISECONDS);
                        getTimers().startPeriodicTimer(TIMER_KEY, new TimerTick(), interval);
                    })
                    .match(TimerTick.class, tick -> replyTo.tell(tick, self()))
                    .build();
        }
    }

    @Test
    public void shouldUseAskPattern_withTimeout() throws Exception {
        ActorRef echo = system.actorOf(EchoActor.props(), "echo-actor-" + System.nanoTime());

        Timeout timeout = Timeout.durationToTimeout(Duration.create(1, TimeUnit.SECONDS));
        CompletionStage<Object> future = PatternsCS.ask(echo, "ping", timeout);

        Object reply = future.toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals("pong", reply);
    }

    public static class EchoActor extends AbstractActor {
        public static Props props() {
            return Props.create(EchoActor.class, EchoActor::new);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .matchEquals("ping", p -> sender().tell("pong", self()))
                    .matchAny(other -> sender().tell(other, self()))
                    .build();
        }
    }
}
