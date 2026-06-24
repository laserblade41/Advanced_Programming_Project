package graph;

import java.util.ArrayList;
import java.util.List;

public class Topic {
    public final String name;
    List<Agent> subs=new ArrayList<>();
    List<Agent> pubs=new ArrayList<>();

    private volatile Message lastMessage = null;

    Topic(String name){
        this.name=name;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void subscribe(Agent a){
        subs.add(a);
    }
    public void unsubscribe(Agent a){
        subs.remove(a);
    }
    public void publish(Message m){
        this.lastMessage = m;
        for(Agent a:subs){
            a.callback(name, m);
        }
    }
    public void addPublisher(Agent a){
        pubs.add(a);
    }

    public void removePublisher(Agent a){
        pubs.remove(a);
    }

    public List<Agent> getPubs() {
        return new ArrayList<>(pubs);
    }

    public List<Agent> getSubs() {
        return new ArrayList<>(subs);
    }
}
