/**
 * Copyright (c) 2009 - 2012 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.swing.dialog
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.swing.dialog;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;

import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.TimerDialog.InternDialog;

/**
 * @author Thomas
 * 
 */
public class CenterOfScreenLocator extends AbstractLocator {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.Locator#getLocationOnScreen(javax.swing
     * .JDialog)
     */
    @Override
    public Point getLocationOnScreen(AbstractDialog<?> d) {
        InternDialog dialog = d.getDialog();
        if (!dialog.getParent().isDisplayable() || !dialog.getParent().isVisible()) {
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            return (new Point((int) (screenSize.getWidth() - dialog.getWidth()) / 2, (int) (screenSize.getHeight() - dialog.getHeight()) / 2));

        } else if (dialog.getParent() instanceof Frame && ((Frame) dialog.getParent()).getExtendedState() == Frame.ICONIFIED) {
            // dock dialog at bottom right if mainframe is not visible

            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice[] screens = ge.getScreenDevices();

            for (final GraphicsDevice screen : screens) {
                final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
                screen.getDefaultConfiguration().getDevice();

                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen.getDefaultConfiguration());
                if (bounds.contains(MouseInfo.getPointerInfo().getLocation())) {
                    
                    return correct(new Point((int) (bounds.x + bounds.getWidth() - dialog.getWidth() - 20 - insets.right), (int) (bounds.y + bounds.getHeight() - dialog.getHeight() - 20 - insets.bottom)),d); 
                    
                
                }

            }
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            return correct(new Point((int) (screenSize.getWidth() - dialog.getWidth() - 20), (int) (screenSize.getHeight() - dialog.getHeight() - 60)),d);
        } else {
            Point ret = SwingUtils.getCenter(dialog.getParent(), dialog);
            
           return  correct(ret,d);
        }

    }
   
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.utils.swing.dialog.Locator#onClose(org.appwork.utils.swing
     * .dialog.AbstractDialog)
     */
    @Override
    public void onClose(AbstractDialog<?> abstractDialog) {
        // TODO Auto-generated method stub

    }

}
