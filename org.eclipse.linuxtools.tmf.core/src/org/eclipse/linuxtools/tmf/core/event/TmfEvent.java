/*******************************************************************************
 * Copyright (c) 2009, 2012 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Francois Chouinard - Updated as per TMF Event Model 1.0
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.event;

import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * <b><u>TmfEvent</u></b>
 * <p>
 * A basic implementation of ITmfEvent.
 * 
 * Note that for performance reasons TmfEvent is NOT immutable. If a shallow
 * copy of the event is needed, use the copy constructor. Otherwise (deep copy)
 * use clone().
 */
public class TmfEvent implements ITmfEvent {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The trace containing the event
     */
    protected ITmfTrace<? extends TmfEvent> fTrace;

    /**
     * The event rank within the trace
     */
    protected long fRank;
    
    /**
     * The event timestamp
     */
    protected ITmfTimestamp fTimestamp;
    
    /**
     * The event source
     */
    protected String fSource;
    
    /**
     * The event type
     */
    protected ITmfEventType fType;
    
    /**
     * The event content (root field)
     */
    protected ITmfEventField fContent;
    
    /**
     * The event reference
     */
    protected String fReference;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     */
    public TmfEvent() {
        fTrace = null;
        fRank = -1;
        fTimestamp = null;
        fSource = null;
        fType = null;
        fContent = null;
        fReference = null;
    }

    /**
     * Full constructor
     * 
     * @param trace the parent trace
     * @param rank the event rank (in the trace)
     * @param timestamp the event timestamp
     * @param source the event source
     * @param type the event type
     * @param type the event content (payload)
     * @param reference the event reference
     */
    public TmfEvent(ITmfTrace<? extends TmfEvent> trace, long rank, ITmfTimestamp timestamp, String source,
                    ITmfEventType type, ITmfEventField content, String reference)
    {
        fTrace = trace;
        fRank = rank;
        fTimestamp = timestamp;
        fSource = source;
        fType = type;
        fContent = content;
        fReference = reference;
    }

    /**
     * Constructor - no rank
     */
    public TmfEvent(ITmfTrace<? extends TmfEvent> trace, ITmfTimestamp timestamp, String source,
            ITmfEventType type, ITmfEventField content, String reference)
    {
        this(trace, -1, timestamp, source, type, content, reference);
    }

    /**
     * Constructor - no rank, no content
     */
    public TmfEvent(ITmfTrace<? extends TmfEvent> trace, ITmfTimestamp timestamp, String source,
            ITmfEventType type, String reference)
    {
        this(trace, -1, timestamp, source, type, null, reference);
    }

    /**
     * Constructor - no rank, no content, no trace
     */
    public TmfEvent(ITmfTimestamp timestamp, String source, ITmfEventType type, String reference)
    {
        this(null, -1, timestamp, source, type, null, reference);
    }

    /**
     * Copy constructor
     * 
     * @param event the original event
     */
    public TmfEvent(TmfEvent event) {
        if (event == null)
            throw new IllegalArgumentException();
        fTrace = event.fTrace;
        fRank = event.fRank;
        fTimestamp = event.fTimestamp;
        fSource = event.fSource;
        fType = event.fType;
        fContent = event.fContent;
        fReference = event.fReference;
    }

    // ------------------------------------------------------------------------
    // ITmfEvent
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.event.ITmfEvent#getTrace()
     */
    @Override
    public ITmfTrace<? extends TmfEvent> getTrace() {
        return fTrace;
    }

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.event.ITmfEvent#getRank()
     */
    @Override
    public long getRank() {
        return fRank;
    }

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.event.ITmfEvent#getTimestamp()
     */
    @Override
    public ITmfTimestamp getTimestamp() {
        return fTimestamp;
    }

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.event.ITmfEvent#getSource()
     */
    @Override
    public String getSource() {
        return fSource;
    }

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.event.ITmfEvent#getType()
     */
    @Override
    public ITmfEventType getType() {
        return fType;
    }

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.event.ITmfEvent#getContent()
     */
    @Override
    public ITmfEventField getContent() {
        return fContent;
    }

    /* (non-Javadoc)
     * @see org.eclipse.linuxtools.tmf.core.event.ITmfEvent#getReference()
     */
    @Override
    public String getReference() {
        return fReference;
    }

    // ------------------------------------------------------------------------
    // Convenience setters
    // ------------------------------------------------------------------------

    /**
     * @param source the new event source
     */
    public void setSource(String source) {
        fSource = source;
    }

    /**
     * @param timestamp the new event timestamp
     */
    public void setTimestamp(ITmfTimestamp timestamp) {
        fTimestamp = timestamp;
    }

    /**
     * @param type the new event type
     */
    public void setType(ITmfEventType type) {
        fType = type;
    }

    /**
     * @param content the event new content
     */
    public void setContent(ITmfEventField content) {
        fContent = content;
    }

    /**
     * @param reference the new event reference
     */
    public void setReference(String reference) {
        fReference = reference;
    }

    // ------------------------------------------------------------------------
    // Cloneable
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public TmfEvent clone() {
        TmfEvent clone = null;
        try {
            clone = (TmfEvent) super.clone();
            clone.fTrace = fTrace;
            clone.fRank = fRank;
            clone.fTimestamp = fTimestamp != null ? fTimestamp.clone() : null;
            clone.fSource = fSource;
            clone.fType = fType != null ? fType.clone() : null;
            clone.fContent = fContent != null ? fContent.clone() : null;
            clone.fReference = fReference;
        } catch (CloneNotSupportedException e) {
        }
        return clone;
    }

    // ------------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fTrace == null) ? 0 : fTrace.hashCode());
        result = prime * result + (int) (fRank ^ (fRank >>> 32));
        result = prime * result + ((fTimestamp == null) ? 0 : fTimestamp.hashCode());
        result = prime * result + ((fSource == null) ? 0 : fSource.hashCode());
        result = prime * result + ((fType == null) ? 0 : fType.hashCode());
        result = prime * result + ((fContent == null) ? 0 : fContent.hashCode());
        result = prime * result + ((fReference == null) ? 0 : fReference.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TmfEvent other = (TmfEvent) obj;
        if (fTrace == null) {
            if (other.fTrace != null)
                return false;
        } else if (!fTrace.equals(other.fTrace))
            return false;
        if (fRank != other.fRank)
            return false;
        if (fTimestamp == null) {
            if (other.fTimestamp != null)
                return false;
        } else if (!fTimestamp.equals(other.fTimestamp))
            return false;
        if (fSource == null) {
            if (other.fSource != null)
                return false;
        } else if (!fSource.equals(other.fSource))
            return false;
        if (fType == null) {
            if (other.fType != null)
                return false;
        } else if (!fType.equals(other.fType))
            return false;
        if (fContent == null) {
            if (other.fContent != null)
                return false;
        } else if (!fContent.equals(other.fContent))
            return false;
        if (fReference == null) {
            if (other.fReference != null)
                return false;
        } else if (!fReference.equals(other.fReference))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    @SuppressWarnings("nls")
    public String toString() {
        return "TmfEvent [fTimestamp=" + fTimestamp + ", fTrace=" + fTrace + ", fRank=" + fRank
                        + ", fSource=" + fSource + ", fType=" + fType + ", fContent=" + fContent
                        + ", fReference=" + fReference + "]";
    }

}