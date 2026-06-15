package project_biu.configs;

/**
 * The {@code (a+b) * (a-b)} sample computational graph from the Exercise 3 PDF.
 *
 * <p>It instantiates three {@link BinOpAgent}s:
 * <ul>
 *   <li>{@code plus}  : reads {@code A, B}, writes {@code R1}</li>
 *   <li>{@code minus} : reads {@code A, B}, writes {@code R2}</li>
 *   <li>{@code mul}   : reads {@code R1, R2}, writes {@code R3}</li>
 * </ul>
 */
public class MathExampleConfig implements Config {

    @Override
    public void create() {
        new BinOpAgent("plus",  "A", "B", "R1", (x, y) -> x + y);
        new BinOpAgent("minus", "A", "B", "R2", (x, y) -> x - y);
        new BinOpAgent("mul",   "R1", "R2", "R3", (x, y) -> x * y);
    }

    @Override
    public String getName() {
        return "Math Example";
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void close() {
        // The plain BinOpAgents we created do not hold resources.
    }
}
