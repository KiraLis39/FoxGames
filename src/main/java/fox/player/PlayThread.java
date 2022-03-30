package fox.player;

import fox.Out;
import lombok.NonNull;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import static fox.Out.Print;
import static fox.player.FoxPlayer.getVolumeConverter;

public class PlayThread extends Thread {
    private final Thread currentThread;
    private final File track;
    private final boolean isLooped;

    private Thread vfThread;
    private FloatControl masterVolume;
    private BooleanControl muteControl;
    private volatile Exception ex;

    private volatile boolean isBraked = false;
    private boolean isLoopFloatedAlready = false;
    private final int audioBufDim = 8192; // default 4096
    private final float volume;


    public PlayThread(@NonNull String name, File track, float volume, boolean isLooped) {
        setName(name);
        currentThread = this;

        this.track = track;
        this.volume = volume;
        this.isLooped = isLooped;

        start();
    }

    @Override
    public void run() {
        Print(getClass(), Out.LEVEL.DEBUG, "FoxPlayer.play: The '" + track.getName() + "' is played...");

        SourceDataLine line = null;
        do {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(track)); // default 8192 byte
                 AudioInputStream in = AudioSystem.getAudioInputStream(bis)
            ) {
                if (in == null) {
                    throw new RuntimeException("Media.musicPlay: The track '" + track.getName() + "' has problem with input stream?..");
                }

                AudioFormat targetFormat = new DefaultFormat01(in.getFormat());
                try (AudioInputStream dataIn = AudioSystem.getAudioInputStream(targetFormat, in)) {
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat); // get a line from a mixer in the system with the wanted format
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    if (line == null) {
                        throw new RuntimeException("Media.musicPlay: The track line is null. " +
                                "A problem with info or format?\n\t(target:\n" + info + ";\n\tformat:\n" + targetFormat + ").");
                    } else {
                        line.open();
                    }
                    getControls(line, volume);
                    line.start();

                    byte[] buffer = new byte[audioBufDim];
                    if (isBraked || currentThread.isInterrupted() || isInterrupted()) {
                        return;
                    } else {
                        if (!isLoopFloatedAlready) {volumeFloater(1);}

                        int nBytesRead;
                        while ((nBytesRead = dataIn.read(buffer, 0, buffer.length)) != -1) {
                            if (isBraked() || currentThread.isInterrupted() || isInterrupted()) {
                                break;
                            }
                            try {line.write(buffer, 0, nBytesRead);
                            } catch (IllegalArgumentException iae) {
                                iae.printStackTrace();
                                interrupt();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                ex = e;
                interrupt();
                currentThread.interrupt();
            } finally {
                if (line != null) {
//                    line.drain();
                    line.stop();
                    line.close();
                }
            }
        } while (isLooped && !isInterrupted());
    }

    private void volumeFloater(int vector) {
        if (vfThread != null && vfThread.isAlive()) {
            vfThread.interrupt();
        }
        float vfStep = 0.25f;
        float aimVolume;
        if (vector == 1) {
            aimVolume = masterVolume.getValue();
        } else {
            aimVolume = FoxPlayer.getVolumeConverter().getMinimum();
        }

        vfThread = new Thread(() -> {
            try {
                if (vector == 1) {
                    while (masterVolume.getValue() < aimVolume - 1) {
                        masterVolume.setValue(masterVolume.getValue() + vfStep);
                        sleep(50);
                    }
                    masterVolume.setValue(aimVolume);
                } else {
                    while (masterVolume.getValue() > aimVolume + 1) {
                        masterVolume.setValue(masterVolume.getValue() - vfStep);
                        sleep(20);
                    }
                    masterVolume.setValue(aimVolume);
                }
            } catch (InterruptedException e) {
                currentThread().interrupt();
            } finally {
                isLoopFloatedAlready = true;
            }
        }) {
            {
                setDaemon(true);
                if (vector == 1) {
                    masterVolume.setValue(getVolumeConverter().getMinimum() / 2);
                }
            }
        };
        vfThread.start();
    }

    private boolean isBraked() {
        return isBraked;
    }

    public void close() {
        new Thread(() -> {
            System.out.println("\nTry to stop thread '" + getName() + "'...");
            try {
                volumeFloater(0);
                vfThread.join();
                isBraked = true;
                interrupt();
                currentThread.interrupt();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.err.println("Media thread '" + getName() + "' was stopped.");
        }).start();
    }

    public Throwable getException() {
        return ex;
    }

    public void mute(boolean isMuted) {
        muteControl.setValue(isMuted);
    }

    public void setVolume(float volume) {
        masterVolume.setValue(volume);
    }

    private void getControls(SourceDataLine line, float volume) {
        muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        muteControl.setValue(false);

        masterVolume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        masterVolume.setValue(volume);
    }

    private static class DefaultFormat01 extends AudioFormat {
        public DefaultFormat01(AudioFormat baseFormat) {
            super(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
        }
    }
}
