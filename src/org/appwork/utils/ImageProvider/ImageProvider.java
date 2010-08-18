/**
 * Copyright (c) 2009 - 2010 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.ImageProvider
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.ImageProvider;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;

import sun.awt.image.ToolkitImage;

/**
 * This class grants easy access to images stored in APPROOT/images/**.png
 * 
 * @author $Author: unknown$
 * 
 */
public class ImageProvider {
    /**
     * Hashcashmap to cache images.
     */
    private static HashMap<String, BufferedImage> IMAGE_CACHE = new HashMap<String, BufferedImage>();
    private static HashMap<String, ImageIcon> IMAGEICON_CACHE = new HashMap<String, ImageIcon>();
    private static HashMap<Icon, Icon> DISABLED_ICON_CACHE = new HashMap<Icon, Icon>();

    private static Object LOCK = new Object();
    // stringbuilder die concat strings fast
    private static StringBuilder SB = new StringBuilder();

    /**
     * @param bufferedImage
     * @return
     */
    public static BufferedImage convertToGrayScale(final BufferedImage bufferedImage) {
        final BufferedImage dest = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        final Graphics2D g2 = dest.createGraphics();
        g2.drawImage(bufferedImage, 0, 0, null);
        g2.dispose();
        return dest;

    }

    /**
     * Creates a dummy Icon
     * 
     * @param string
     * @param i
     * @param j
     * @return
     */
    public static Image createIcon(final String string, final int width, final int height) {
        final int w = width;
        final int h = height;

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        final GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);

        final Graphics2D g = image.createGraphics();
        int size = 1 + width / string.length();
        // find max font size
        int ww = 0;
        int hh = 0;
        while (size > 0) {
            size--;
            g.setFont(new Font("Arial", Font.BOLD, size));
            ww = g.getFontMetrics().stringWidth(string);
            hh = g.getFontMetrics().getAscent();
            if (ww < w - 4 && hh < h - 2) {
                break;
            }
        }
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w - 1, h - 1);
        g.draw3DRect(0, 0, w - 1, h - 1, true);
        g.setColor(Color.BLACK);
        g.drawString(string, (w - ww) / 2, hh + (h - hh) / 2);
        g.dispose();
        return image;

    }

    /**
     * 
     * @param name
     *            to the png file
     * @param createDummy
     *            TODO
     * @return
     * @throws IOException
     */
    public static Image getBufferedImage(final String name, final boolean createDummy) throws IOException {
        synchronized (ImageProvider.LOCK) {
            if (ImageProvider.IMAGE_CACHE.containsKey(name)) { return ImageProvider.IMAGE_CACHE.get(name); }

            final File absolutePath = Application.getRessource("images/" + name + ".png");
            try {
                Log.L.info("Init Image: " + absolutePath.getAbsolutePath());
                final BufferedImage image = ImageIO.read(absolutePath);
                ImageProvider.IMAGE_CACHE.put(name, image);
                return image;
            } catch (final IOException e) {
                Log.L.severe("Could not Init Image: " + absolutePath.getAbsolutePath());
                Log.L.severe("Image exists: " + absolutePath.exists());
                try {
                    final File images = Application.getRessource("images/");
                    final String[] list = images.list();
                    if (list != null && list.length > 0) {
                        for (final String image : list) {
                            Log.L.severe("Existing images: " + image);
                        }
                    } else {
                        Log.L.severe("empty or not a folder: " + images);
                    }
                } catch (final Throwable e2) {
                }
                if (createDummy) {
                    Log.exception(e);
                    return ImageProvider.createIcon(name.toUpperCase(), 48, 48);
                } else {
                    throw e;
                }

            }
        }
    }

    /**
     * Uses the uimanager to get a grayscaled disabled Icon
     * 
     * @param icon
     * @return
     */
    public static Icon getDisabledIcon(final Icon icon) {
        if (icon == null) { return null; }
        Icon ret = ImageProvider.DISABLED_ICON_CACHE.get(icon);
        if (ret != null) { return ret; }
        ret = UIManager.getLookAndFeel().getDisabledIcon(null, icon);
        ImageProvider.DISABLED_ICON_CACHE.put(icon, ret);
        return ret;
    }

    /**
     * Loads the image, scales it to the desired size and returns it as an
     * imageicon
     * 
     * @param name
     * @param width
     * @param height
     * @param createDummy
     *            TODO
     * @return
     * @throws IOException
     */
    public static ImageIcon getImageIcon(final String name, int width, int height, final boolean createDummy) throws IOException {
        synchronized (ImageProvider.LOCK) {
            ImageProvider.SB.delete(0, ImageProvider.SB.capacity());
            ImageProvider.SB.append(name);
            ImageProvider.SB.append('_');
            ImageProvider.SB.append(width);
            ImageProvider.SB.append('_');
            ImageProvider.SB.append(height);
            String key;
            if (ImageProvider.IMAGEICON_CACHE.containsKey(key = ImageProvider.SB.toString())) { return ImageProvider.IMAGEICON_CACHE.get(key); }
            final Image image = ImageProvider.getBufferedImage(name, createDummy);
            final double faktor = Math.max((double) image.getWidth(null) / width, (double) image.getHeight(null) / height);
            width = (int) (image.getWidth(null) / faktor);
            height = (int) (image.getHeight(null) / faktor);
            final ImageIcon imageicon = new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
            ImageProvider.IMAGEICON_CACHE.put(key, imageicon);
            return imageicon;
        }
    }

    /**
     * @param image
     * @param imageIcon
     * @param i
     * @param j
     */
    public static BufferedImage merge(final Image image, final Image b, final int xoffset, final int yoffset) {
        final int width = Math.max(image.getWidth(null), xoffset + b.getWidth(null));
        final int height = Math.max(image.getHeight(null), yoffset + b.getHeight(null));
        final BufferedImage dest = new BufferedImage(width, height, Transparency.TRANSLUCENT);
        final Graphics2D g2 = dest.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.drawImage(b, xoffset, yoffset, null);
        g2.dispose();
        return dest;
    }

    /**
     * Scales a buffered Image to the given size. This method is NOT cached. so
     * take care to cache it externally if you use it frequently
     * 
     * @param img
     * @param width
     * @param height
     * @return
     */
    public static Image scaleBufferedImage(final BufferedImage img, int width, int height) {
        if (img == null) { return null; }
        final double faktor = Math.max((double) img.getWidth() / width, (double) img.getHeight() / height);
        width = (int) (img.getWidth() / faktor);
        height = (int) (img.getHeight() / faktor);
        if (faktor == 1.0) { return img; }
        return img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

    }

    /**
     * Scales an imageicon to w x h.<br>
     * like {@link #scaleBufferedImage(BufferedImage, int, int)}, this Function
     * is NOT cached. USe an external cache if you use it frequently
     * 
     * @param img
     * @param w
     * @param h
     * @return
     */
    public static ImageIcon scaleImageIcon(final ImageIcon img, final int w, final int h) {
        // already has the desired size?
        if (img.getIconHeight() == h && img.getIconWidth() == w) { return img; }

        BufferedImage dest;

        if (img.getImage() instanceof ToolkitImage) {
            dest = new BufferedImage(w, h, Transparency.TRANSLUCENT);
            final Graphics2D g2 = dest.createGraphics();
            g2.drawImage(img.getImage(), 0, 0, null);
            g2.dispose();
        } else {
            dest = (BufferedImage) img.getImage();
        }

        return new ImageIcon(ImageProvider.scaleBufferedImage(dest, w, h));
    }

    /**
     * Converts an Icon to an Imageicon.
     * 
     * @param icon
     * @return
     */
    public static ImageIcon toImageIcon(final Icon icon) {

        if (icon == null) { return null; }
        if (icon instanceof ImageIcon) {
            return (ImageIcon) icon;
        } else {
            final int w = icon.getIconWidth();
            final int h = icon.getIconHeight();
            final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice gd = ge.getDefaultScreenDevice();
            final GraphicsConfiguration gc = gd.getDefaultConfiguration();
            final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);
            final Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return new ImageIcon(image);

        }

    }
}
