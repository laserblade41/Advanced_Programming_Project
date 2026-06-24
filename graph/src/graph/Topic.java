package graph;

import java.util.ArrayList;
import java.util.List;

/**
 * A named pub-sub channel in the computational graph.
 *
 * <p>Agents subscribe to a topic to receive messages via {@link Agent#callback}, and
 * register as publishers when they produce output on that topic. Calling {@link #publish}
 * stores the message as the last value and synchronously notifies all subscribers.</p>
 *
 * <p>Instances are created exclusively through
 * {@link TopicManagerSingleton.TopicManager#getTopic}; the constructor is package-private.</p>
 *
 * <p><strong>Thread safety:</strong> {@link #lastMessage} is stored in a {@code volatile}
 * field. The {@link #subs} and {@link #pubs} lists are not synchronized; concurrent
 * subscribe/unsubscribe operations alongside {@link #publish} may race unless external
 * coordination is applied.</p>
 *
 * @see TopicManagerSingleton
 * @see Agent
 * @see Message
 */
public class Topic {

    /** The unique name of this topic. */
    public final String name;

    /** The list of agents subscribed to receive messages from this topic. */
    List<Agent> subs=new ArrayList<>();

    /** The list of agents registered as publishers on this topic. */
    List<Agent> pubs=new ArrayList<>();

    private volatile Message lastMessage = null;

    Topic(String name){
        this.name=name;
    }

    /**
     * Returns the most recently published message on this topic.
     *
     * @return the last {@link Message}, or {@code null} if none has been published yet
     */
    public Message getLastMessage() {
        return lastMessage;
    }

    /**
     * Registers an agent to receive messages published on this topic.
     *
     * @param a the agent to subscribe
     */
    public void subscribe(Agent a){
        subs.add(a);
    }

    /**
     * Removes an agent from this topic's subscriber list.
     *
     * @param a the agent to unsubscribe
     */
    public void unsubscribe(Agent a){
        subs.remove(a);
    }

    /**
     * Publishes a message to this topic and notifies all subscribers.
     *
     * <p>Updates {@link #getLastMessage()} and invokes {@link Agent#callback} on each
     * subscriber synchronously in the calling thread.</p>
     *
     * @param m the message to publish
     */
    public void publish(Message m){
        this.lastMessage = m;
        for(Agent a:subs){
            a.callback(name, m);
        }
    }

    /**
     * Registers an agent as a publisher on this topic.
     *
     * <p>Used for graph visualization to record data-flow direction; does not affect
     * message delivery.</p>
     *
     * @param a the agent to register as a publisher
     */
    public void addPublisher(Agent a){
        pubs.add(a);
    }

    /**
     * Removes an agent from this topic's publisher list.
     *
     * @param a the agent to remove
     */
    public void removePublisher(Agent a){
        pubs.remove(a);
    }

    /**
     * Returns a defensive copy of the publisher list.
     *
     * @return a new {@link List} containing all registered publishers
     */
    public List<Agent> getPubs() {
        return new ArrayList<>(pubs);
    }

    /**
     * Returns a defensive copy of the subscriber list.
     *
     * @return a new {@link List} containing all subscribers
     */
    public List<Agent> getSubs() {
        return new ArrayList<>(subs);
    }
}
