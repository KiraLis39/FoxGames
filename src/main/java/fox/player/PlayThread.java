package fox.player;

import fox.Out;
import lombok.NonNull;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import static fox.Out.Print;

public class PlayThread extends Thread {
    private final Thread currentThread;
    private FloatControl masterGain = new EmptyFloatControl();
    private BooleanControl muteControl = new EmptyBooleanControl();

    private final File track;
    private final int audioBufDim = 4096; // default 4096
    private volatile Exception ex;
    private volatile boolean isBraked = false;
    private final boolean isLooped;


    public PlayThread(@NonNull String name, File track, boolean isLooped) {
        setName(name);
        currentThread = this;

        this.track = track;
        this.isLooped = isLooped;

        start();
    }

    @Override
    public void run() {
        if (muteControl.getValue()) {
            return;
        }
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
                    getControls(line);
                    line.start();

                    byte[] buffer = new byte[audioBufDim];
                    if (isBraked || currentThread.isInterrupted() || isInterrupted()) {
                        return;
                    } else {
                        int nBytesRead;
                        while ((nBytesRead = dataIn.read(buffer, 0, buffer.length)) != -1) {
                            if (isBraked() || currentThread.isInterrupted() || isInterrupted()) {
                                break;
                            }
                            line.write(buffer, 0, nBytesRead);
                            Thread.sleep(15);
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

    private boolean isBraked() {
        return isBraked;
    }

    public void close() {
        isBraked = true;
        interrupt();
        currentThread.interrupt();
    }

    public Throwable getException() {
        return ex;
    }

    public void mute(boolean isMuted) {
        muteControl.setValue(isMuted);
    }

    public void setVolume(float volumePercentToGain) {
        masterGain.setValue(FoxPlayer.getVolumeConverter().volumePercentToGain(volumePercentToGain));
    }

    private void getControls(SourceDataLine line) {
        boolean mute = false;
        if (muteControl != null) {
            mute = muteControl.getValue();
        }
        muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        muteControl.setValue(mute);

        float volume = 0f;
        if (masterGain != null) {
            volume = masterGain.getValue();
        }
        masterGain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        masterGain.setValue(volume);
    }

    private static class EmptyBooleanControl extends BooleanControl {
        public EmptyBooleanControl() {
            super(Type.MUTE, false);
        }
    }

    private static class EmptyFloatControl extends FloatControl {
        public EmptyFloatControl() {
            super(Type.MASTER_GAIN, -80, 6, 1, 1000, 0, "dB");
        }
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
