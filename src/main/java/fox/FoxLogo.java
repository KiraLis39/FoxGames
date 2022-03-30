package fox;

import fox.Out.LEVEL;
import lombok.Data;
import render.FoxRender;
import utils.InputAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.concurrent.TimeUnit;

@Data
public class FoxLogo implements Runnable {
    private Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    public enum IMAGE_STYLE {FILL, DEFAULT, WRAP}
    private IMAGE_STYLE imStyle = IMAGE_STYLE.DEFAULT;
    public enum BACK_STYLE {ASIS, PICK, COLOR}
    private BACK_STYLE bStyle = BACK_STYLE.ASIS;

    protected long timeStamp;
    protected Font customFont;

    private JFrame logoFrame;
    private Thread engine;

    private BufferedImage[] images;
    private Color logoBackColor, color;
    private Raster raster;

    private int breakKey = KeyEvent.VK_ESCAPE;
    private int picCounter = -1;
    private int fps = 30, imageShowTime = 5000;

    private String cornerLabelText;
    private float alphaGrad = 0f;

    private boolean isBreaked = false;
    private boolean hightQualityMode = false;

    public void start(String cornerLabelText, BufferedImage[] textureFilesMassive) {
        start(cornerLabelText, textureFilesMassive, imStyle, bStyle, breakKey);
    }

    public void start(String cornerLabelText, BufferedImage[] textureFilesMassive, IMAGE_STYLE imStyle, BACK_STYLE bStyle) {
        start(cornerLabelText, textureFilesMassive, imStyle, bStyle, breakKey);
    }

    public void start(String cornerLabelText, BufferedImage[] textureFilesMassive, IMAGE_STYLE imStyle, BACK_STYLE bStyle, int _breakKey) {
        if (textureFilesMassive == null) {
            throw new RuntimeException("StartLogoRenderer: start: Error. Textures massive is NULL.");
        }
        this.cornerLabelText = cornerLabelText;
        Out.Print(FoxLogo.class, LEVEL.INFO, "Load StartLogo`s images count: " + textureFilesMassive.length);
        images = textureFilesMassive;

        Out.Print(FoxLogo.class, LEVEL.INFO, "Set StartLogo`s breakKey to " + KeyEvent.getKeyText(_breakKey) + "\n");
        breakKey = _breakKey;

        this.imStyle = imStyle;
        this.bStyle = bStyle;

        engine = new Thread(this);
        engine.start();
    }

    @Override
    public void run() {
        loadNextImage();
        timeStamp = System.currentTimeMillis();

        logoFrame = new JFrame() {
            private boolean rising = true, hiding = false;

            {
                setFocusable(true);
                setUndecorated(true);
                setBackground(new Color(0, 0, 0, 0));
                setExtendedState(Frame.MAXIMIZED_BOTH);

                inAc(this);

                setLocationRelativeTo(null);
                setVisible(true);
            }

            @Override
            public void paint(Graphics g) {
                if (isBreaked) {
                    return;
                }
                super.paint(g);

                Graphics2D g2D = (Graphics2D) g;
                FoxRender.setRender(g2D, FoxRender.RENDER.MED);

                if (rising) gradeUp();

                if (bStyle == BACK_STYLE.ASIS) {
                    g2D.setColor(logoBackColor);
                    g2D.fillRect(0, 0, getWidth(), getHeight());
                } else if (bStyle == BACK_STYLE.COLOR) {
                    g2D.setColor(color == null ? Color.MAGENTA : color);
                    g2D.fillRect(0, 0, getWidth(), getHeight());
                }

                g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaGrad));

                Float imageWidth, imageHeight;
                if (imStyle == IMAGE_STYLE.WRAP) { // style is WRAP:
                    imageWidth = (float) images[picCounter].getWidth();
                    imageHeight = (float) images[picCounter].getHeight();
                    while (imageWidth > screen.width) {
                        imageWidth -= 4;
                        imageHeight -= 2;
                    }
                    while (imageHeight > screen.height) {
                        imageHeight -= 4;
                        imageWidth -= 2.5f;
                    }

                } else if (imStyle == IMAGE_STYLE.FILL) { // style is FILL:
                    imageWidth = (float) screen.getWidth();
                    imageHeight = (float) screen.getHeight();

                } else { // style is DEFAULT:
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
                if (cornerLabelText != null && !cornerLabelText.isBlank()) {
                    g2D.setColor(Color.BLACK);
                    if (customFont != null) {
                        g2D.setFont(customFont);
                    }
                    g2D.drawString(cornerLabelText, 30, 30);
                }
            }

            private void drawImage(Graphics2D g2D, int imWidth, int imHeight) {
                g2D.drawImage(images[picCounter],
                        screen.width / 2 - imWidth / 2,
                        screen.height / 2 - imHeight / 2,
                        imWidth, imHeight, logoFrame);
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
            Object data = raster.getDataElements(1, images[picCounter].getHeight() / 2, null);
            logoBackColor = new Color(images[picCounter].getColorModel().getRGB(data), true);
            alphaGrad = 0;
        }
    }

    public void finalLogo() {
        isBreaked = true;
    }

    public void join() throws InterruptedException {
        if (engine != null && engine.isAlive()) {
            engine.join();
        }
    }
}