package com.gft.test;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.ActorMaterializerSettings;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TestAkka5 {

    private static ActorSystem system;
    private static Materializer materializer;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create("test-system");

        // --- Settings personalizzati (Akka 2.5.23) ---
        ActorMaterializerSettings settings =
                ActorMaterializerSettings.create(system)
                        .withInputBuffer(2, 16) // initial=2, max=16
                        .withDispatcher("akka.actor.default-dispatcher")
                        .withDebugLogging(true);

        // Materializer basato su ActorMaterializer con settings custom
        materializer = ActorMaterializer.create(settings, system);
        // --------------------------------------------
    }

    @AfterClass
    public static void teardown() throws Exception {
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void shouldProcessNumbers() throws Exception {
        Source<Integer, NotUsed> source = Source.from(Arrays.asList(1, 2, 3, 4, 5));

        Flow<Integer, Integer, NotUsed> flow =
                Flow.of(Integer.class)
                        .map(i -> i * 2)          // 2,4,6,8,10
                        .filter(i -> i % 4 == 0);  // 4,8

        Sink<Integer, CompletionStage<List<Integer>>> sink = Sink.seq();

        CompletionStage<List<Integer>> resultCs = source.via(flow).runWith(sink, materializer);

        List<Integer> result = resultCs.toCompletableFuture().get(3, TimeUnit.SECONDS);
        assertEquals(Arrays.asList(4, 8), result);
    }
}