package project_biu.configs;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import project_biu.graph.Agent;
import project_biu.graph.ParallelAgent;

/**
 * Reads a text-based description of a publish/subscribe topology and
 * instantiates every agent it describes, wrapped in {@link ParallelAgent}.
 *
 * <p>The file format is three lines per agent:
 * <ol>
 *   <li>fully-qualified class name of the agent (e.g. {@code project_biu.configs.PlusAgent})</li>
 *   <li>comma-separated list of subscription topics</li>
 *   <li>comma-separated list of publication topics</li>
 * </ol>
 *
 * <p>Agents are loaded reflectively via {@code Class.forName(...)} and the
 * {@code (String[], String[])} constructor signature, then wrapped in
 * {@link ParallelAgent} so each one runs its callbacks on its own worker
 * thread. The wrapped agents are kept in a list so {@link #close()} can
 * shut them all down cleanly.
 */
public class GenericConfig implements Config {

    private static final int QUEUE_CAPACITY = 100;

    private String confFile;
    private final List<ParallelAgent> agents = new ArrayList<>();

    /** Constructs an empty {@code GenericConfig}; call {@link #setConfFile(String)} before {@link #create()}. */
    public GenericConfig() { /* no setup needed */ }

    /**
     * Sets the path of the configuration file to load.
     *
     * @param confFile path to the text config file
     */
    public void setConfFile(String confFile) {
        this.confFile = confFile;
    }

    @Override
    public void create() {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(confFile));
        } catch (IOException e) {
            // The spec doesn't say what to do on I/O failure -- bail quietly
            // so the caller can decide.
            return;
        }

        // Drop blank lines so trailing newlines etc. don't break the
        // "lines.size() % 3 == 0" sanity check.
        List<String> cleaned = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (!line.trim().isEmpty()) cleaned.add(line.trim());
        }

        if (cleaned.size() % 3 != 0) return; // malformed file

        for (int i = 0; i < cleaned.size(); i += 3) {
            String className = cleaned.get(i);
            String[] subs = splitCsv(cleaned.get(i + 1));
            String[] pubs = splitCsv(cleaned.get(i + 2));

            try {
                Class<?> cls = Class.forName(className);
                Constructor<?> ctor = cls.getConstructor(String[].class, String[].class);
                Agent agent = (Agent) ctor.newInstance((Object) subs, (Object) pubs);
                agents.add(new ParallelAgent(agent, QUEUE_CAPACITY));
            } catch (ReflectiveOperationException e) {
                // Skip agents we can't instantiate (bad class name, wrong
                // constructor, etc.) rather than blow up the whole config.
            }
        }
    }

    /** Splits "A,B,C" into {"A","B","C"}; empty input yields a zero-length array. */
    private static String[] splitCsv(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return new String[0];
        String[] parts = trimmed.split(",");
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        return parts;
    }

    @Override
    public String getName() {
        return "Generic";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void close() {
        for (ParallelAgent a : agents) a.close();
        agents.clear();
    }
}
