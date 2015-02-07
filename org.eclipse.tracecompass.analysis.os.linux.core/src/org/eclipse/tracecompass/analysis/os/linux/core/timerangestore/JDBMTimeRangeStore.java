package org.eclipse.tracecompass.analysis.os.linux.core.timerangestore;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jdbm.PrimaryStoreMap;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.SecondaryKeyExtractor;
import jdbm.SecondaryTreeMap;

/**
 * @author efficios
 * @since 1.0
 */
public class JDBMTimeRangeStore<T extends ITimeRange> implements ITimeRangeStore<T> {

    private final RecordManager manager;

    private final PrimaryStoreMap<Long, T> mainMap;
    private final SecondaryTreeMap<Long, Long, T> startTimesIndex;
    private final SecondaryTreeMap<Long, Long, T> endTimesIndex;

    private long nbEntries = 0;

    /**
     *
     */
    public JDBMTimeRangeStore() {
        try {
            manager = RecordManagerFactory.createRecordManager("name");
            mainMap = manager.storeMap("map");
        } catch (IOException e) {
            throw new IllegalStateException();
        }

        startTimesIndex = mainMap.secondaryTreeMap("startTimes",
                new SecondaryKeyExtractor<Long, Long, T>() {
                    @Override
                    public Long extractSecondaryKey(Long key, T value) {
                        return value.getStartTime();
                    }
        });

        endTimesIndex = mainMap.secondaryTreeMap("endTimes",
                new SecondaryKeyExtractor<Long, Long, T>() {
                    @Override
                    public Long extractSecondaryKey(Long key, T value) {
                        return value.getEndTime();
                    }
        });
    }

    @Override
    public synchronized void addValue(T val) {
        mainMap.putValue(val);
        if (++nbEntries % 10000 == 0) {
            try {
                manager.commit();
            } catch (IOException e) {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        return mainMap.values().iterator();
    }

    @Override
    public long getNbElements() {
        return nbEntries;
    }

    @Override
    public T getElementAtIndex(long index) {
        return mainMap.get(Long.valueOf(index));
    }

    @Override
    public Iterable<T> getIntersectingElements(long timestamp) {
        Set<Long> matchingStartTimes = startTimesIndex.tailMap(Long.valueOf(timestamp)).keySet();
        Set<Long> matchingEndTimes = endTimesIndex.headMap(Long.valueOf(timestamp)).keySet();
        Set<Long> matchingIntervals = intersection(matchingStartTimes, matchingEndTimes);

        List<T> ret = new LinkedList<>();
        for (Long index : matchingIntervals) {
            ret.add(mainMap.get(Long.valueOf(index)));
        }
        return ret;
    }

    /**
     * Compute the intersection of two sets
     */
    private static <V> Set<V> intersection(Set<V> set1, Set<V> set2) {
        Set<V> ret = new TreeSet<>();
        ret.addAll(set1);
        ret.addAll(set2);
        return ret;
    }

    @Override
    public void dispose() {
        try {
            manager.close();
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

}
