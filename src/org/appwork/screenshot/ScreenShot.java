/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.screenshot
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.screenshot;

import java.awt.AWTException;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import javax.swing.JWindow;

/**
 * @author thomas
 * 
 */
public class ScreenShot {
    public static void main(final String[] args) throws AWTException, InterruptedException {
        new ScreenShot().start();
    }

    private BufferedImage complete;

    public ScreenShot() {

    }

    /**
     * @throws AWTException
     * 
     */
    private void captureScreen() throws AWTException {

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();

        // Get size of each screen
        final Robot robot;

        robot = new Robot();

        // for (final GraphicsDevice screen : screens) {
        int xMax = 0;
        int xMin = 0;
        int yMax = 0;
        int yMin = 0;
        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
            xMax = Math.max(xMax, bounds.x + bounds.width);
            yMax = Math.max(bounds.y + bounds.height, yMax);
            yMin = Math.min(yMin, bounds.y);
            xMin = Math.min(xMin, bounds.x);
        }
        this.complete = new BufferedImage(xMax - xMin, yMax - yMin, Transparency.TRANSLUCENT);
        final Graphics2D g2 = this.complete.createGraphics();

        for (final GraphicsDevice screen : screens) {
            final DisplayMode dm = screen.getDisplayMode();
            System.out.println(screen.getDefaultConfiguration().getBounds());
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();
            final int screenWidth = dm.getWidth();
            final int screenHeight = dm.getHeight();

            System.out.println(screen + " : " + screenWidth + "x" + screenHeight);
            final Rectangle rect = new Rectangle(screenWidth, screenHeight);
            rect.setLocation(bounds.x, bounds.y);

            final BufferedImage image = robot.createScreenCapture(rect);
            g2.drawImage(image, bounds.x, bounds.y, null);

        }
        g2.dispose();
        final JWindow window = new JWindow() {

            @Override
            public void paint(final Graphics g) {
                g.drawImage(ScreenShot.this.complete, 0, 0, null);
                // g.setColor(Color.RED);
                // g.drawRect(0, 0, screenWidth, screenHeight);
            }
        };

        // this.windows.add(new ScreenWindow(window, image, bounds));
        window.setSize(this.complete.getWidth(), this.complete.getHeight());
        window.setVisible(true);
        // // screen.setFullScreenWindow(window);
        // System.out.println(window.getLocation());
        // window.setVisible(true);
        // try {
        // Dialog.getInstance().showConfirmDialog(0, "", "", new
        // ImageIcon(ImageProvider.getScaledInstance(dest, 1500, 1500,
        // RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, true)), null,
        // null);
        // } catch (final DialogClosedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (final DialogCanceledException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

    }

    public BufferedImage getComplete() {
        return this.complete;
    }

    /**
     * @throws AWTException
     * @throws InterruptedException
     * 
     */
    private void start() throws AWTException, InterruptedException {
        this.captureScreen();

        final Magnifyer mag = new Magnifyer(this);

        Thread.sleep(30000);
        System.exit(1);

    }
}
