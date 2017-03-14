package it.unibo.alchemist.loader.displacements;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.random.RandomGenerator;

import it.unibo.alchemist.model.interfaces.Environment;
import it.unibo.alchemist.model.interfaces.Position;

/**
 *
 */
public abstract class AbstractRandomDisplacement implements Displacement {

    private final Environment<?> env;
    private final RandomGenerator rng;
    private final int nodes;

    /**
     * @param env
     *            the {@link Environment}
     * @param rng
     *            the {@link RandomGenerator}
     * @param nodes
     *            the number of nodes
     */
    public AbstractRandomDisplacement(final Environment<?> env, final RandomGenerator rng, final int nodes) {
        this.env = env;
        this.rng = rng;
        this.nodes = nodes;
    }

    @Override
    public Stream<Position> stream() {
        return IntStream.range(0, nodes).mapToObj(this::indexToPosition);
    }

    /**
     * Builds a position, relying on the internal {@link Environment}.
     * 
     * @see Environment#makePosition(Number...)
     * 
     * @param coordinates the coordinates
     * @return a position
     */
    protected final Position makePosition(final Number... coordinates) {
        return env.makePosition(coordinates);
    }

    /**
     * @param from
     *            minimum value
     * @param to
     *            maximum value
     * @return a random uniformly distributed in such range
     */
    protected final double randomDouble(final double from, final double to) {
        return rng.nextDouble() * Math.abs(to - from) + Math.min(from, to);
    }

    /**
     * @return a random double in the [0, 1] interval
     */
    protected final double randomDouble() {
        return rng.nextDouble();
    }

    /**
     * @param from
     *            minimum value
     * @param toExclusive
     *            maximum value (exclusive)
     * @return a random uniformly distributed in such range
     */
    protected final int randomInt(final int from, final int toExclusive) {
        return rng.nextInt(Math.abs(toExclusive - from)) + Math.min(from, toExclusive);
    }

    /**
     * @param i
     *            the node number
     * @return the position of the node
     */
    protected abstract Position indexToPosition(int i);


}
