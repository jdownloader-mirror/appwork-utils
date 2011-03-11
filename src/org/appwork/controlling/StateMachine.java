package org.appwork.controlling;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.utils.logging.Log;

public class StateMachine {

    private static State checkState(final State state) {
        State finalState = null;
        for (final State s : state.getChildren()) {
            final State ret = StateMachine.checkState(s);
            if (finalState == null) {
                finalState = ret;
            }
            if (finalState != ret) { throw new StateConflictException("States do not all result in one common final state"); }
        }
        if (finalState == null) { throw new StateConflictException(state + " is a blind state (has no children)"); }
        return finalState;
    }

    /**
     * validates a statechain and checks if all states can be reached, and if
     * all chans result in one common finalstate
     * 
     * @param initState
     * @throws StateConflictException
     */
    public static void validateStateChain(final State initState) {
        if (initState.getParents().size() > 0) { throw new StateConflictException("initState must not have a parent"); }
        StateMachine.checkState(initState);
    }

    private final State                     initState;
    private volatile State                  currentState;
    private final StateEventsender          eventSender;

    private final State                     finalState;
    private final ArrayList<StatePathEntry> path;
    private final StateMachineInterface     owner;
    private final Object                    lock  = new Object();

    private final Object                    lock2 = new Object();

    private final HashMap<State, Throwable> exceptionMap;

    public StateMachine(final StateMachineInterface interfac, final State startState, final State endState) {
        owner = interfac;
        initState = startState;
        currentState = startState;
        finalState = endState;
        exceptionMap = new HashMap<State, Throwable>();
        eventSender = new StateEventsender();
        path = new ArrayList<StatePathEntry>();
        path.add(new StatePathEntry(initState));
    }

    public void addListener(final StateEventListener listener) {
        eventSender.addListener(listener);
    }

    /**
     * synchronized execution of a runnable if statemachine is currently in a
     * given state
     * 
     * @param run
     * @param state
     * @return
     */
    public boolean executeIfOnState(final Runnable run, final State state) {
        if (run == null || state == null) { return false; }
        synchronized (lock) {
            if (isState(state)) {
                run.run();
                return true;
            }
        }
        return false;
    }

    /*
     * synchronized hasPassed/addListener to start run when state has
     * reached/passed
     */
    public void executeOnceOnState(final Runnable run, final State state) {
        if (run == null || state == null) { return; }
        boolean reached = false;
        synchronized (lock) {
            if (hasPassed(state)) {
                reached = true;
            } else {
                addListener(new StateListener(state) {
                    @Override
                    public void onStateReached(final StateEvent event) {
                        StateMachine.this.removeListener(this);
                        run.run();
                    }
                });
            }
            if (reached) {
                run.run();
            }
        }
    }

    public void fireUpdate(final State currentState) {
        if (currentState != null) {
            synchronized (lock) {
                if (this.currentState != currentState) { throw new StateConflictException("Cannot update state " + currentState + " because current state is " + this.currentState); }
            }
        }
        final StateEvent event = new StateEvent(this, StateEvent.Types.UPDATED, currentState, currentState);
        eventSender.fireEvent(event);
    }

    public void forceState(final State newState) {
        StateEvent event;
        synchronized (lock) {
            if (currentState == newState) { return; }
            event = new StateEvent(this, StateEvent.Types.CHANGED, currentState, newState);
            synchronized (lock2) {
                path.add(new StatePathEntry(newState));
            }
            Log.L.finest(owner + " State changed " + currentState + " -> " + newState);
            currentState = newState;
        }
        eventSender.fireEvent(event);
    }

    public Throwable getCause(final State newState) {
        return exceptionMap.get(newState);
    }

    /**
     * TODO: not synchronized
     * 
     * @param failedState
     * @return
     */
    public StatePathEntry getLatestStateEntry(final State failedState) {
        try {
            StatePathEntry entry = null;
            synchronized (lock2) {
                for (int i = path.size() - 1; i >= 0; i--) {
                    entry = path.get(i);
                    if (entry.getState() == failedState) { return entry; }
                }
            }
        } catch (final Exception e) {
        }
        return null;
    }

    public StateMachineInterface getOwner() {
        return owner;
    }

    /**
     * @return the path
     */
    public ArrayList<StatePathEntry> getPath() {
        return path;
    }

    public State getState() {
        return currentState;
    }

    // public void forceState(int id) {
    //
    // State newState;
    // synchronized (lock) {
    // newState = getStateById(this.initState, id, null);
    // if (newState == null) throw new
    // StateConflictException("No State with ID " + id);
    // }
    // forceState(newState);
    // }

    public boolean hasPassed(final State... states) {
        synchronized (lock2) {
            for (final State s : states) {
                for (final StatePathEntry e : path) {
                    if (e.getState() == s) { return true; }
                }
            }
        }
        return false;
    }

    // private State getStateById(State startState, int id, ArrayList<State>
    // foundStates) {
    //
    // if (foundStates == null) foundStates = new ArrayList<State>();
    // if (foundStates.contains(startState)) return null;
    // foundStates.add(startState);
    // State ret = null;
    // for (State s : startState.getChildren()) {
    //
    // if (s.getID() == id) return s;
    // ret = getStateById(s, id, foundStates);
    // if (ret != null) return ret;
    // }
    // return null;
    // }

    public boolean isFinal() {
        synchronized (lock) {
            return finalState == currentState;
        }
    }

    /**
     * returns if the statemachine is in startstate currently
     */
    public boolean isStartState() {
        synchronized (lock) {
            return currentState == initState;
        }
    }

    public boolean isState(final State... states) {
        synchronized (lock) {
            for (final State s : states) {
                if (s == currentState) { return true; }
            }
        }
        return false;
    }

    public void removeListener(final StateEventListener listener) {
        eventSender.removeListener(listener);
    }

    public void reset() {
        StateEvent event;
        synchronized (lock) {
            if (currentState == initState) { return; }
            if (finalState != currentState) { throw new StateConflictException("Cannot reset from state " + currentState); }
            event = new StateEvent(this, StateEvent.Types.CHANGED, currentState, initState);
            Log.L.finest(owner + " State changed (reset) " + currentState + " -> " + initState);
            currentState = initState;
            synchronized (lock2) {
                path.clear();
                path.add(new StatePathEntry(initState));
            }
        }
        eventSender.fireEvent(event);
    }

    public void setCause(final State failedState, final Throwable e) {
        exceptionMap.put(failedState, e);
    }

    public void setStatus(final State newState) {
        synchronized (lock) {
            if (currentState == newState) { return; }
            if (!currentState.getChildren().contains(newState)) { throw new StateConflictException("Cannot change state from " + currentState + " to " + newState); }
        }
        forceState(newState);
    }

    /**
     * Throws a StateViolationException if the current state is not state
     * 
     * @param downloadBranchlist
     */
    public void validateState(final State state) {
        if (!isState(state)) { throw new StateViolationException(state); }
    }
}
