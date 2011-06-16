package org.appwork.utils.swing.table.columns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.border.CompoundBorder;
import javax.swing.event.TableModelEvent;

import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtDefaultRowSorter;
import org.appwork.utils.swing.table.ExtTableModel;

abstract public class ExtProgressColumn<E> extends ExtColumn<E> {
    private static final long serialVersionUID = -2473320164484034664L;
    protected JProgressBar    determinatedRenderer;
    private CompoundBorder    defaultBorder;
    private JProgressBar      indeterminatedRenderer;
    private JProgressBar      renderer;
    private HashMap<E, Long>  map;
    private int               columnIndex      = -1;

    /**
     * 
     */
    public ExtProgressColumn(final String title) {
        this(title, null);
    }

    public ExtProgressColumn(final String name, final ExtTableModel<E> extModel) {
        super(name, extModel);
        this.determinatedRenderer = new JProgressBar();

        this.indeterminatedRenderer = new JProgressBar() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;
            private long              timer            = 0;
            private long              cleanupTimer     = 0;

            @Override
            public boolean isDisplayable() {
                return true;
            }

            @Override
            public void repaint() {
                final ExtTableModel<E> mod = ExtProgressColumn.this.getModel();
                if (mod != null && mod.getTable() != null && mod.getTable().isShowing()) {

                    // cleanup map in case we removed a indeterminated value
                    if (System.currentTimeMillis() - this.cleanupTimer > 30000) {
                        Entry<E, Long> next;
                        for (final Iterator<Entry<E, Long>> it = ExtProgressColumn.this.map.entrySet().iterator(); it.hasNext();) {
                            next = it.next();
                            final long lastUpdate = System.currentTimeMillis() - next.getValue();
                            if (lastUpdate > 5000) {
                                it.remove();
                            }
                        }

                        this.cleanupTimer = System.currentTimeMillis();
                        if (ExtProgressColumn.this.map.size() == 0) {
                            ExtProgressColumn.this.indeterminatedRenderer.setIndeterminate(false);
                            return;
                        }

                    }
                    if (System.currentTimeMillis() - this.timer > 1000 / ExtProgressColumn.this.getFps() && ExtProgressColumn.this.columnIndex >= 0) {
                        final ArrayList<E> selection = mod.getSelectedObjects();

                        mod.fireTableChanged(new TableModelEvent(mod, 0, Integer.MAX_VALUE, ExtProgressColumn.this.columnIndex, TableModelEvent.UPDATE));
                        mod.setSelectedObjects(selection);
                        this.timer = System.currentTimeMillis();
                    }

                }

            }

        };
        this.map = new HashMap<E, Long>();

        // this.getModel().addTableModelListener(new TableModelListener() {
        //
        // @Override
        // public void tableChanged(final TableModelEvent e) {
        // switch (e.getType()) {
        // case TableModelEvent.DELETE:
        // case TableModelEvent.UPDATE:
        // System.out.println(e);
        // if (ExtProgressColumn.this.map.size() == 0) {
        // ExtProgressColumn.this.indeterminatedRenderer.setIndeterminate(false);
        // }
        // }
        //
        // }
        // });
        this.renderer = this.determinatedRenderer;
        this.defaultBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1), this.determinatedRenderer.getBorder());
        this.setRowSorter(new ExtDefaultRowSorter<E>() {

            @Override
            public int compare(final E o1, final E o2) {
                final long v1 = ExtProgressColumn.this.getValue(o1);
                final long v2 = ExtProgressColumn.this.getValue(o2);
                if (v1 == v2) { return 0; }
                if (this.isSortOrderToggle()) {
                    return v1 > v2 ? -1 : 1;
                } else {
                    return v2 < v1 ? -1 : 1;
                }
            }

        });
    }

    @Override
    public void configureEditorComponent(final E value, final boolean isSelected, final int row, final int column) {
        // TODO Auto-generated method stub

    }

    @Override
    public void configureRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        if (this.renderer == this.determinatedRenderer) {

            // Normalize value and maxvalue to fit in the integer range
            long v = this.getValue(value);
            long m = this.getMax(value);
            final double factor = Math.max(v / (double) Integer.MAX_VALUE, m / (double) Integer.MAX_VALUE);

            if (factor >= 1.0) {
                v /= factor;
                m /= factor;
            }
            // take care to set the maximum before the value!!
            this.renderer.setMaximum((int) m);
            this.renderer.setValue((int) v);

            this.renderer.setString(this.getString(value));

        } else {
            this.renderer.setString(this.getString(value));
            if (!this.renderer.isIndeterminate()) {
                this.renderer.setIndeterminate(true);
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.rapidshare.rsmanager.gui.components.table.ExtColumn#getCellEditorValue
     * ()
     */
    @Override
    public Object getCellEditorValue() {

        return null;
    }

    /**
     * @return
     */
    @Override
    public JComponent getEditorComponent(final E value, final boolean isSelected, final int row, final int column) {
        return null;
    }

    /**
     * @return
     */
    protected int getFps() {
        return 20;
    }

    protected long getMax(final E value) {
        return 100;
    }

    /**
     * @return
     */
    @Override
    public JComponent getRendererComponent(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        this.columnIndex = column;
        if (this.isIndeterminated(value, isSelected, hasFocus, row, column)) {
            this.renderer = this.indeterminatedRenderer;
            if (this.map.size() == 0) {
                this.indeterminatedRenderer.setIndeterminate(true);
            }
            this.map.put(value, System.currentTimeMillis());

        } else {
            this.renderer = this.determinatedRenderer;
            this.map.remove(value);
            if (this.map.size() == 0) {
                this.indeterminatedRenderer.setIndeterminate(false);
            }
        }
        return this.renderer;
    }

    abstract protected String getString(E value);

    @Override
    protected String getToolTip(final E value) {

        return this.getString(value);
    }

    abstract protected long getValue(E value);

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.rapidshare.rsmanager.gui.components.table.ExtColumn#isEditable(java
     * .lang.Object)
     */
    @Override
    public boolean isEditable(final E obj) {

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.rapidshare.rsmanager.gui.components.table.ExtColumn#isEnabled(java
     * .lang.Object)
     */
    @Override
    public boolean isEnabled(final E obj) {

        return true;
    }

    /**
     * @param column
     * @param row
     * @param hasFocus
     * @param isSelected
     * @param value
     * @return
     */
    protected boolean isIndeterminated(final E value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.rapidshare.rsmanager.gui.components.table.ExtColumn#isSortable(java
     * .lang.Object)
     */
    @Override
    public boolean isSortable(final E obj) {

        return true;
    }

    @Override
    public void resetEditor() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetRenderer() {
        this.renderer.setOpaque(false);
        this.renderer.setStringPainted(true);
        this.renderer.setBorder(this.defaultBorder);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.rapidshare.rsmanager.gui.components.table.ExtColumn#setValue(java
     * .lang.Object, java.lang.Object)
     */
    @Override
    public void setValue(final Object value, final E object) {

    }

}
