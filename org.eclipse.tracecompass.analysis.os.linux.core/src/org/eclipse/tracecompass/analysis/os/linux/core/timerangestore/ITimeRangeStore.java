package org.eclipse.tracecompass.analysis.os.linux.core.timerangestore;

/**
 * @author efficios
 *
 * @param <T>
 * @since 1.0
 */
public interface ITimeRangeStore<T extends ITimeRange> extends Iterable<T> {

    void addValue(T val);

    long getNbElements();

    T getElementAtIndex(long index);

    Iterable<T> getIntersectingElements(long time);

    void dispose();
}
