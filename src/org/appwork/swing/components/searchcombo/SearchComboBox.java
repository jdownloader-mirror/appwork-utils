package org.appwork.swing.components.searchcombo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;

import org.appwork.app.gui.BasicGui;
import org.appwork.app.gui.MigPanel;
import org.appwork.resources.AWUTheme;
import org.appwork.swing.components.searchcombo.SearchComboBox.Editor;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;

/**
 * this component extends a normal combobox and implements a editable
 * filter/autocompletion feature. <b> make sure that you model is sorted</b>
 * 
 * @author thomas
 * 
 * @param <T>
 */
public abstract class SearchComboBox<T> extends JComboBox {

    class Editor implements ComboBoxEditor, FocusListener, DocumentListener {
        private final JTextField tf;
        private final MigPanel   panel;

        private final JLabel     icon;
        private T                value;

        private boolean          setting;

        public Editor() {
            this.tf = new JTextField() {

                @Override
                public void setText(final String t) {
                    super.setText(t);
                }
                //
                // super.setText(t);
                // if (SearchComboBox.this.helptext != null) {
                // if (SearchComboBox.this.helptext.equals(t)) {
                // SearchComboBox.this.setColorState(SearchComboBox.this.helpColorSet);
                //
                // } else {
                // // if (!Editor.this.autoComplete(false)) {
                // // this.setForeground(SearchComboBox.this.foregroundBad);
                // // } else {
                // // this.setForeground(Editor.this.defaultForeground);
                // // }
                //
                // this.setForeground(SearchComboBox.this.getForeground());
                // }
                // }
                // }

            };
            this.tf.getDocument().addDocumentListener(this);
tf.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(final FocusEvent e) {
//                SearchComboBox.this.getEditor().getEditorComponent().requestFocus();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                // TODO Auto-generated method stub
              hidePopup();

            }
        });
            this.icon = new JLabel();
            // editor panel
            this.panel = new MigPanel("ins 0", "[][grow,fill]", "[grow,fill]") {

                @Override
                public void requestFocus() {
                    Editor.this.tf.requestFocus();
                }

            };

            this.tf.addFocusListener(this);
            this.panel.add(this.icon);
            this.panel.setOpaque(true);
            this.panel.setBackground(this.tf.getBackground());

            this.tf.setBackground(null);
            SwingUtils.setOpaque(this.tf, false);

            this.panel.add(this.tf);
            SwingUtils.setOpaque(this.panel, false);

            // this.panel.setBorder(this.tf.getBorder());
            this.tf.setBorder(null);

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.ComboBoxEditor#addActionListener(java.awt.event.
         * ActionListener)
         */
        @Override
        public void addActionListener(final ActionListener l) {
            this.tf.addActionListener(l);
        }

        private void auto() {
            if (this.setting) { return; }
            System.out.println("auto");
            // scheduler executes at least 50 ms after this submit.
            // this.sheduler.run();
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    Editor.this.autoComplete(true);
                }
            });

        }

        /**
         * finds all possible matches of the entered text and sets the selected
         * object
         */
        protected boolean autoComplete(final boolean showPopup) {

            final String txt = Editor.this.tf.getText();
            if (this.value != null && SearchComboBox.this.getTextForValue(this.value).equals(txt)) { return true; }
            String text = null;
            final ArrayList<T> found = new ArrayList<T>();

            for (int i = 0; i < SearchComboBox.this.getModel().getSize(); i++) {
                text = SearchComboBox.this.getTextForValue((T) SearchComboBox.this.getModel().getElementAt(i));
                if (text != null && text.startsWith(txt)) {
                    found.add((T) SearchComboBox.this.getModel().getElementAt(i));

                }
            }

            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    final int pos = Editor.this.tf.getCaretPosition();

                    if (found.size() == 0) {

                        SearchComboBox.this.hidePopup();

                        if (SearchComboBox.this.getSelectedIndex() != -1) {
                            SearchComboBox.this.setSelectedIndex(-1);
                            Editor.this.tf.setText(txt);
                        }

                        // javax.swing.plaf.synth.SynthComboPopup
                    } else {
                        Editor.this.tf.setForeground(SearchComboBox.this.getForeground());

                        // Editor.this.setItem(found.get(0));
                        SearchComboBox.this.setSelectedItem(found.get(0));
                        Editor.this.setItem(found.get(0));
                        Editor.this.tf.setCaretPosition(pos);
                        Editor.this.tf.select(txt.length(), Editor.this.tf.getText().length());
                        // Show popup, and scroll to correct position

                        if (found.size() > 1 && showPopup) {
                            // limit popup rows
                            SearchComboBox.this.setMaximumRowCount(found.size());
                            SearchComboBox.this.setPopupVisible(true);

                            // Scroll popup list, so that found[0] is the first
                            // entry. This is a bit "dirty", so we put it in a
                            // try catch...just to avoid EDT Exceptions
                            try {
                                final Object popup = SearchComboBox.this.getUI().getAccessibleChild(SearchComboBox.this, 0);
                                if (popup instanceof Container) {
                                    final Component scrollPane = ((Container) popup).getComponent(0);
                                    if (popup instanceof ComboPopup) {
                                        final JList jlist = ((ComboPopup) popup).getList();
                                        if (scrollPane instanceof JScrollPane) {
                                            final Rectangle cellBounds = jlist.getCellBounds(SearchComboBox.this.getSelectedIndex(), SearchComboBox.this.getSelectedIndex() + found.size() - 1);
                                            if (cellBounds != null) {
                                                jlist.scrollRectToVisible(cellBounds);
                                            }

                                        }
                                    }
                                }
                            } catch (final Throwable e) {
                                Log.exception(e);
                            }
                        } else {
                            SearchComboBox.this.hidePopup();
                        }

                    }
                    SearchComboBox.this.updateColorByContent();

                }
            };
            return found.size() > 0;
        }

        public void caretUpdate(final CaretEvent arg0) {

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#changedUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public void changedUpdate(final DocumentEvent e) {
            this.auto();
            SearchComboBox.this.onChanged();
        }

        public void focusGained(final FocusEvent arg0) {

            if (this.tf.getText().equals(SearchComboBox.this.helptext)) {
                this.tf.setText("");
                SearchComboBox.this.updateColorByContent();
            } else {
                Editor.this.tf.selectAll();
            }

        }

        public void focusLost(final FocusEvent arg0) {

            if (!SearchComboBox.this.isUnkownTextInputAllowed() && !Editor.this.autoComplete(false)) {
                // reset text after modifications to a valid value

                Editor.this.tf.setText(SearchComboBox.this.getTextForValue(Editor.this.value));

                // Editor.this.autoComplete(false);
            } else {
                SearchComboBox.this.updateHelpText();
            }

            SearchComboBox.this.updateColorByContent();

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.ComboBoxEditor#getEditorComponent()
         */
        @Override
        public Component getEditorComponent() {
            // TODO Auto-generated method stub

            return this.panel;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.ComboBoxEditor#getItem()
         */
        @Override
        public Object getItem() {
            // TODO Auto-generated method stub
            return this.value;
        }

        public JTextField getTf() {
            return this.tf;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#insertUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public void insertUpdate(final DocumentEvent e) {
            this.auto();
            SearchComboBox.this.onChanged();

        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.ComboBoxEditor#removeActionListener(java.awt.event.
         * ActionListener)
         */
        @Override
        public void removeActionListener(final ActionListener l) {
            this.tf.removeActionListener(l);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * javax.swing.event.DocumentListener#removeUpdate(javax.swing.event
         * .DocumentEvent)
         */
        @Override
        public void removeUpdate(final DocumentEvent e) {
            if (this.tf.getSelectionEnd() - this.tf.getSelectionStart() == 0) {
                this.auto();
            }
            SearchComboBox.this.onChanged();
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.ComboBoxEditor#selectAll()
         */
        @Override
        public void selectAll() {
            this.tf.selectAll();
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.ComboBoxEditor#setItem(java.lang.Object)
         */
        @SuppressWarnings("unchecked")
        @Override
        public void setItem(final Object anObject) {
            // if (this.value == anObject) { return; }
            this.setting = true;
            this.tf.setText(SearchComboBox.this.getTextForValue((T) anObject));
            this.icon.setIcon(SearchComboBox.this.getIconForValue((T) anObject));
            this.value = (T) anObject;
            SearchComboBox.this.updateHelpText();
            SearchComboBox.this.updateColorByContent();
            this.setting = false;

        }

    }

    public static void main(final String[] args) {
        final BasicGui gui = new BasicGui(SearchComboBox.class.getSimpleName()) {

            @Override
            protected void layoutPanel() {
                try {
                    final SearchComboBox<String> box1 = new SearchComboBox<String>() {

                        @Override
                        protected Icon getIconForValue(final String value) {
                            return value == null ? AWUTheme.getInstance().getIcon("close", 28) : AWUTheme.getInstance().getIcon(value, 28);
                        }

                        @Override
                        protected String getTextForValue(final String value) {

                            return value + "-icon";
                        }
                    };
                    final String p = AWUTheme.I().getPath();
                    final ArrayList<String> list = new ArrayList<String>();

                    final URL images = AWUTheme.class.getResource(p + "images");
                    for (final String s : new File(images.toURI()).list(new FilenameFilter() {

                        @Override
                        public boolean accept(final File dir, final String name) {

                            return name.endsWith(".png");
                        }
                    })) {
                        list.add(s.replace(".png", ""));
                    }

                    final MigPanel contentPane = new MigPanel("ins 10,wrap 1", "[grow,fill]", "[]");
                    this.getFrame().setContentPane(contentPane);

                    box1.setList(list);
                    box1.setBorder(BorderFactory.createEtchedBorder());

                    this.getFrame().getContentPane().add(box1);
                    final JToggleButton toggle = new JToggleButton("Toggle Allow unknown");
                    this.getFrame().getContentPane().add(toggle);
                    toggle.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            box1.setUnkownTextInputAllowed(toggle.isSelected());
                        }
                    });

                    final JToggleButton toggle2 = new JToggleButton("Toggle HelpText");
                    this.getFrame().getContentPane().add(toggle2);
                    toggle2.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            box1.setHelpText(toggle2.isSelected() ? "I'm Help Text" : null);
                        }
                    });

                } catch (final URISyntaxException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            @Override
            protected void requestExit() {
                System.exit(1);
            }
        };

    }

    private final ColorState helpColorSet           = new ColorState(Color.LIGHT_GRAY);

    private final ColorState badColorSet            = new ColorState(Color.RED);

    private final ColorState normalColorSet         = new ColorState(Color.BLACK);

    {

        this.normalColorSet.setForeground(this.getForeground());
        final Color disabled = (Color) UIManager.get("TextField.disabledForeground");
        if (disabled != null) {
            this.helpColorSet.setForeground(disabled);
        }

    }

    private String           helptext;

    private boolean          unkownTextInputAllowed = false;

    protected ImageIcon      badgeIcon;

    private ColorState       currentColorSet;

    /**
     * @param plugins
     */
    public SearchComboBox() {
        this(null);
    }

    public SearchComboBox(final List<T> plugins) {
        super((ComboBoxModel) null);
        this.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(final FocusEvent e) {
                SearchComboBox.this.getEditor().getEditorComponent().requestFocus();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                // TODO Auto-generated method stub
              hidePopup();

            }
        });
        if (plugins != null) {
            this.setList(plugins);
        }

   
        this.setEditor(new Editor());
        
        this.setEditable(true);

        // we extends the existing renderer. this avoids LAF incompatibilities
        final ListCellRenderer org = this.getRenderer();
        this.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                SearchComboBox.this.setMaximumRowCount(8);

            }

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {

            }
        });
        this.setRenderer(new ListCellRenderer() {

            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                try {
                    final JLabel ret = (JLabel) org.getListCellRendererComponent(list, SearchComboBox.this.getTextForValue((T) value), index, isSelected, cellHasFocus);

                    ret.setIcon(SearchComboBox.this.getIconForValue((T) value));

                    // ret.setOpaque(false);
                    return ret;
                } catch (final Throwable e) {
                    // org might not be a JLabel (depending on the LAF)
                    // fallback here

                    return org.getListCellRendererComponent(list, SearchComboBox.this.getTextForValue((T) value), index, isSelected, cellHasFocus);

                }
            }
        });
        this.setColorState(this.normalColorSet);
    }

    /**
     * @return
     */
    public String getEditorText() {

        return this.getTextField().getText();
    }

    /**
     * @param value
     * @return
     */
    abstract protected Icon getIconForValue(T value);

    public String getText() {

        String ret = this.getTextField().getText();
        if (ret.equals(this.helptext)) {
            ret = "";
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public JTextField getTextField() {
        if ((Editor) this.getEditor() == null) { return null; }
        return ((Editor) this.getEditor()).getTf();
    }

    /**
     * @param value
     * @return
     */
    abstract protected String getTextForValue(T value);

    public boolean isHelpTextVisible() {
        return this.helptext != null && this.helptext.equals(this.getText());
    }

    /**
     * if unknown values are allowed, the component will not try to find a valid
     * entry on fopcus lost
     * 
     * @return
     */
    public boolean isUnkownTextInputAllowed() {

        return this.unkownTextInputAllowed;
    }

    /**
     * 
     */
    protected void onChanged() {
        // TODO Auto-generated method stub

    }

    /**
     * @param object
     */
    public void setBadColor(final Color color) {
        this.badColorSet.setForeground(color);
        final JTextField tf = this.getTextField();
        if (tf != null) {
            tf.setForeground(this.currentColorSet.getForeground());
        }

    }

    private void setColorState(final ColorState cs) {
        this.currentColorSet = cs;

        final JTextField tf = this.getTextField();
        if (tf != null) {
            tf.setForeground(this.currentColorSet.getForeground());
        }

    }

    @Override
    public void setForeground(final Color fg) {
        super.setForeground(fg);
        this.setNormalColor(fg);
    }

    /**
     * @param addLinksDialog_layoutDialogContent_packagename_help
     */
    public void setHelpText(final String helptext) {
        this.helptext = helptext;
        this.updateHelpText();
    }

    /**
     * Sets the Model for this combobox
     * 
     * @param listModel
     */
    public void setList(final List<T> listModel) {
        if (this.usePrototype() && listModel.size() > 0) {
            this.setPrototypeDisplayValue(listModel.get(0));
        }
        super.setModel(new DefaultComboBoxModel(listModel.toArray(new Object[] {})));
        try {
            final BasicComboBoxUI udi = (BasicComboBoxUI) this.getUI();
            JComponent arrowButton = null;
            try {
                final Field field = BasicComboBoxUI.class.getDeclaredField("arrowButton");

                final BasicComboBoxUI bla = null;

                if (field != null) {
                    field.setAccessible(true);
                    arrowButton = (JComponent) field.get(udi);

                }
            } catch (final Throwable e) {

            }
            if (listModel.size() > 0) {
                udi.unconfigureArrowButton();
                udi.configureArrowButton();
                if (arrowButton != null) {
                    arrowButton.setEnabled(true);

                }

            } else {
                udi.unconfigureArrowButton();
                if (arrowButton != null) {
                    arrowButton.setEnabled(false);

                }

            }

        } catch (final Throwable e) {
            e.printStackTrace();
            // for lafs not extending BasicComboBoxUI it is possible to open a
            // empty popup
        }

    }

    /**
     * Do not use this method. For Type Safty, please use
     * {@link #setList(ArrayList)} instead
     * 
     * @deprecated use {@link #setList(ArrayList)}
     */
    @Override
    @Deprecated
    public void setModel(final ComboBoxModel aModel) {
        if (aModel == null) {
            super.setModel(new DefaultComboBoxModel());
            return;
        }
        throw new RuntimeException("Use setList()");
    }

    /**
     * @param fg
     */
    public void setNormalColor(final Color fg) {
        final JTextField tf = this.getTextField();
        if (tf != null) {
            tf.setForeground(this.currentColorSet.getForeground());
        }

    }

    @Override
    public void setRenderer(final ListCellRenderer aRenderer) {

        super.setRenderer(aRenderer);
    }

    /**
     * @param defaultDownloadFolder
     */
    public void setText(final String text) {
        this.getTextField().setText(text);

    }

    @Override
    public void setToolTipText(final String text) {
        super.setToolTipText(text);
        final JTextField tf = this.getTextField();
        if (tf != null) {
            tf.setToolTipText(text);
        }
    }

    /**
     * if unknown values are allowed, the component will not try to find a valid
     * entry on fopcus lost
     * 
     * @param allowUnknownValuesEnabled
     */
    public void setUnkownTextInputAllowed(final boolean allowUnknownValuesEnabled) {
        this.unkownTextInputAllowed = allowUnknownValuesEnabled;
    }

    /**
     * @param txt
     * @return
     */
    private boolean textMatchesEntry(final String txt) {
        if (txt == null) { return false; }

        String text;

        for (int i = 0; i < SearchComboBox.this.getModel().getSize(); i++) {
            text = SearchComboBox.this.getTextForValue((T) SearchComboBox.this.getModel().getElementAt(i));
            if (text != null && text.startsWith(txt)) { return true; }
        }
        return false;
    }

    /**
     * 
     */
    public void updateColorByContent() {
        final String txt = this.getTextField().getText();
        if (this.helptext != null && this.helptext.equals(txt)) {
            this.setColorState(this.helpColorSet);
        } else {
            if (this.textMatchesEntry(txt)) {
                this.setColorState(this.normalColorSet);

            } else {
                this.setColorState(this.badColorSet);
            }

        }
    }

    /**
     * 
     */
    private void updateHelpText() {
        if (this.getEditor() == null || this.helptext == null) { return; }
        if (this.getTextField().getDocument().getLength() == 0) {
            this.setText(this.helptext);
            this.updateColorByContent();
        }
    }

    public boolean usePrototype() {
        return true;
    }

}
