package eu.javaspecialists.deadlock.lab1solution;

import eu.javaspecialists.deadlock.lab1.*;

import java.util.concurrent.*;

/**
 * At the symposium, we create a bunch of thinkers and place cups of wine
 * between them.  We then run them in a cached thread pool.  Unfortunately when
 * all the Thinker instances try to drink at the same time, they cause a
 * deadlock.
 * <p>
 * DO NOT CHANGE THIS CODE!
 *
 * @author Heinz Kabutz
 */
public class Symposium {
    private final Krasi[] cups;
    private final Thinker[] thinkers;

    public Symposium(int delegates) {
        cups = new Krasi[delegates];
        thinkers = new Thinker[delegates];
        for (int i = 0; i < cups.length; i++) {
            cups[i] = new Krasi();
        }
        for (int i = 0; i < delegates; i++) {
            Krasi left = cups[i];
            Krasi right = cups[(i + 1) % delegates];
            thinkers[i] = new Thinker(i, left, right);
        }
    }

    public ThinkerStatus run() throws InterruptedException, TimeoutException {
        // do this after we created the symposium, so that we do not
        // let the reference to the Symposium escape.
        ThinkerStatus result = ThinkerStatus.HAPPY_THINKER;
        ExecutorService exec = Executors.newCachedThreadPool();
        try {
            CompletionService<ThinkerStatus> results =
                new ExecutorCompletionService<>(exec);
            for (Thinker thinker : thinkers) {
                results.submit(thinker);
            }
            System.out.println("Waiting for results");
            for (Thinker thinker : thinkers) {
                try {
                    Future<ThinkerStatus> future = results.poll(1, TimeUnit.SECONDS);
                    if (future == null)
                        throw new TimeoutException("Did not finish drinking fast enough");
                    ThinkerStatus status = future.get();
                    System.out.println(status);
                    if (status == ThinkerStatus.UNHAPPY_THINKER) {
                        result = ThinkerStatus.UNHAPPY_THINKER;
                    }
                } catch (ExecutionException e) {
                    throw new IllegalStateException(e.getCause());
                }
            }
        } finally {
            exec.shutdown();
        }
        return result;
    }
}
