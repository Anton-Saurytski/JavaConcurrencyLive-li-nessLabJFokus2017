package eu.javaspecialists.deadlock.lab1solution;

import eu.javaspecialists.deadlock.lab1.*;
import eu.javaspecialists.deadlock.util.*;

import java.util.concurrent.*;

/**
 * Launcher to test whether the symposium ends in a deadlock.  Hopefully the
 * deadlock has now disappeared.
 * <p>
 * DO NOT CHANGE THIS CODE!
 *
 * @author Henri Tremblay, Heinz Kabutz
 */
public class Main {
    public static void main(String... args) throws InterruptedException, TimeoutException {
        Symposium symposium = new Symposium(5);
        ThinkerStatus status = symposium.run();
        if (status == ThinkerStatus.UNHAPPY_THINKER) {
            System.err.println("Probably a deadlock (or incorrect code)");
            return;
        }

        DeadlockTester tester = new DeadlockTester();
        tester.checkThatCodeDoesNotDeadlock(new Runnable() {
            public void run() {
                try {
                    ThinkerStatus status = new Symposium(5).run();
                    if (status != ThinkerStatus.HAPPY_THINKER) {
                        throw new AssertionError("Thinker not happy");
                    }
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }
        });

        System.out.println("No deadlock detected");
    }
}

