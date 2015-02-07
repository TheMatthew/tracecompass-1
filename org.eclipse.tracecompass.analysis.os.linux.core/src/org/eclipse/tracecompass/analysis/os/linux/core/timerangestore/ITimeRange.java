package org.eclipse.tracecompass.analysis.os.linux.core.timerangestore;

import java.io.Serializable;

/**
 * @author efficios
 * @since 1.0
 */
public interface ITimeRange extends Serializable {

    long getStartTime();

    long getEndTime();

    long getDuration();
}
