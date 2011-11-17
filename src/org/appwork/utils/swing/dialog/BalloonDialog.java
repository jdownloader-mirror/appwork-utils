/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.swing.dialog
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.swing.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.appwork.resources.AWUTheme;
import org.appwork.screenshot.ScreensShotHelper;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;

public class BalloonDialog extends AbstractDialog<Integer> {

    private static final long serialVersionUID = -7647771640756844691L;
    private JComponent        component;
    private Point             desiredLocation;

    private BallonPanel       ballonPanel;

    private ScreenShotPanel   screenshotPanel;
    private boolean           opaqueWorkaround = false;

    public BalloonDialog(final int flag, final JComponent comp, final Point point) throws OffScreenException {
        super(flag | Dialog.BUTTONS_HIDE_CANCEL | Dialog.BUTTONS_HIDE_OK | Dialog.STYLE_HIDE_ICON, "Balloon", null, null, null);
        this.component = comp;
        this.desiredLocation = point == null ? MouseInfo.getPointerInfo().getLocation() : point;

    }

    /**
     * this function will init and show the dialog
     * 
     * @throws OffScreenException
     */
    @Override
    protected void _init() {

        this.layoutDialog();

        // p.add(this.component);

        this.getDialog().setUndecorated(true);

        if (BinaryLogic.containsAll(this.flagMask, Dialog.LOGIC_COUNTDOWN)) {
            this.timerLbl.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(final MouseEvent e) {
                    BalloonDialog.this.cancel();
                    BalloonDialog.this.timerLbl.removeMouseListener(this);
                }

            });
            this.timerLbl.setToolTipText(_AWU.T.TIMERDIALOG_TOOLTIP_TIMERLABEL());

            this.timerLbl.setIcon(AWUTheme.I().getIcon("dialog/cancel", 16));

        }
        /**
         * this is very important so the new shown dialog will become root for
         * all following dialogs! we save old parentWindow, then set current
         * dialogwindow as new root and show dialog. after dialog has been
         * shown, we restore old parentWindow
         */
        final Component parentOwner = Dialog.getInstance().getParentOwner();
        Dialog.getInstance().setParentOwner(this.getDialog());
        try {

            dont: if (BinaryLogic.containsAll(this.flagMask, Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN)) {

                try {
                    final int i = BinaryLogic.containsAll(this.flagMask, Dialog.LOGIC_DONT_SHOW_AGAIN_DELETE_ON_EXIT) ? AbstractDialog.getSessionDontShowAgainValue(this.getDontShowAgainKey()) : JSonStorage.getStorage("Dialogs").get(this.getDontShowAgainKey(), -1);

                    if (i >= 0) {
                        // filter saved return value
                        int ret = i & (Dialog.RETURN_OK | Dialog.RETURN_CANCEL);
                        // add flags
                        ret |= Dialog.RETURN_DONT_SHOW_AGAIN | Dialog.RETURN_SKIPPED_BY_DONT_SHOW;

                        /*
                         * if LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL or
                         * LOGIC_DONT_SHOW_AGAIN_IGNORES_OK are used, we check
                         * here if we should handle the dont show again feature
                         */
                        if (BinaryLogic.containsAll(this.flagMask, Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL) && BinaryLogic.containsAll(ret, Dialog.RETURN_CANCEL)) {
                            break dont;
                        }
                        if (BinaryLogic.containsAll(this.flagMask, Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_OK) && BinaryLogic.containsAll(ret, Dialog.RETURN_OK)) {
                            break dont;
                        }

                        this.returnBitMask = ret;
                        return;
                    }
                } catch (final Exception e) {
                    Log.exception(e);
                }
            }
            if (parentOwner == null || !parentOwner.isShowing()) {
                this.getDialog().setAlwaysOnTop(true);
            }
            // The Dialog Modal
            this.getDialog().setModal(true);

            // Dispose dialog on close
            this.getDialog().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            this.getDialog().addWindowListener(this);

            // add the countdown timer
            // this.getDialog().add(this.timerLbl, "split 3,growx,hidemode 2");
            if (BinaryLogic.containsAll(this.flagMask, Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN)) {
                this.dontshowagain = new JCheckBox(_AWU.T.ABSTRACTDIALOG_STYLE_SHOW_DO_NOT_DISPLAY_AGAIN());
                this.dontshowagain.setHorizontalAlignment(SwingConstants.TRAILING);
                this.dontshowagain.setHorizontalTextPosition(SwingConstants.LEADING);
                this.dontshowagain.setSelected(this.doNotShowAgainSelected);
                // this.getDialog().add(this.dontshowagain,
                // "growx,pushx,alignx right,gapleft 20");
            } else {
                // this.getDialog().add(Box.createHorizontalGlue(),
                // "growx,pushx,alignx right,gapleft 20");
            }

            try {
                this.ballonPanel = new BallonPanel(this, this.component, this.timerLbl, this.dontshowagain, getDesiredLocation());

            } catch (final OffScreenException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                throw new RuntimeException(e1);
            }
            // this.getDialog().setLayout(new MigLayout("ins 0,", "0[]0",
            // "0[]0"));
            this.getDialog().getContentPane().add(this.ballonPanel);
            // this.getDialog().setContentPane(this.ballonPanel);
            // if()
            //
            Class<?> clazz;
            try {
                if (CrossSystem.isWindows()) {
                    clazz = Class.forName("com.sun.awt.AWTUtilities");
                    // com.sun.awt.AWTUtilities.setWindowOpaque(window, flag)
                    final Method method = clazz.getMethod("setWindowOpaque", new Class[] { Window.class, boolean.class });
                    method.invoke(null, new Object[] { BalloonDialog.this.getDialog(), false });
                } else {
                    this.opaqueWorkaround = true;
                }
            } catch (final Throwable e1) {
                e1.printStackTrace();
                this.opaqueWorkaround = true;
            }

            if (BinaryLogic.containsAll(this.flagMask, Dialog.LOGIC_COUNTDOWN)) {
                // show timer
                this.initTimer(this.getCountdown());
            } else {
                this.timerLbl.setVisible(false);
            }

            // pack dialog
            this.getDialog().invalidate();

            this.pack();
            // minimum size foir a dialog

            // // Dimension screenDim =
            // Toolkit.getDefaultToolkit().getScreenSize();

            // this.setMaximumSize(Toolkit.getDefaultToolkit().getScreenSize());
            this.getDialog().toFront();

            // if (this.getDesiredSize() != null) {
            // this.setSize(this.getDesiredSize());
            // }

            // TODO: Find the error, and make this ugly workaround useless
            // this workaround ajusts locations to the dart
            if (this.ballonPanel.isExpandToBottom()) {

                this.getDialog().setLocation(new Point(this.getDesiredLocation().x - 11, this.getDesiredLocation().y - 13));
            } else {

                this.getDialog().setLocation(new Point(this.getDesiredLocation().x - 11, this.getDesiredLocation().y + 27));
            }

            // register an escape listener to cancel the dialog
            final KeyStroke ks = KeyStroke.getKeyStroke("ESCAPE");
            this.ballonPanel.getInputMap().put(ks, "ESCAPE");
            this.ballonPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, "ESCAPE");
            this.ballonPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, "ESCAPE");
            this.ballonPanel.getActionMap().put("ESCAPE", new AbstractAction() {

                private static final long serialVersionUID = -6666144330707394562L;

                public void actionPerformed(final ActionEvent e) {
                    Log.L.fine("Answer: Key<ESCAPE>");
                    BalloonDialog.this.setReturnmask(false);
                    BalloonDialog.this.dispose();
                }
            });
            this.ballonPanel.requestFocus();
            this.packed();

            // System.out.println("NEW ONE "+this.getDialog());
            /*
             * workaround a javabug that forces the parentframe to stay always
             * on top
             */
            if (this.getDialog().getParent() != null && !CrossSystem.isMac()) {
                ((Window) this.getDialog().getParent()).setAlwaysOnTop(true);
                ((Window) this.getDialog().getParent()).setAlwaysOnTop(false);
            }
            // new Thread() {
            // @Override
            // public void run() {
            // while (true) {
            // try {
            // Thread.sleep(1000);
            // } catch (final InterruptedException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
            // if (!BalloonDialog.this.isVisible()) {
            //
            // System.exit(0);
            // return;
            // }
            // final Point mouse = MouseInfo.getPointerInfo().getLocation();
            // // mouse.x /= 2;
            // // mouse.y /= 2;
            // new EDTRunner() {
            //
            // @Override
            // protected void runInEDT() {
            //
            // BalloonDialog.this.getDialog().setLocation(mouse);
            //
            // }
            // };
            //
            // }
            // }
            // }.start();
            // this.getDialog().setModal(false);

            this.setVisible(true);
        } finally {
            // System.out.println("SET OLD");
            Dialog.getInstance().setParentOwner(this.getDialog().getParent());
        }

        /*
         * workaround a javabug that forces the parentframe to stay always on
         * top
         */
        if (this.getDialog().getParent() != null && !CrossSystem.isMac()) {
            ((Window) this.getDialog().getParent()).setAlwaysOnTop(true);
            ((Window) this.getDialog().getParent()).setAlwaysOnTop(false);
        }

    }

    /**
     * @return
     */
    protected Point getDesiredLocation() {
        // TODO Auto-generated method stub
        return desiredLocation;
    }

    /**
     * @param ballonPanel
     * 
     */

    @Override
    protected Integer createReturnValue() {

        return this.getReturnmask();
    }

    /**
     * Override this to force expanding to top or bottom
     * 
     * @param b
     * @return true of the dart should point to top, and the balloon expands to
     *         bottom
     */
    protected boolean doExpandToBottom(final boolean b) {

        return b;
    }

    /**
     * override this method to force expanding to left or right
     * 
     * @param b
     * @return true if the ballon expands to the right. Dart points to left in
     *         this case
     */
    protected boolean doExpandToRight(final boolean b) {
        return b;
    }

    /**
     * @return
     */
    public Color getBorderColor() {
        return Color.DARK_GRAY;
    }

    /**
     * @return
     */
    public int[] getContentInsets() {
        // TODO Auto-generated method stub
        return new int[] { 5, 5, 5, 5 };
    }

    /**
     * @param ballonPanel2
     * @return
     */
    public Paint getPaint(final BallonPanel panel) {
        // TODO Auto-generated method stub
        return new GradientPaint(0, 0, Color.WHITE, panel.getWidth(), panel.getHeight(), new Color(255, 255, 255, 180));
    }

    /**
     * @return
     */
    public Color getShadowColor() {
        // TODO Auto-generated method stub
        return new Color(0, 0, 0, 50);
    }

    @Override
    protected void layoutDialog() {

        this.dialog = new InternDialog() {

            {
                BalloonDialog.this.screenshotPanel = new ScreenShotPanel("ins 10", "[]", "[]");
                if (!BalloonDialog.this.opaqueWorkaround) {
                    BalloonDialog.this.screenshotPanel.setOpaque(false);
                }
                this.setContentPane(BalloonDialog.this.screenshotPanel);

            }

            @Override
            public void setLocation(final Point p) {
                try {

                    this.setLayout(new BorderLayout());
                    BalloonDialog.this.ballonPanel.relayout(p);

                    p.x += BalloonDialog.this.ballonPanel.getXOffset();
                    p.y += BalloonDialog.this.ballonPanel.getYOffset();

                    final boolean v = this.isVisible();
                    this.setVisible(false);
                    super.setLocation(p);
                    if (BalloonDialog.this.opaqueWorkaround) {
                        final Image screenshot = ScreensShotHelper.getScreenShot(p.x, p.y, this.getWidth(), this.getHeight());

                        BalloonDialog.this.screenshotPanel.setScreenShot(screenshot);
                    }
                    // this.invalidate();
                    // BalloonDialog.this.ballonPanel.invalidate();
                    // this.setSize(BalloonDialog.this.ballonPanel.getSize());

                    BalloonDialog.this.ballonPanel.revalidate();
                    BalloonDialog.this.ballonPanel.repaint();
                    this.setVisible(v);
                } catch (final OffScreenException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        };

        this.timerLbl = new JLabel(TimeFormatter.formatSeconds(this.getCountdown(), 0));

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.utils.swing.dialog.AbstractDialog#layoutDialogContent()
     */
    @Override
    public JComponent layoutDialogContent() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setComponent(final JComponent component) {
        this.component = component;
    }

    public void setDesiredLocation(final Point desiredLocation) {
        this.desiredLocation = desiredLocation;
    }

    @Override
    public String toString() {
        if (BinaryLogic.containsAll(this.flagMask, Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY)) {
            return ("dialog-" + this.getTitle()).replaceAll("\\W", "_");
        } else {
            return ("dialog-" + this.getTitle() + "_" + this.component.toString()).replaceAll("\\W", "_");
        }

    }

}
