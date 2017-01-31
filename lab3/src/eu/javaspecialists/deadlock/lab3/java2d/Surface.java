/*
 * @(#)Surface.java	1.55 06/08/09
 *
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

/*
 * @(#)Surface.java	1.55 06/08/09
 */

package eu.javaspecialists.deadlock.lab3.java2d;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;

import static java.awt.RenderingHints.*;


/**
 * Surface is the base class for the 2d rendering demos.  Demos must
 * implement the render() method. Subclasses for Surface are
 * AnimatingSurface, ControlsSurface and AnimatingControlsSurface.
 */
public abstract class Surface extends JPanel implements Printable {


    public Object AntiAlias = VALUE_ANTIALIAS_ON;
    public Object Rendering = VALUE_RENDER_SPEED;
    public AlphaComposite composite;
    public Paint texture;
    public String perfStr;            // PerformanceMonitor
    public BufferedImage bimg;
    public int imageType;
    public String name;
    public boolean clearSurface = true;
    // Demos using animated gif's that implement ImageObserver set dontThread.
    public boolean dontThread;
    public AnimatingSurface animating;

    protected long sleepAmount = 50;

    private long orig, start, frame;
    private Toolkit toolkit;
    private boolean perfMonitor, outputPerf;
    private int biw, bih;
    private boolean clearOnce;
    private boolean toBeInitialized = true;


    public Surface() {
        setDoubleBuffered(this instanceof AnimatingSurface);
        toolkit = getToolkit();
        name = this.getClass().getSimpleName();
        setImageType(0);

        // To launch an individual demo with the performance str output  :
        //    java -Djava2demo.perf= -cp Java2Demo.jar demos.Clipping.ClipAnim
        try {
            if (System.getProperty("java2demo.perf") != null) {
                perfMonitor = outputPerf = true;
            }
        } catch (Exception ex) {
        }
        if (this instanceof AnimatingSurface) {
            animating = (AnimatingSurface) this;
        }
    }

    protected Image getImage(String name) {
        return DemoImages.getImage(name, this);
    }

    public int getImageType() {
        return imageType;
    }

    public void setImageType(int imgType) {
        if (imgType == 0) {
            imageType = 1;
        } else {
            imageType = imgType;
        }
        bimg = null;
    }


    public void setAntiAlias(boolean aa) {
        AntiAlias = aa ? VALUE_ANTIALIAS_ON : VALUE_ANTIALIAS_OFF;
    }


    public void setRendering(boolean rd) {
        Rendering = rd ? VALUE_RENDER_QUALITY : VALUE_RENDER_SPEED;
    }


    public void setTexture(Object obj) {
        if (obj instanceof GradientPaint) {
            texture = new GradientPaint(0, 0, Color.white,
                getSize().width * 2, 0, Color.green);
        } else {
            texture = (Paint) obj;
        }
    }


    public void setComposite(boolean cp) {
        composite = cp
            ? AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
            : null;
    }


    public void setMonitor(boolean pm) {
        perfMonitor = pm;
    }


    public void setSleepAmount(long amount) {
        sleepAmount = amount;
    }


    public long getSleepAmount() {
        return sleepAmount;
    }


    public BufferedImage createBufferedImage(Graphics2D g2,
                                             int w,
                                             int h,
                                             int imgType) {
        BufferedImage bi = null;
        if (imgType == 0) {
            bi = (BufferedImage) g2.getDeviceConfiguration().
                createCompatibleImage(w, h);
        } else if (imgType > 0 && imgType < 14) {
            bi = new BufferedImage(w, h, imgType);
        } else if (imgType == 14) {
            bi = createBinaryImage(w, h, 2);
        } else if (imgType == 15) {
            bi = createBinaryImage(w, h, 4);
        } else if (imgType == 16) {
            bi = createSGISurface(w, h, 32);
        } else if (imgType == 17) {
            bi = createSGISurface(w, h, 16);
        }
        return bi;
    }


    // Lookup tables for BYTE_BINARY 1, 2 and 4 bits.
    static byte[] lut1Arr = new byte[]{0, (byte) 255};
    static byte[] lut2Arr = new byte[]{0, (byte) 85, (byte) 170, (byte) 255};
    static byte[] lut4Arr = new byte[]{0, (byte) 17, (byte) 34, (byte) 51,
        (byte) 68, (byte) 85, (byte) 102, (byte) 119,
        (byte) 136, (byte) 153, (byte) 170, (byte) 187,
        (byte) 204, (byte) 221, (byte) 238, (byte) 255};


    private BufferedImage createBinaryImage(int w, int h, int pixelBits) {
        int bytesPerRow = w * pixelBits / 8;
        if (w * pixelBits % 8 != 0) {
            bytesPerRow++;
        }
        byte[] imageData = new byte[h * bytesPerRow];
        IndexColorModel cm = null;
        switch (pixelBits) {
            case 1:
                cm = new IndexColorModel(pixelBits, lut1Arr.length,
                    lut1Arr, lut1Arr, lut1Arr);
                break;
            case 2:
                cm = new IndexColorModel(pixelBits, lut2Arr.length,
                    lut2Arr, lut2Arr, lut2Arr);
                break;
            case 4:
                cm = new IndexColorModel(pixelBits, lut4Arr.length,
                    lut4Arr, lut4Arr, lut4Arr);
                break;
            default: {
                new Exception("Invalid # of bit per pixel").printStackTrace();
            }
        }

        DataBuffer db = new DataBufferByte(imageData, imageData.length);
        WritableRaster r = Raster.createPackedRaster(db, w, h, pixelBits, null);
        return new BufferedImage(cm, r, false, null);
    }

    {
        startClock();
    }

    private BufferedImage createSGISurface(int w, int h, int pixelBits) {
        int rMask32 = 0xFF000000;
        int rMask16 = 0xF800;
        int gMask32 = 0x00FF0000;
        int gMask16 = 0x07C0;
        int bMask32 = 0x0000FF00;
        int bMask16 = 0x003E;

        DirectColorModel dcm = null;
        DataBuffer db = null;
        WritableRaster wr = null;
        switch (pixelBits) {
            case 16:
                short[] imageDataUShort = new short[w * h];
                dcm = new DirectColorModel(16, rMask16, gMask16, bMask16);
                db = new DataBufferUShort(imageDataUShort, imageDataUShort.length);
                wr = Raster.createPackedRaster(db, w, h, w,
                    new int[]{rMask16, gMask16, bMask16},
                    null);
                break;
            case 32:
                int[] imageDataInt = new int[w * h];
                dcm = new DirectColorModel(32, rMask32, gMask32, bMask32);
                db = new DataBufferInt(imageDataInt, imageDataInt.length);
                wr = Raster.createPackedRaster(db, w, h, w,
                    new int[]{rMask32, gMask32, bMask32},
                    null);
                break;
            default: {
                new Exception("Invalid # of bit per pixel").printStackTrace();
            }
        }

        return new BufferedImage(dcm, wr, false, null);
    }

    public Graphics2D createGraphics2D(int width,
                                       int height,
                                       BufferedImage bi,
                                       Graphics g) {

        Graphics2D g2 = null;

        if (bi != null) {
            g2 = bi.createGraphics();
        } else {
            g2 = (Graphics2D) g;
        }

        g2.setBackground(getBackground());
        g2.setRenderingHint(KEY_ANTIALIASING, AntiAlias);
        g2.setRenderingHint(KEY_RENDERING, Rendering);

        if (clearSurface || clearOnce) {
            g2.clearRect(0, 0, width, height);
            clearOnce = false;
        }

        if (texture != null) {
            // set composite to opaque for texture fills
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setPaint(texture);
            g2.fillRect(0, 0, width, height);
        }

        if (composite != null) {
            g2.setComposite(composite);
        }

        return g2;
    }

    // ...demos that extend Surface must implement this routine...
    public abstract void render(int w, int h, Graphics2D g2);

    /**
     * It's possible to turn off double-buffering for just the repaint
     * calls invoked directly on the non double buffered component.
     * This can be done by overriding paintImmediately() (which is called
     * as a result of repaint) and getting the current RepaintManager and
     * turning off double buffering in the RepaintManager before calling
     * super.paintImmediately(g).
     */
    public void paintImmediately(int x, int y, int w, int h) {
        RepaintManager repaintManager = null;
        boolean save = true;
        if (!isDoubleBuffered()) {
            repaintManager = RepaintManager.currentManager(this);
            save = repaintManager.isDoubleBufferingEnabled();
            repaintManager.setDoubleBufferingEnabled(false);
        }
        super.paintImmediately(x, y, w, h);

        if (repaintManager != null) {
            repaintManager.setDoubleBufferingEnabled(save);
        }
    }

    public void paint(Graphics g) {
        super.paint(g);

        Dimension d = getSize();

        if (biw != d.width || bih != d.height) {
            toBeInitialized = true;
            biw = d.width;
            bih = d.height;
        }

        if (imageType == 1)
            bimg = null;
        else if (bimg == null || toBeInitialized) {
            bimg = createBufferedImage((Graphics2D) g,
                d.width, d.height, imageType - 2);
            clearOnce = true;
        }

        if (toBeInitialized) {
            if (animating != null)
                animating.reset(d.width, d.height);
            toBeInitialized = false;
            startClock();
        }

        if (animating != null && animating.thread != null) {
            animating.step(d.width, d.height);
        }
        Graphics2D g2 = createGraphics2D(d.width, d.height, bimg, g);
        render(d.width, d.height, g2);
        g2.dispose();

        if (bimg != null) {
            g.drawImage(bimg, 0, 0, null);
            toolkit.sync();
        }

        if (perfMonitor) {
            LogPerformance();
        }
    }


    public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
        if (pi >= 1) {
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());
        g2d.translate(pf.getImageableWidth() / 2,
            pf.getImageableHeight() / 2);

        Dimension d = getSize();

        double scale = Math.min(pf.getImageableWidth() / d.width,
            pf.getImageableHeight() / d.height);
        if (scale < 1.0) {
            g2d.scale(scale, scale);
        }

        g2d.translate(-d.width / 2.0, -d.height / 2.0);

        if (bimg == null) {
            Graphics2D g2 = createGraphics2D(d.width, d.height, null, g2d);
            render(d.width, d.height, g2);
            g2.dispose();
        } else {
            g2d.drawImage(bimg, 0, 0, this);
        }

        return Printable.PAGE_EXISTS;
    }

    public void startClock() {
        orig = System.currentTimeMillis();
        start = orig;
        frame = 0;
    }

    private static final int REPORTFRAMES = 30;

    private void LogPerformance() {
        if ((frame % REPORTFRAMES) == 0) {
            long end = System.currentTimeMillis();
            long rel = (end - start);
            long tot = (end - orig);
            if (frame == 0) {
                perfStr = name + " " + rel + " ms";
                if (animating == null || animating.thread == null) {
                    frame = -1;
                }
            } else {
                String s1 = Float.toString((REPORTFRAMES / (rel / 1000.0f)));
                s1 = (s1.length() < 4) ? s1.substring(0, s1.length()) : s1.substring(0, 4);
                perfStr = name + " " + s1 + " fps";
            }
            if (outputPerf) {
                System.out.println(perfStr);
            }
            start = end;
        }
        ++frame;
    }

    public void repaint() {
        super.repaint();
        if (orig == 0) startClock();

    }

    // System.out graphics state information.
    public void verbose() {
        String str = "  " + name + " ";
        if (animating != null && animating.thread != null) {
            str = str.concat(" Running");
        } else if (this instanceof AnimatingSurface) {
            str = str.concat(" Stopped");
        }

        str = str.concat(" " + GlobalControls.screenCombo.getSelectedItem());

        str.concat((AntiAlias == VALUE_ANTIALIAS_ON) ? " ANTIALIAS_ON "
            : " ANTIALIAS_OFF ");
        str.concat((Rendering == VALUE_RENDER_QUALITY) ? "RENDER_QUALITY "
            : "RENDER_SPEED ");

        if (texture != null) {
            str = str.concat("Texture ");
        }

        if (composite != null) {
            str = str.concat("Composite=" + composite.getAlpha() + " ");
        }

        Runtime r = Runtime.getRuntime();
        r.gc();
        float freeMemory = (float) r.freeMemory();
        float totalMemory = (float) r.totalMemory();
        str = str.concat(((totalMemory - freeMemory) / 1024) + "K used");
        System.out.println(str);
    }


    public static void createDemoFrame(Surface surface) {
        final DemoPanel dp = new DemoPanel(surface);
        Frame f = new Frame("Java2D Demo - " + surface.name);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            public void windowDeiconified(WindowEvent e) {
                dp.start();
            }

            public void windowIconified(WindowEvent e) {
                dp.stop();
            }
        });
        f.add("Center", dp);
        f.pack();
        f.setSize(new Dimension(500, 300));
        f.setVisible(true);
        if (surface.animating != null) {
            surface.animating.start();
        }
    }
}