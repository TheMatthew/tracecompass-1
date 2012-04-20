package org.eclipse.linuxtools.tmf.core.ctfadaptor;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.ctf.core.trace.CTFTrace;
import org.eclipse.linuxtools.tmf.core.component.TmfEventProvider;
import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.request.ITmfDataRequest;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignal;
import org.eclipse.linuxtools.tmf.core.signal.TmfSignalManager;
import org.eclipse.linuxtools.tmf.core.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.core.trace.ITmfLocation;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class CtfTmfTrace extends TmfEventProvider<CtfTmfEvent> implements ITmfTrace<CtfTmfEvent> {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    // the Ctf Trace
    private CTFTrace fTrace;

    // The number of events collected
    protected long fNbEvents = 0;

    // The time span of the event stream
    private ITmfTimestamp fStartTime = TmfTimestamp.BIG_CRUNCH;
    private ITmfTimestamp fEndTime = TmfTimestamp.BIG_BANG;

    // The trace resource
    private IResource fResource;

    /*
     * Since in TMF, "traces" can read events, this trace here will have its own
     * iterator. The user can instantiate extra iterator if they want to seek at
     * many places at the same time.
     */
    protected CtfIterator iterator;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public CtfTmfTrace() {
        super();
    }

    @Override
    public void initTrace(final IResource resource, final String path, final Class<CtfTmfEvent> eventType)
            throws FileNotFoundException {
        this.fResource = resource;
        try {
            this.fTrace = new CTFTrace(path);
        } catch (final CTFReaderException e) {
            /*
             * If it failed at the init(), we can assume it's because the file
             * was not found or was not recognized as a CTF trace. Throw into
             * the new type of exception expected by the rest of TMF.
             */
            throw new FileNotFoundException(e.getMessage());
        }
        this.iterator = new CtfIterator(this, 0, 0);
        setStartTime(iterator.getCurrentEvent().getTimestamp());
        TmfSignalManager.register(this);
        // this.currLocation.setTimestamp(this.fEvent.getTimestamp().getValue());
        // this.fStartTime = new TmfSimpleTimestamp(this.currLocation
        // .getLocation().getStartTime());
        // this.fEndTime = new TmfSimpleTimestamp(this.currLocation
        // .getLocation().getEndTime());
        // setTimeRange(new TmfTimeRange(this.fStartTime.clone(),
        // this.fEndTime.clone()));
    }

    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);
    }

    @Override
    public void broadcast(final TmfSignal signal) {
        TmfSignalManager.dispatchSignal(signal);
    }

    @Override
    public boolean validate(final IProject project, final String path) {
        try {
            final CTFTrace temp = new CTFTrace(path);
            return temp.majortIsSet(); // random test
        } catch (final CTFReaderException e) {
            /* Nope, not a CTF trace we can read */
            return false;
        }
    }

    @Override
    public CtfTmfTrace clone() throws CloneNotSupportedException {
        CtfTmfTrace clone = null;
        clone = (CtfTmfTrace) super.clone();
        clone.fStartTime = this.fStartTime.clone();
        clone.fEndTime = this.fEndTime.clone();
        clone.fTrace = this.fTrace;
        return clone;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * @return the trace path
     */
    @Override
    public Class<CtfTmfEvent> getType() {
        return fType;
    }

    public int getNbEnvVars() {
        return this.fTrace.getEnvironment().size();
    }


    public String[] getEnvNames() {
        final String[] s = new String[getNbEnvVars()];
        return this.fTrace.getEnvironment().keySet().toArray(s);
    }

    public String getEnvValue(final String key)    {
        return this.fTrace.getEnvironment().get(key);
    }


    /**
     * @return the trace path
     */
    @Override
    public String getPath() {
        return this.fTrace.getPath();
    }

    @Override
    public String getName() {
        final String temp[] = this.fTrace.getPath().split(
                System.getProperty("file.separator")); //$NON-NLS-1$
        if (temp.length > 2)
            return temp[temp.length - 1];
        return temp[0];
    }

    @Override
    public int getCacheSize() {
        return 50000; // not true, but it works
    }

    @Override
    public long getNbEvents() {
        return this.fNbEvents;
    }

    @Override
    public TmfTimeRange getTimeRange() {
        return new TmfTimeRange(this.fStartTime, this.fEndTime);
    }

    @Override
    public ITmfTimestamp getStartTime() {
        return this.fStartTime;
    }

    @Override
    public ITmfTimestamp getEndTime() {
        return this.fEndTime;
    }

    @Override
    public ITmfLocation<?> getCurrentLocation() {
        return iterator.getLocation();
    }

    // ------------------------------------------------------------------------
    // Operators
    // ------------------------------------------------------------------------

    protected void setTimeRange(final TmfTimeRange range) {
        this.fStartTime = range.getStartTime();
        this.fEndTime = range.getEndTime();
    }

    protected void setStartTime(final ITmfTimestamp startTime) {
        this.fStartTime = startTime;
    }

    protected void setEndTime(final ITmfTimestamp endTime) {
        this.fEndTime = endTime;
    }

    // ------------------------------------------------------------------------
    // TmfProvider
    // ------------------------------------------------------------------------

    @Override
    public ITmfContext armRequest(final ITmfDataRequest<CtfTmfEvent> request) {
        if ((request instanceof ITmfEventRequest<?>)
                && !TmfTimestamp.BIG_BANG
                .equals(((ITmfEventRequest<CtfTmfEvent>) request)
                        .getRange().getStartTime())
                        && (request.getIndex() == 0)) {
            final ITmfContext context = seekEvent(((ITmfEventRequest<CtfTmfEvent>) request)
                    .getRange().getStartTime());
            ((ITmfEventRequest<CtfTmfEvent>) request)
            .setStartIndex((int) context.getRank());
            return context;
        }
        return seekEvent(request.getIndex());
    }

    /**
     * The trace reader keeps its own iterator: the "context" parameter here
     * will be ignored.
     *
     * If you wish to specify a new context, instantiate a new CtfIterator and
     * seek() it to where you want, and use that to read events.
     *
     * FIXME merge with getNextEvent below once they both use the same parameter
     * type.
     */
    @Override
    public CtfTmfEvent getNext(final ITmfContext context) {
        iterator.advance();
        return iterator.getCurrentEvent();
    }

    // ------------------------------------------------------------------------
    // ITmfTrace
    // ------------------------------------------------------------------------

    @Override
    public ITmfContext seekLocation(final ITmfLocation<?> location) {
        CtfLocation currentLocation = (CtfLocation) location;
        if (currentLocation == null)
            currentLocation = new CtfLocation(0L);
        iterator.setLocation(currentLocation);
        return iterator;
    }

    @Override
    public double getLocationRatio(final ITmfLocation<?> location) {
        final CtfLocation curLocation = (CtfLocation) location;
        iterator.seek(curLocation.getLocation());
        return ((double) iterator.getCurrentEvent().getTimestampValue() - iterator
                .getStartTime())
                / (iterator.getEndTime() - iterator.getStartTime());
    }

    @Override
    public long getStreamingInterval() {
        return 0;
    }

    @Override
    public ITmfContext seekEvent(final ITmfTimestamp timestamp) {
        iterator.seek(timestamp.getValue());
        return iterator;
    }

    /**
     * Seek by rank
     */
    @Override
    public ITmfContext seekEvent(final long rank) {
        iterator.setRank(rank);
        return iterator;
    }

    /**
     * Seek rank ratio
     */
    @Override
    public ITmfContext seekLocation(final double ratio) {
        iterator.seek((long) (this.fNbEvents * ratio));
        return iterator;
    }

    @Override
    public CtfTmfEvent getNextEvent(final ITmfContext context) {
        iterator.advance();
        return iterator.getCurrentEvent();
    }

    @Override
    public CtfTmfEvent parseEvent(final ITmfContext context) {
        return iterator.getCurrentEvent();
    }

    @Override
    public IResource getResource() {
        return this.fResource;
    }

    CTFTrace getCTFTrace() {
        return fTrace;
    }



}