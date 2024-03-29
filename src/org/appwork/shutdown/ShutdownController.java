package org.appwork.shutdown;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;

public class ShutdownController extends Thread {
    class ShutdownEventWrapper extends ShutdownEvent {

        private final Thread orgThread;

        /**
         * @param value
         */
        public ShutdownEventWrapper(final Thread value) {
            orgThread = value;
            // call "Nativ" hooks at the end.
            setHookPriority(Integer.MIN_VALUE);
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof ShutdownEventWrapper) { return orgThread == ((ShutdownEventWrapper) obj).orgThread; }
            return false;
        }

        @Override
        public int hashCode() {

            return orgThread.hashCode();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.appwork.shutdown.ShutdownEvent#run()
         */
        @Override
        public void onShutdown(final ShutdownRequest shutdownRequest) {
            orgThread.run();
        }

        @Override
        public String toString() {
            return "ShutdownEventWrapper " + orgThread + " - " + orgThread.getClass().getName() + " Priority: " + getHookPriority();
        }

    }

    private static final ShutdownController INSTANCE = new ShutdownController();

    /**
     * get the only existing instance of ShutdownController. This is a singleton
     * 
     * @return
     */
    public static ShutdownController getInstance() {
        return ShutdownController.INSTANCE;
    }

    public static void main(final String[] args) {
        Log.L.setLevel(Level.ALL);
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public int getHookPriority() {
                return 1;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                Log.L.finest("DO " + getHookPriority());
            }

        });

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public int getHookPriority() {
                return 3;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                Log.L.finest("DO " + getHookPriority());

            }

        });

        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public int getHookPriority() {
                return 2;
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                Log.L.finest("DO " + getHookPriority());
            }

        });
    }

    private final ArrayList<ShutdownEvent>             hooks;
    private final ArrayList<ShutdownEvent>             originalShutdownHooks;
    private final java.util.List<ShutdownVetoListener> vetoListeners;

    private int                                        exitCode           = 0;
    private final AtomicInteger                        requestedShutDowns = new AtomicInteger(0);

    private volatile Thread                            exitThread         = null;

    private final AtomicBoolean                        shutDown           = new AtomicBoolean(false);
    protected ShutdownRequest                          shutdownRequest;

    /**
     * Create a new instance of ShutdownController. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private ShutdownController() {
        super(ShutdownController.class.getSimpleName());

        hooks = new ArrayList<ShutdownEvent>();
        originalShutdownHooks = new ArrayList<ShutdownEvent>();
        vetoListeners = new ArrayList<ShutdownVetoListener>();
        try {
            // first try to hook in the original hooks manager
            // to "disable" the original hook manager, we overwrite the actual
            // hook list with our own one, and redirect all registered hooks to
            // ShutdownController.
            // this may fail (reflektion). As a fallback we just add
            // Shutdowncontroller as a normal hook.

            final IdentityHashMap<Thread, Thread> hookDelegater = new IdentityHashMap<Thread, Thread>() {
                /**
                 * 
                 */
                private static final long serialVersionUID = 8334628124340671103L;

                {
                    // SHutdowncontroller should be the only hook!!
                    super.put(ShutdownController.this, ShutdownController.this);
                }

                @Override
                public Thread put(final Thread key, final Thread value) {
                    final ShutdownEventWrapper hook = new ShutdownEventWrapper(value);

                    ShutdownController.this.addShutdownEvent(hook);
                    return null;
                }

                @Override
                public Thread remove(final Object key) {
                    ShutdownController.this.removeShutdownEvent(new ShutdownEventWrapper((Thread) key));
                    return (Thread) key;
                }
            };

            final Field field = Class.forName("java.lang.ApplicationShutdownHooks").getDeclaredField("hooks");
            field.setAccessible(true);
            final Map<Thread, Thread> hooks = (Map<Thread, Thread>) field.get(null);
            synchronized (hooks) {

                final Set<Thread> threads = hooks.keySet();

                for (final Thread hook : threads) {
                    addShutdownEvent(new ShutdownEventWrapper(hook));

                }
                field.set(null, hookDelegater);
            }

        } catch (final Throwable e) {
            Log.exception(e);
            Runtime.getRuntime().addShutdownHook(this);
        }

        // do not remove the Log call here. we have to be sure that Log.class is
        // already loaded
        Log.L.finest("Init ShutdownController");
        addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                Log.closeLogfile();

            }
        });
    }

    public void addShutdownEvent(final ShutdownEvent event) {
        if (isAlive()) {
            Log.exception(new IllegalStateException("Cannot add hooks during shutdown"));
            return;
        }
        if (event instanceof ShutdownEventWrapper) {
            synchronized (originalShutdownHooks) {
                originalShutdownHooks.add(event);
            }
        } else {
            synchronized (hooks) {
                ShutdownEvent next;
                int i = 0;
                // add event sorted
                for (final Iterator<ShutdownEvent> it = hooks.iterator(); it.hasNext();) {
                    next = it.next();
                    if (next.getHookPriority() <= event.getHookPriority()) {
                        hooks.add(i, event);
                        return;
                    }
                    i++;
                }
                hooks.add(event);
            }
        }

    }

    public void addShutdownVetoListener(final ShutdownVetoListener listener) {
        synchronized (vetoListeners) {
            if (vetoListeners.contains(listener)) { return; }
            Log.L.finest("ADD " + listener);
            vetoListeners.add(listener);
            java.util.Collections.sort(vetoListeners, new Comparator<ShutdownVetoListener>() {

                @Override
                public int compare(final ShutdownVetoListener o1, final ShutdownVetoListener o2) {

                    return new Long(o1.getShutdownVetoPriority()).compareTo(new Long(o2.getShutdownVetoPriority()));
                }
            });
        }
    }

    /**
     * @return
     * @return
     */
    public ShutdownRequest collectVetos(final ShutdownRequest request) {
        // final java.util.List<ShutdownVetoException> vetos = new
        // ArrayList<ShutdownVetoException>();
        ShutdownVetoListener[] localList = null;
        synchronized (vetoListeners) {
            localList = vetoListeners.toArray(new ShutdownVetoListener[] {});
        }
        for (final ShutdownVetoListener v : localList) {
            try {
                if (request != null && request.askForVeto(v) == false) {
                    continue;
                }

                v.onShutdownVetoRequest(request);

            } catch (final ShutdownVetoException e) {

                if (request != null) {
                    try {
                        request.addVeto(e);
                    } catch (final Throwable e2) {
                        e2.printStackTrace();
                    }
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        return request;

    }

    public int getExitCode() {
        return exitCode;
    }

    public ShutdownRequest getShutdownRequest() {
        return shutdownRequest;
    }

    public List<ShutdownVetoListener> getShutdownVetoListeners() {
        synchronized (vetoListeners) {
            return new ArrayList<ShutdownVetoListener>(vetoListeners);
        }
    }

    /**
     * Same function as org.appwork.utils.Exceptions.getStackTrace(Throwable)<br>
     * <b>DO NOT REPLACE IT EITHER!</b> Exceptions.class my be unloaded. This
     * would cause Initialize Exceptions during shutdown.
     * 
     * @param thread
     * @return
     */
    private String getStackTrace(final Thread thread) {
        try {
            final StackTraceElement[] st = thread.getStackTrace();
            final StringBuilder sb = new StringBuilder("");
            for (final StackTraceElement element : st) {
                sb.append(element);
                sb.append("\r\n");
            }
            return sb.toString();
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param instance2
     * @return
     */
    public boolean hasShutdownEvent(final ShutdownEvent instance2) {
        synchronized (hooks) {
            return hooks.contains(instance2);
        }
    }

    public boolean isShutDownRequested() {
        return requestedShutDowns.get() > 0;
    }

    public void removeShutdownEvent(final ShutdownEvent event) {
        if (isAlive()) { throw new IllegalStateException("Cannot add hooks during shutdown"); }
        synchronized (hooks) {
            ShutdownEvent next;

            // add event sorted
            for (final Iterator<ShutdownEvent> it = hooks.iterator(); it.hasNext();) {
                next = it.next();
                if (next == event) {
                    it.remove();
                }

            }
        }
    }

    public void removeShutdownVetoListener(final ShutdownVetoListener listener) {
        synchronized (vetoListeners) {
            Log.L.finest("Remove " + listener);
            vetoListeners.remove(listener);
        }
    }

    public boolean requestShutdown() {
        return this.requestShutdown(false);
    }

    // /**
    // *
    // */
    public boolean requestShutdown(final boolean silent) {
        return this.requestShutdown(new BasicShutdownRequest(silent));
    }

    public boolean requestShutdown(final ShutdownRequest request) {
        if (request == null) { throw new NullPointerException(); }
        log("Request Shutdown: " + request);
        requestedShutDowns.incrementAndGet();
        try {

            collectVetos(request);
            final java.util.List<ShutdownVetoException> vetos = request.getVetos();

            if (vetos.size() == 0) {
                Log.L.info("No Vetos");
                ShutdownVetoListener[] localList = null;
                synchronized (vetoListeners) {
                    localList = vetoListeners.toArray(new ShutdownVetoListener[] {});
                }

                Log.L.info("Fire onShutDownEvents");
                for (final ShutdownVetoListener v : localList) {
                    try {
                        Log.L.info("Call onShutdown: " + v);
                        v.onShutdown(request);
                    } catch (final Throwable e) {
                        Log.exception(e);

                    } finally {
                        Log.L.info("Call onShutdown done: " + v);
                    }
                }
                if (shutDown.getAndSet(true) == false) {
                    Log.L.info("Create ExitThread");
                    exitThread = new Thread("ShutdownThread") {

                        @Override
                        public void run() {
                            shutdownRequest = request;
                            Log.L.info("Exit Now: Code: " + ShutdownController.this.getExitCode());
                            System.exit(ShutdownController.this.getExitCode());
                        }
                    };
                    exitThread.start();
                }
                Log.L.info("Wait");
                while (exitThread.isAlive()) {
                    try {
                        Thread.sleep(500);
                    } catch (final InterruptedException e) {
                        return true;
                    }
                }
                Log.L.finest("DONE");
                return true;
            } else {
                Log.L.info("Vetos found");
                ShutdownVetoListener[] localList = null;
                synchronized (vetoListeners) {
                    localList = vetoListeners.toArray(new ShutdownVetoListener[] {});
                }
                for (final ShutdownVetoListener v : localList) {
                    try {
                        /* make sure noone changes content of vetos */
                        v.onShutdownVeto(request);
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
                return false;
            }
        } finally {
            requestedShutDowns.decrementAndGet();
        }
    }

    private LogSource logger;

    public LogSource getLogger() {
        return logger;
    }

    public void setLogger(final LogSource logger) {
        this.logger = logger;
    }

    /**
     * @param string
     */
    private void log(final String string) {
        try {
            // code may run in the shutdown hook. Classloading problems are
            // possible
            System.out.println(string);
            if (logger != null) {
                logger.info(string);
            }
        } catch (final Throwable e) {

        }
    }

    @Override
    public void run() {
        /*
         * Attention. This runs in shutdownhook. make sure, that we do not have
         * to load previous unloaded classes here.
         */

        try {

            java.util.List<ShutdownEvent> list;
            synchronized (hooks) {
                list = new ArrayList<ShutdownEvent>(hooks);

            }
            synchronized (originalShutdownHooks) {
                list.addAll(originalShutdownHooks);
            }
            int i = 0;
            for (final ShutdownEvent e : list) {
                try {
                    i++;

                    final long started = System.currentTimeMillis();

                    Log.L.finest("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: start item->" + e);
                    System.out.println("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: start item->" + e);

                    final Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            e.onShutdown(shutdownRequest);
                        }
                    });
                    thread.setName("ShutdownHook [" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]");

                    thread.start();
                    try {
                        thread.join(Math.max(0, e.getMaxDuration()));
                    } catch (final Throwable e1) {
                        e1.printStackTrace();

                    }
                    if (thread.isAlive()) {
                        Log.L.finest("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: " + e + "->is still running after " + e.getMaxDuration() + " ms");
                        Log.L.finest("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: " + e + "->StackTrace:\r\n" + this.getStackTrace(thread));
                    } else {
                        Log.L.finest("[" + i + "/" + list.size() + "|Priority: " + e.getHookPriority() + "]" + "ShutdownController: item ended after->" + (System.currentTimeMillis() - started));
                    }
                    System.out.println("[Done:" + i + "/" + list.size());
                } catch (final Throwable e1) {
                    e1.printStackTrace();
                }
            }
            Log.L.info("Shutdown Hooks Finished");

        } catch (final Throwable e1) {
            // do not use Log here. If Log.exception(e1); throws an exception,
            // we have to catch it here without the risk of another exception.

        }
    }

    /**
     * @param i
     */
    public void setExitCode(final int i) {
        exitCode = i;

    }
}
