package project_biu.graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Active Object decorator for {@link Agent}.
 *
 * <p>{@code ParallelAgent} wraps another {@link Agent} so that the caller of
 * {@link #callback(String, Message)} is never blocked by the wrapped agent's
 * work. The caller simply enqueues the {@code (topic, message)} pair into a
 * bounded blocking queue and returns; a dedicated worker thread, started in
 * the constructor, dequeues items one by one and invokes the wrapped agent.
 *
 * <p>This is the publish/subscribe analogue of the Active Object pattern from
 * the course's week-2 PDF. It also gives every "agent" in a computational
 * graph its own thread, which lets agents that perform long-running work not
 * stall the topic that delivers messages to them.
 */
public class ParallelAgent implements Agent {

    /** The agent whose work we are wrapping. */
    private final Agent agent;

    /** Bounded queue of pending {@code (topic, message)} pairs. */
    private final BlockingQueue<QueueItem> queue;

    /** Worker thread that drains the queue. */
    private final Thread worker;

    /** Set by {@link #close()} to ask the worker to exit. */
    private volatile boolean stopped = false;

    /** Tiny pair holder used as the queue's element type. */
    private static final class QueueItem {
        final String topic;
        final Message msg;

        QueueItem(String topic, Message msg) {
            this.topic = topic;
            this.msg = msg;
        }
    }

    /**
     * Wraps {@code agent} behind a queue of at most {@code capacity} pending
     * callbacks. Starts the worker thread immediately.
     *
     * @param agent    the agent whose callbacks will run on the worker thread
     * @param capacity maximum number of pending callbacks held in the queue
     */
    public ParallelAgent(Agent agent, int capacity) {
        this.agent = agent;
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.worker = new Thread(this::drain, "ParallelAgent-" + agent.getName());
        this.worker.start();
    }

    /**
     * Worker loop. Blocks on {@link BlockingQueue#take()} when the queue is
     * empty (no busy-waiting) and exits cleanly when {@link #close()} sets
     * {@link #stopped} and then interrupts the thread.
     */
    private void drain() {
        while (!stopped) {
            try {
                QueueItem item = queue.take();
                agent.callback(item.topic, item.msg);
            } catch (InterruptedException e) {
                // close() interrupts us; re-check the stopped flag and exit.
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public String getName() {
        return agent.getName();
    }

    @Override
    public void reset() {
        agent.reset();
    }

    @Override
    public void callback(String topic, Message msg) {
        try {
            queue.put(new QueueItem(topic, msg));
        } catch (InterruptedException e) {
            // Preserve interrupt status but do not throw checked exception
            // from the Agent interface signature.
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        stopped = true;
        worker.interrupt();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        agent.close();
    }
}
