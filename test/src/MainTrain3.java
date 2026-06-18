package test;

import java.util.Random;

import test.TopicManagerSingleton.TopicManager;

/**
 * MainTrain3: unit tests for Exercise 4 (PlusAgent, IncAgent, GenericConfig).
 */
public class MainTrain3 {

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static double[] subscribeCollector(String topic) {
        double[] result = {Double.NaN};
        TopicManagerSingleton.get().getTopic(topic).subscribe(new Agent() {
            @Override
            public String getName() {
                return "Collector";
            }

            @Override
            public void reset() {
            }

            @Override
            public void callback(String t, Message msg) {
                result[0] = msg.asDouble;
            }

            @Override
            public void close() {
            }
        });
        return result;
    }

    private static void wireAgent(Agent agent, String[] subs, String out) {
        TopicManager tm = TopicManagerSingleton.get();
        for (String sub : subs) {
            tm.getTopic(sub).subscribe(agent);
        }
        tm.getTopic(out).addPublisher(agent);
    }

    private static void unwireAgent(Agent agent, String[] subs, String out) {
        TopicManager tm = TopicManagerSingleton.get();
        for (String sub : subs) {
            tm.getTopic(sub).unsubscribe(agent);
        }
        tm.getTopic(out).removePublisher(agent);
    }

    public static void testPlusAgent_addition() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        PlusAgent plus = new PlusAgent("X", "Y", "Z");
        wireAgent(plus, new String[]{"X", "Y"}, "Z");
        double[] result = subscribeCollector("Z");

        try {
            tm.getTopic("X").publish(new Message(5.0));
            tm.getTopic("Y").publish(new Message(3.0));

            if (Math.abs(result[0] - 8.0) > 0.01) {
                System.out.println("testPlusAgent_addition FAILED: expected 8.0, got " + result[0]);
            } else {
                System.out.println("testPlusAgent_addition PASSED");
            }
        } finally {
            unwireAgent(plus, new String[]{"X", "Y"}, "Z");
        }
    }

    public static void testPlusAgent_waitsForBothInputs() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        PlusAgent plus = new PlusAgent("X", "Y", "Z");
        wireAgent(plus, new String[]{"X", "Y"}, "Z");
        double[] result = subscribeCollector("Z");

        try {
            tm.getTopic("X").publish(new Message(7.0));

            if (!Double.isNaN(result[0])) {
                System.out.println("testPlusAgent_waitsForBothInputs FAILED: should not publish with one input");
            } else {
                System.out.println("testPlusAgent_waitsForBothInputs PASSED");
            }
        } finally {
            unwireAgent(plus, new String[]{"X", "Y"}, "Z");
        }
    }

    public static void testPlusAgent_nonNumericInput() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        PlusAgent plus = new PlusAgent("X", "Y", "Z");
        wireAgent(plus, new String[]{"X", "Y"}, "Z");
        double[] result = subscribeCollector("Z");

        try {
            tm.getTopic("X").publish(new Message("hello"));
            tm.getTopic("Y").publish(new Message(5.0));

            if (!Double.isNaN(result[0])) {
                System.out.println("testPlusAgent_nonNumericInput FAILED: should not publish when one input is non-numeric");
                return;
            }

            tm.getTopic("X").publish(new Message(10.0));

            if (Math.abs(result[0] - 15.0) > 0.01) {
                System.out.println("testPlusAgent_nonNumericInput FAILED: expected 15.0, got " + result[0]);
            } else {
                System.out.println("testPlusAgent_nonNumericInput PASSED");
            }
        } finally {
            unwireAgent(plus, new String[]{"X", "Y"}, "Z");
        }
    }

    public static void testPlusAgent_sequentialUpdates() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        PlusAgent plus = new PlusAgent("X", "Y", "Z");
        wireAgent(plus, new String[]{"X", "Y"}, "Z");

        int[] publishCount = {0};
        double[] lastResult = {Double.NaN};
        tm.getTopic("Z").subscribe(new Agent() {
            @Override
            public String getName() {
                return "Collector";
            }

            @Override
            public void reset() {
            }

            @Override
            public void callback(String topic, Message msg) {
                publishCount[0]++;
                lastResult[0] = msg.asDouble;
            }

            @Override
            public void close() {
            }
        });

        try {
            tm.getTopic("X").publish(new Message(1.0));
            tm.getTopic("Y").publish(new Message(2.0));

            if (publishCount[0] != 1 || Math.abs(lastResult[0] - 3.0) > 0.01) {
                System.out.println("testPlusAgent_sequentialUpdates FAILED: first pair expected 3.0");
                return;
            }

            plus.reset();
            tm.getTopic("X").publish(new Message(10.0));
            tm.getTopic("Y").publish(new Message(20.0));

            if (publishCount[0] != 2 || Math.abs(lastResult[0] - 30.0) > 0.01) {
                System.out.println("testPlusAgent_sequentialUpdates FAILED: second pair expected 30.0");
            } else {
                System.out.println("testPlusAgent_sequentialUpdates PASSED");
            }
        } finally {
            unwireAgent(plus, new String[]{"X", "Y"}, "Z");
        }
    }

    public static void testIncAgent_increment() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        IncAgent inc = new IncAgent("A", "B");
        wireAgent(inc, new String[]{"A"}, "B");
        double[] result = subscribeCollector("B");

        try {
            tm.getTopic("A").publish(new Message(10.0));

            if (Math.abs(result[0] - 11.0) > 0.01) {
                System.out.println("testIncAgent_increment FAILED: expected 11.0, got " + result[0]);
            } else {
                System.out.println("testIncAgent_increment PASSED");
            }
        } finally {
            unwireAgent(inc, new String[]{"A"}, "B");
        }
    }

    public static void testIncAgent_nonNumericIgnored() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        IncAgent inc = new IncAgent("A", "B");
        wireAgent(inc, new String[]{"A"}, "B");
        double[] result = subscribeCollector("B");

        try {
            tm.getTopic("A").publish(new Message("not a number"));

            if (!Double.isNaN(result[0])) {
                System.out.println("testIncAgent_nonNumericIgnored FAILED: should not publish on non-numeric input");
            } else {
                System.out.println("testIncAgent_nonNumericIgnored PASSED");
            }
        } finally {
            unwireAgent(inc, new String[]{"A"}, "B");
        }
    }

    public static void testParallelAgent_wrappedPipeline() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        int initialThreads = Thread.activeCount();
        PlusAgent plus = new PlusAgent("A", "B", "C");
        ParallelAgent wPlus = new ParallelAgent(plus, 10);
        IncAgent inc = new IncAgent("C", "D");
        ParallelAgent wInc = new ParallelAgent(inc, 10);

        wireAgent(wPlus, new String[]{"A", "B"}, "C");
        wireAgent(wInc, new String[]{"C"}, "D");

        try {
            if (Thread.activeCount() != initialThreads + 2) {
                System.out.println("testParallelAgent_wrappedPipeline FAILED: expected " + (initialThreads + 2) + " threads");
                return;
            }

            double[] result = subscribeCollector("D");
            tm.getTopic("A").publish(new Message(2.0));
            tm.getTopic("B").publish(new Message(3.0));
            sleep(300);

            if (Math.abs(result[0] - 6.0) > 0.01) {
                System.out.println("testParallelAgent_wrappedPipeline FAILED: expected 6.0, got " + result[0]);
            } else {
                System.out.println("testParallelAgent_wrappedPipeline PASSED");
            }
        } finally {
            unwireAgent(wPlus, new String[]{"A", "B"}, "C");
            wPlus.close();
            unwireAgent(wInc, new String[]{"C"}, "D");
            wInc.close();
            sleep(150);

            if (Thread.activeCount() != initialThreads) {
                System.out.println("testParallelAgent_wrappedPipeline FAILED: threads not closed");
            }
        }
    }

    public static void testGenericConfig_simpleConf() {
        TopicManager tm = TopicManagerSingleton.get();
        tm.clear();

        int initialThreads = Thread.activeCount();
        GenericConfig gc = new GenericConfig();

        try {
            gc.setConfFile("config_files/simple.conf");
            gc.create();

            if (Thread.activeCount() != initialThreads + 2) {
                System.out.println("testGenericConfig_simpleConf FAILED: expected " + (initialThreads + 2) + " threads");
                return;
            }

            double[] result = subscribeCollector("D");
            Random r = new Random();
            boolean allPassed = true;

            for (int i = 0; i < 5; i++) {
                int x = r.nextInt(100) + 1;
                int y = r.nextInt(100) + 1;
                tm.getTopic("A").publish(new Message(x));
                tm.getTopic("B").publish(new Message(y));
                sleep(200);

                double expected = x + y + 1.0;
                if (Math.abs(result[0] - expected) > 0.01) {
                    System.out.println("testGenericConfig_simpleConf FAILED iteration " + i + ": expected " + expected + ", got " + result[0]);
                    allPassed = false;
                }
            }

            if (allPassed) {
                System.out.println("testGenericConfig_simpleConf PASSED");
            }
        } catch (Exception e) {
            System.out.println("testGenericConfig_simpleConf ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            gc.close();
            sleep(150);

            if (Thread.activeCount() != initialThreads) {
                System.out.println("testGenericConfig_simpleConf FAILED: threads not closed after close()");
            }
        }
    }

    public static void testGenericConfig_implementsConfig() {
        GenericConfig gc = new GenericConfig();
        boolean passed = true;

        if (!(gc instanceof Config)) {
            System.out.println("testGenericConfig_implementsConfig FAILED: does not implement Config");
            passed = false;
        }
        if (!"Generic Config".equals(gc.getName())) {
            System.out.println("testGenericConfig_implementsConfig FAILED: wrong getName()");
            passed = false;
        }
        if (gc.getVersion() != 1) {
            System.out.println("testGenericConfig_implementsConfig FAILED: wrong getVersion()");
            passed = false;
        }

        if (passed) {
            System.out.println("testGenericConfig_implementsConfig PASSED");
        }
    }

    // -------------------------------------------------------------------------
    // Diagnostic tests — help pinpoint runtime failures in the grading system.
    // Each test catches all exceptions and prints details instead of crashing.
    // -------------------------------------------------------------------------

    private static void diagPass(String name, String detail) {
        System.out.println("DIAG " + name + " PASSED: " + detail);
    }

    private static void diagFail(String name, String detail) {
        System.out.println("DIAG " + name + " FAILED: " + detail);
    }

    private static void diagFail(String name, Exception e) {
        System.out.println("DIAG " + name + " FAILED: " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace(System.out);
    }

    public static void testDiag_javaEnvironment() {
        try {
            diagPass("javaEnvironment",
                    "java.version=" + System.getProperty("java.version")
                            + ", user.dir=" + System.getProperty("user.dir")
                            + ", threads=" + Thread.activeCount());
        } catch (Exception e) {
            diagFail("javaEnvironment", e);
        }
    }

    public static void testDiag_classNames() {
        String[] direct = {"test.PlusAgent", "test.IncAgent"};
        for (String name : direct) {
            try {
                Class<?> clazz = Class.forName(name);
                diagPass("classNames", name + " found (" + clazz.getName() + ")");
            } catch (ClassNotFoundException e) {
                diagFail("classNames", name + " NOT FOUND");
            }
        }

        String[] aliased = {
                "project_biu.configs.PlusAgent",
                "project_biu.configs.IncAgent",
                "PlusAgent",
                "IncAgent"
        };
        for (String name : aliased) {
            try {
                Class<?> clazz = GenericConfig.loadAgentClass(name);
                diagPass("classNames", name + " resolved via GenericConfig -> " + clazz.getName());
            } catch (ClassNotFoundException e) {
                diagFail("classNames", name + " could not be resolved — GenericConfig.create() will crash");
            }
        }
    }

    public static void testDiag_officialConfFormat() {
        TopicManagerSingleton.get().clear();
        java.nio.file.Path tempConf = java.nio.file.Paths.get("diag_official.conf");
        GenericConfig gc = new GenericConfig();
        int threadsBefore = Thread.activeCount();

        try {
            java.util.List<String> lines = new java.util.ArrayList<String>();
            lines.add("project_biu.configs.PlusAgent");
            lines.add("A,B");
            lines.add("C");
            lines.add("project_biu.configs.IncAgent");
            lines.add("C");
            lines.add("D");
            java.nio.file.Files.write(tempConf, lines, java.nio.charset.StandardCharsets.UTF_8);

            gc.setConfFile("diag_official.conf");
            gc.create();

            if (Thread.activeCount() != threadsBefore + 2) {
                diagFail("officialConfFormat",
                        "thread count expected " + (threadsBefore + 2) + ", got " + Thread.activeCount());
                return;
            }

            double[] result = subscribeCollector("D");
            TopicManagerSingleton.get().getTopic("A").publish(new Message(2.0));
            TopicManagerSingleton.get().getTopic("B").publish(new Message(3.0));
            sleep(300);

            if (Math.abs(result[0] - 6.0) > 0.01) {
                diagFail("officialConfFormat", "expected D=6.0, got " + result[0]);
            } else {
                diagPass("officialConfFormat", "project_biu.configs.* conf works -> D=6.0");
            }
        } catch (Exception e) {
            diagFail("officialConfFormat", e);
        } finally {
            try {
                gc.close();
                sleep(150);
                java.nio.file.Files.deleteIfExists(tempConf);
            } catch (Exception ignored) {
            }
        }
    }

    public static void testDiag_reflectionConstructors() {
        try {
            java.lang.reflect.Constructor<?> plusCtor =
                    PlusAgent.class.getConstructor(String.class, String.class, String.class);
            PlusAgent plus = (PlusAgent) plusCtor.newInstance("A", "B", "C");
            if (!(plus instanceof Agent)) {
                diagFail("reflectionConstructors", "PlusAgent is not an Agent");
                return;
            }
            diagPass("reflectionConstructors", "PlusAgent(String,String,String) OK, getName=" + plus.getName());

            java.lang.reflect.Constructor<?> incCtor =
                    IncAgent.class.getConstructor(String.class, String.class);
            IncAgent inc = (IncAgent) incCtor.newInstance("C", "D");
            if (!(inc instanceof Agent)) {
                diagFail("reflectionConstructors", "IncAgent is not an Agent");
                return;
            }
            diagPass("reflectionConstructors", "IncAgent(String,String) OK, getName=" + inc.getName());
        } catch (Exception e) {
            diagFail("reflectionConstructors", e);
        }
    }

    public static void testDiag_confFileReadable() {
        String conf = "config_files/simple.conf";
        java.nio.file.Path direct = java.nio.file.Paths.get(conf);
        java.nio.file.Path fromUserDir =
                java.nio.file.Paths.get(System.getProperty("user.dir"), conf);

        diagPass("confFileReadable", "direct path=" + direct.toAbsolutePath() + ", exists=" + java.nio.file.Files.exists(direct));
        diagPass("confFileReadable", "user.dir path=" + fromUserDir.toAbsolutePath() + ", exists=" + java.nio.file.Files.exists(fromUserDir));

        java.nio.file.Path found = null;
        java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            java.nio.file.Path candidate = dir.resolve("config_files").resolve("simple.conf");
            if (java.nio.file.Files.exists(candidate)) {
                found = candidate;
                break;
            }
            dir = dir.getParent();
        }
        if (found != null) {
            diagPass("confFileReadable", "found via parent search: " + found);
        } else {
            diagFail("confFileReadable", "config_files/simple.conf not found walking up from user.dir");
        }

        try {
            GenericConfig gc = new GenericConfig();
            gc.setConfFile(conf);
            gc.create();
            gc.close();
            diagPass("confFileReadable", "GenericConfig loaded " + conf + " successfully");
        } catch (Exception e) {
            diagFail("confFileReadable", e);
        }
    }

    public static void testDiag_genericConfigCreateSafe() {
        TopicManagerSingleton.get().clear();
        GenericConfig gc = new GenericConfig();
        int threadsBefore = Thread.activeCount();

        try {
            gc.setConfFile("config_files/simple.conf");
            gc.create();
            int threadsAfter = Thread.activeCount();
            diagPass("genericConfigCreateSafe",
                    "create() succeeded, threads " + threadsBefore + " -> " + threadsAfter
                            + " (expected +" + 2 + ")");
            if (threadsAfter != threadsBefore + 2) {
                diagFail("genericConfigCreateSafe",
                        "thread count mismatch: got " + (threadsAfter - threadsBefore) + " new threads, expected 2");
            }
        } catch (Exception e) {
            diagFail("genericConfigCreateSafe", e);
        } finally {
            try {
                gc.close();
                sleep(150);
            } catch (Exception e) {
                diagFail("genericConfigCreateSafe_close", e);
            }
        }
    }

    public static void testDiag_createWithoutConfFile() {
        GenericConfig gc = new GenericConfig();
        try {
            gc.create();
            diagFail("createWithoutConfFile", "create() should throw when conf file not set");
        } catch (IllegalStateException e) {
            diagPass("createWithoutConfFile", "correctly threw IllegalStateException: " + e.getMessage());
        } catch (Exception e) {
            diagFail("createWithoutConfFile", e);
        }
    }

    public static void testDiag_doubleCreate() {
        TopicManagerSingleton.get().clear();
        GenericConfig gc = new GenericConfig();
        try {
            gc.setConfFile("config_files/simple.conf");
            gc.create();
            gc.create();
            diagPass("doubleCreate", "calling create() twice did not throw (may duplicate agents/threads)");
            diagPass("doubleCreate", "threads after double create=" + Thread.activeCount());
        } catch (Exception e) {
            diagFail("doubleCreate", e);
        } finally {
            try {
                gc.close();
                sleep(150);
            } catch (Exception ignored) {
            }
        }
    }

    /** Mirrors MainTrain.java step-by-step with diagnostics at each stage. */
    public static void testDiag_simulateMainTrain() {
        TopicManagerSingleton.get().clear();
        int c = Thread.activeCount();
        GenericConfig gc = new GenericConfig();

        try {
            gc.setConfFile("config_files/simple.conf");
            try {
                gc.create();
                diagPass("simulateMainTrain", "create() OK, threads=" + Thread.activeCount() + " (baseline was " + c + ")");
            } catch (Exception e) {
                diagFail("simulateMainTrain_create", e);
                return;
            }

            if (Thread.activeCount() != c + 2) {
                diagFail("simulateMainTrain",
                        "thread count after create: expected " + (c + 2) + ", got " + Thread.activeCount());
            } else {
                diagPass("simulateMainTrain", "thread count after create OK");
            }

            final double[] result = {0.0};
            TopicManagerSingleton.get().getTopic("D").subscribe(new Agent() {
                @Override public String getName() { return ""; }
                @Override public void reset() {}
                @Override public void callback(String topic, Message msg) { result[0] = msg.asDouble; }
                @Override public void close() {}
            });

            Random r = new Random();
            int failures = 0;
            for (int i = 0; i < 9; i++) {
                int x = r.nextInt(1000);
                int y = r.nextInt(1000);
                TopicManagerSingleton.get().getTopic("A").publish(new Message(x));
                TopicManagerSingleton.get().getTopic("B").publish(new Message(y));
                sleep(100);
                if (result[0] != x + y + 1) {
                    failures++;
                    if (failures <= 3) {
                        diagFail("simulateMainTrain",
                                "iteration " + i + ": x=" + x + " y=" + y
                                        + " expected=" + (x + y + 1) + " got=" + result[0]
                                        + " (timing/async issue?)");
                    }
                }
            }
            if (failures == 0) {
                diagPass("simulateMainTrain", "all 9 iterations produced x+y+1");
            } else {
                diagFail("simulateMainTrain", failures + "/9 iterations failed (not a crash — logic/timing issue)");
            }

            gc.close();
            sleep(100);

            if (Thread.activeCount() != c) {
                diagFail("simulateMainTrain",
                        "threads after close: expected " + c + ", got " + Thread.activeCount());
            } else {
                diagPass("simulateMainTrain", "thread count restored after close()");
            }
        } catch (Exception e) {
            diagFail("simulateMainTrain", e);
        }
    }

    public static void testDiag_parallelAgentCallbackThread() {
        TopicManagerSingleton.get().clear();
        final String[] threadName = {null};
        ParallelAgent pa = new ParallelAgent(new Agent() {
            @Override public String getName() { return "Inner"; }
            @Override public void reset() {}
            @Override public void callback(String topic, Message msg) {
                threadName[0] = Thread.currentThread().getName();
            }
            @Override public void close() {}
        }, 10);

        try {
            TopicManagerSingleton.get().getTopic("T").subscribe(pa);
            TopicManagerSingleton.get().getTopic("T").publish(new Message(1.0));
            sleep(200);
            if (threadName[0] == null) {
                diagFail("parallelAgentCallbackThread", "callback never ran");
            } else if (threadName[0].equals(Thread.currentThread().getName())) {
                diagFail("parallelAgentCallbackThread", "callback ran on main thread: " + threadName[0]);
            } else {
                diagPass("parallelAgentCallbackThread", "callback ran on worker thread: " + threadName[0]);
            }
        } catch (Exception e) {
            diagFail("parallelAgentCallbackThread", e);
        } finally {
            pa.close();
            sleep(100);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== MainTrain3: Exercise 4 Unit Tests ===\n");

        System.out.println("--- Diagnostic Tests (runtime failure hints) ---");
        testDiag_javaEnvironment();
        testDiag_classNames();
        testDiag_reflectionConstructors();
        testDiag_confFileReadable();
        testDiag_createWithoutConfFile();
        testDiag_genericConfigCreateSafe();
        testDiag_officialConfFormat();
        testDiag_doubleCreate();
        testDiag_parallelAgentCallbackThread();
        testDiag_simulateMainTrain();

        System.out.println("\n--- Functional Tests ---");
        testPlusAgent_addition();
        testPlusAgent_waitsForBothInputs();
        testPlusAgent_nonNumericInput();
        testPlusAgent_sequentialUpdates();
        testIncAgent_increment();
        testIncAgent_nonNumericIgnored();
        testParallelAgent_wrappedPipeline();
        testGenericConfig_simpleConf();
        testGenericConfig_implementsConfig();

        System.out.println("\n=== All tests completed ===");
    }
}
