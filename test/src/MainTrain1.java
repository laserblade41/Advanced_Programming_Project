package test;

import java.util.Random;

import test.TopicManagerSingleton.TopicManager;

public class MainTrain1 { // simple tests to get you going...

    static String tn=null;

    public static void testMessage() {

        // Test String constructor
        String testString = "Hello";
        Message msgFromString = new Message(testString);
        if (!testString.equals(msgFromString.asText)) {
            System.out.println("Error: String constructor - asText does not match input string (-5)");
        }
        if (!java.util.Arrays.equals(testString.getBytes(), msgFromString.data)) {
            System.out.println("Error: String constructor - data does not match input string bytes (-5)");
        }
        if (!Double.isNaN(msgFromString.asDouble)) {
            System.out.println("Error: String constructor - asDouble should be NaN for non-numeric string (-5)");
        }
        if (msgFromString.date == null) {
            System.out.println("Error: String constructor - date should not be null (-5)");
        }

    }

    public static  abstract class AAgent implements Agent{
        public void reset() {}
        public void close() {}
        public String getName(){
            return getClass().getName();
        }
    }

    public static class TestAgent1 extends AAgent{

        double sum=0;
        int count=0;
        TopicManager tm=TopicManagerSingleton.get();

        public TestAgent1(){
            tm.getTopic("Numbers").subscribe(this);
        }

        @Override
        public void callback(String topic, Message msg) {
            tn=Thread.currentThread().getName();
        }
        
    }

    public static class TestAgent2 extends AAgent{

        double sum=0;
        TopicManager tm=TopicManagerSingleton.get();

        public TestAgent2(){
            tm.getTopic("Sum").subscribe(this);
        }

        @Override
        public void callback(String topic, Message msg) {
            sum=msg.asDouble;
        }

        public double getSum(){
            return sum;
        }
        
    }

    public static void testAgents(){        
        TopicManager tm=TopicManagerSingleton.get();
        TestAgent1 a=new TestAgent1();
        TestAgent2 a2=new TestAgent2();        
        double sum=0;
        for(int c=0;c<3;c++){
            Topic num=tm.getTopic("Numbers");
            Random r=new Random();
            for(int i=0;i<5;i++){
                int x=r.nextInt(1000);
                num.publish(new Message(x));
                sum+=x;
            }
            double result=a2.getSum();
            if(result!=sum){
                System.out.println("your code published a wrong result (-10)");
            }
        }
        a.close();
        a2.close();
    }


    public static void main(String[] args) {
        TopicManager tm=TopicManagerSingleton.get();
        int tc=Thread.activeCount();
        ParallelAgent pa=new ParallelAgent(new TestAgent1(), 10);
        tm.getTopic("A").subscribe(pa);

        if (Thread.activeCount()!=tc+1){
            System.out.println("your ParallelAgent does not open a thread (-10)");
        }


        tm.getTopic("A").publish(new Message("a"));
        try { Thread.sleep(100);} catch (InterruptedException e) {}
        if(tn==null){
            System.out.println("your ParallelAgent didn't run the wrapped agent callback (-20)");
        }else{
            if(tn.equals(Thread.currentThread().getName())){
                System.out.println("the ParallelAgent does not run the wrapped agent in a different thread (-10)");
            }
            String last=tn;
            tm.getTopic("A").publish(new Message("a"));
            try { Thread.sleep(100);} catch (InterruptedException e) {}
            if(!last.equals(tn))
                System.out.println("all messages should be processed in the same thread of ParallelAgent (-10)");
        }

        pa.close();


        System.out.println("done");
    }
}
