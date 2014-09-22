/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package machine;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

/**
 *
 * @author jsanchez
 */
public class Clock {
    private long tstates;
    private long frames;
    private long timeout;
    private final ArrayList<ClockTimeoutListener> clockListeners = new ArrayList<ClockTimeoutListener>();

    public Clock() {

    }

    /**
     * Adds a new event listener to the list of event listeners.
     *
     * @param listener The new event listener.
     *
     * @throws NullPointerException Thrown if the listener argument is null.
     */
    public void addClockTimeoutListener(final ClockTimeoutListener listener) {

        if (listener == null) {
            throw new NullPointerException("Error: Listener can't be null");
        }

        if (!clockListeners.contains(listener)) {
            clockListeners.add(listener);
        }
    }

    /**
     * Remove a new event listener from the list of event listeners.
     *
     * @param listener The event listener to remove.
     *
     * @throws NullPointerException Thrown if the listener argument is null.
     * @throws IllegalArgumentException Thrown if the listener wasn't registered.
     */
    public void removeClockTimeoutListener(final ClockTimeoutListener listener) {

        if (listener == null) {
            throw new NullPointerException("Internal Error: Listener can't be null");
        }

        if (clockListeners.contains(listener)) {
            clockListeners.remove(listener);
        }
    }

    /**
     * @return the tstates
     */
    public long getTstates() {
        return tstates;
    }

    /**
     * @param states the tstates to set
     */
    public void setTstates(long states) {
        tstates = states;
        frames = 0;
    }

    public void addTstates(long states) {
        tstates += states;

        if (timeout > 0) {
            timeout -= states;
            if (timeout < 1) {
                for (final ClockTimeoutListener listener : clockListeners) {
                    listener.clockTimeout();
                }
            }
        }
    }

    public long getFrames() {
        return frames;
    }

    public void reset() {
        frames = tstates = 0;
    }

    public void setTimeout(long ntstates) {
        timeout = ntstates > 0 ? ntstates : 1;
    }
  
} 
