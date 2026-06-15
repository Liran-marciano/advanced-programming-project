package project_biu.configs;

import java.util.function.BinaryOperator;

import project_biu.graph.Agent;
import project_biu.graph.Message;
import project_biu.graph.TopicManagerSingleton;
import project_biu.graph.TopicManagerSingleton.TopicManager;

/**
 * An agent that performs a binary operation over two input topics and
 * publishes the result to one output topic.
 *
 * <p>Wiring is done in the constructor: the agent subscribes to both input
 * topics through the {@link TopicManagerSingleton}, and registers itself as
 * a publisher on the output topic so that the {@code Graph.createFromTopics}
 * pass can later draw the correct edges.
 *
 * <p>A result is published only once <em>both</em> inputs have received at
 * least one numeric {@link Message}, per the spec's "if there is in fact a
 * {@code Double} message in each of the two inputs" rule.
 */
public class BinOpAgent implements Agent {

    private final String name;
    private final String sub1Name;
    private final String sub2Name;
    private final String pubName;
    private final BinaryOperator<Double> op;

    private double x = 0.0;
    private double y = 0.0;
    private boolean hasX = false;
    private boolean hasY = false;

    public BinOpAgent(String name,
                      String sub1Name,
                      String sub2Name,
                      String pubName,
                      BinaryOperator<Double> op) {
        this.name = name;
        this.sub1Name = sub1Name;
        this.sub2Name = sub2Name;
        this.pubName = pubName;
        this.op = op;

        TopicManager tm = TopicManagerSingleton.get();
        tm.getTopic(sub1Name).subscribe(this);
        tm.getTopic(sub2Name).subscribe(this);
        tm.getTopic(pubName).addPublisher(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void reset() {
        x = 0.0;
        y = 0.0;
        hasX = false;
        hasY = false;
    }

    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) return; // ignore non-numeric input

        if (topic.equals(sub1Name)) {
            x = msg.asDouble;
            hasX = true;
        } else if (topic.equals(sub2Name)) {
            y = msg.asDouble;
            hasY = true;
        } else {
            return; // unrelated topic, shouldn't happen
        }

        if (hasX && hasY) {
            double result = op.apply(x, y);
            TopicManagerSingleton.get().getTopic(pubName).publish(new Message(result));
        }
    }

    @Override
    public void close() {
        // No resources to release.
    }
}
