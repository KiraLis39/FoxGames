package fox;

import lombok.Data;
import render.FoxRender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.concurrent.TimeUnit;

@Data
public class FoxLogo implements Runnable {
    public enum IMAGE_STYLE {FILL, DEFAULT, WRAP}
    private IMAGE_STYLE imStyle = IMAGE_STYLE.DEFAULT;

    public enum BACK_STYLE {OPAQUE, DEFAULT, COLOR}
    private BACK_STYLE bStyle = BACK_STYLE.OPAQUE;

    private JFrame logoFrame;
    private Thread engine;

    private BufferedImage[] images;
    private Color logoBackColor, handColor;
    private Raster raster;
    private Object data;

    private int breakKey = KeyEvent.VK_ESCAPE;
    private int picCounter = -1;
    private int fps = 30, imageShowTime = 5000;

    private String text;
    private float alphaGrad = 0f;

    private boolean isBreaked = false;
    private boolean hightQualityMode = false;

    protected long timeStamp;

    protected Font customFont;


    public void start(BufferedImage[] textureFilesMassive) {
        start(null, textureFilesMassive, breakKey, IMAGE_STYLE.DEFAULT, BACK_STYLE.DEFAULT);
    }

    public void start(String text, BufferedImage[] textureFilesMassive) {
        start(text, textureFilesMassive, breakKey, imStyle, bStyle);
    }

    public void start(String text, BufferedImage[] textureFilesMassive, IMAGE_STYLE is) {
        start(text, textureFilesMassive, breakKey, is, BACK_STYLE.DEFAULT);
    }

    public void start(String text, BufferedImage[] textureFilesMassive, IMAGE_STYLE is, BACK_STYLE bs) {
        start(text, textureFilesMassive, breakKey, is, bs);
    }

    public void start(String text, BufferedImage[] textureFilesMassive, int _breakKey, IMAGE_STYLE is, BACK_STYLE bs) {
        if (textureFilesMassive == null) {
            throw new RuntimeException("StartLogoRenderer: start: Error. Textures massive is NULL.");
        }
        this.text = text;
        Out.Print(FoxLogo.class, Out.LEVEL.INFO, "Load StartLogo`s images count: " + textureFilesMassive.length);
        images = textureFilesMassive;

        Out.Print(FoxLogo.class, Out.LEVEL.INFO, "Set StartLogo`s breakKey to " + KeyEvent.getKeyText(_breakKey) + "\n");
        breakKey = _breakKey;

        this.imStyle = is;
        this.bStyle = bs;

        engine = new Thread(this);
        engine.start();
    }

    @Override
    public void run() {
        loadNextImage();
        timeStamp = System.currentTimeMillis();

        logoFrame = new JFrame() {
            private boolean rising = true, hiding = false;

            @Override
            public void paint(Graphics g) {
                if (isBreaked) {return;}
                super.paint(g);

                Graphics2D g2D = (Graphics2D) g;
                FoxRender.setMedRender(g2D);

                if (rising) gradeUp();

                if (bStyle == BACK_STYLE.DEFAULT) {
                    g2D.setColor(logoBackColor);
                    g2D.fillRect(0, 0, getWidth(), getHeight());
                } else if (bStyle == BACK_STYLE.COLOR) {
                    g2D.setColor(handColor == null ? Color.GREEN : handColor);
                    g2D.fillRect(0, 0, getWidth(), getHeight());
                }

                g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaGrad));

                Float imageWidth, imageHeight;
                if (imStyle == IMAGE_STYLE.WRAP) {
                    imageWidth = (float) images[picCounter].getWidth();
                    imageHeight = (float) images[picCounter].getHeight();
                    while (imageWidth > Toolkit.getDefaultToolkit().getScreenSize().width) {
                        imageWidth -= 4;
                        imageHeight -= 2;
                    }
                    while (imageHeight > Toolkit.getDefaultToolkit().getScreenSize().height) {
                        imageHeight -= 4;
                        imageWidth -= 2.5f;
                    }
                } else if (imStyle == IMAGE_STYLE.FILL) {
                    imageWidth = (float) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
                    imageHeight = (float) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
                } else {
                    imageWidth = (float) images[picCounter].getWidth();
                    imageHeight = (float) images[picCounter].getHeight();
                }

                drawImage(g2D, imageWidth.intValue(), imageHeight.intValue());
                drawText(g2D);

                g2D.dispose();

                if (System.currentTimeMillis() - timeStamp > imageShowTime && hiding) {
                    gradeDown();
                }
                if (alphaGrad == 0) {
                    loadNextImage();
                }
            }

            private void drawText(Graphics2D g2D) {
                if (text != null && !text.isBlank()) {
                    g2D.setColor(Color.BLACK);
                    if (customFont != null) {
                        g2D.setFont(customFont);
                    }
                    g2D.drawString(text, 30, 30);
                }
            }

            private void drawImage(Graphics2D g2D, int w, int h) {
                g2D.drawImage(images[picCounter],
                        Toolkit.getDefaultToolkit().getScreenSize().width / 2 - w / 2,
                        Toolkit.getDefaultToolkit().getScreenSize().height / 2 - h / 2,
                        w, h, null);
            }

            private void gradeUp() {
                if (alphaGrad > 0.94f) {
                    alphaGrad = 1f;
                    rising = false;
                    hiding = true;
                } else {
                    alphaGrad += 0.05f;
                }
            }

            private void gradeDown() {
                if (alphaGrad < 0.076f) {
                    alphaGrad = 0f;
                    rising = true;
                    hiding = false;
                } else {
                    alphaGrad -= 0.075f;
                }
            }

            {
                setFocusable(true);
                setUndecorated(true);
                setBackground(new Color(0, 0, 0, 0));
                setExtendedState(Frame.MAXIMIZED_BOTH);

                inAc(this);

                setLocationRelativeTo(null);
                setVisible(true);
            }
        };

        while (!isBreaked && !engine.isInterrupted()) {
            try {
                logoFrame.repaint();
                TimeUnit.MILLISECONDS.sleep(1000 / fps);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        finalLogo();
        logoFrame.dispose();
    }

    private void inAc(JFrame logo) {
        InputAction.add("logoFrame", logo);
        InputAction.set("logoFrame", "final", breakKey, 0, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finalLogo();
            }
        });
    }

    private void loadNextImage() {
        picCounter++;

        if (picCounter >= images.length) {
            isBreaked = true;
        } else {
            timeStamp = System.currentTimeMillis();
            raster = images[picCounter].getRaster();
            data = raster.getDataElements(1, images[picCounter].getHeight() / 2, null);
            logoBackColor = new Color(images[picCounter].getColorModel().getRGB(data), true);
            alphaGrad = 0;
        }
    }

    public void finalLogo() {
        isBreaked = true;
    }

    public void setBackgroundColor(Color c) {
        handColor = c;
    }

    public boolean isActive() {
        return engine.isAlive();
    }

    public void join() throws InterruptedException {
        engine.join();
    }

    public void setHQmode(boolean hightQualityMode) {
        this.hightQualityMode = hightQualityMode;
    }

    public void setCustomFont(Font customFont) {
        this.customFont = customFont;
    }
}