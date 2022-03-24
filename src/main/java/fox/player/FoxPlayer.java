package fox.player;

import fox.Out.LEVEL;
import lombok.Data;
import lombok.NonNull;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static fox.Out.Print;

@Data
public class FoxPlayer implements iPlayer {
    private static final Map<String, File> trackMap = new LinkedHashMap<>();

    private String name;
    private String lastTrack;

    private Thread thread;

    private boolean isParallelPlayable = false;
    private boolean showLineInfo = false;
    private boolean isLooped = false;

    private int audioBufDim = 4096; // 4096

    private SourceDataLine line;
    private FloatControl masterGain = new EmptyFloatControl();
    private BooleanControl muteControl = new EmptyBooleanControl();


    public FoxPlayer(@NonNull String name) {
        this.name = name;
    }

    @Override
    public void load(@NonNull Path audioDirectoryPath) {
        File[] files = audioDirectoryPath.toFile().listFiles();
        if (files == null || files.length == 0) {
            Print(getClass(), LEVEL.INFO, String.format("Media directory %s is empty?", audioDirectoryPath.toFile().getPath()));
            throw new FoxPlayerException(String.format("Media directory %s is empty?", audioDirectoryPath.toFile().getPath()));
        }
        Arrays.stream(files).forEach(file -> add(file.getName().substring(0, file.getName().length() - 4), file));
    }

    @Override
    public void add(@NonNull String trackName, @NonNull File sourceFile) {
        trackMap.put(trackName, sourceFile);
    }

    public void play(@NonNull String trackName) {
        this.play(trackName, isLooped);
    }

    @Override
    public void play(@NonNull String trackName, boolean isLooped) {
        if (muteControl.getValue()) {
            return;
        }

        if (trackMap.containsKey(trackName)) {
            Print(getClass(), LEVEL.DEBUG, "FoxPlayer.play: The track '" + trackName + "' was found in the trackMap.");
            this.lastTrack = trackName;
            this.isLooped = isLooped;
            if (!isParallelPlayable) {
                stop();
            }

            thread = new Thread(() -> {
                Print(getClass(), LEVEL.DEBUG, "FoxPlayer.play: The '" + trackName + "' is played...");

                do {
                    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(trackMap.get(lastTrack)));
                         AudioInputStream in = AudioSystem.getAudioInputStream(bis)
                    ) {
                        if (in == null) {
                            throw new RuntimeException("Media.musicPlay: The track '" + lastTrack + "' has problem with input stream?..");
                        }

                        AudioFormat targetFormat = new DefaultFormat01(in.getFormat());
                        try (AudioInputStream dataIn = AudioSystem.getAudioInputStream(targetFormat, in)) {
                            DataLine.Info info = new DataLine.Info(SourceDataLine.class, targetFormat); // get a line from a mixer in the system with the wanted format
                            line = (SourceDataLine) AudioSystem.getLine(info);
                            if (line == null) {
                                throw new RuntimeException("Media.musicPlay: The track line is null. " +
                                        "A problem with info or format?\n\t(target:\n" + info + ";\n\tformat:\n" + targetFormat + ").");
                            }

                            try {
                                line.open();
                                if (showLineInfo) {
                                    printLineInfo(line);
                                }

                                getControls(line);
                                line.start();

                                byte[] buffer = new byte[audioBufDim];
                                int nBytesRead = 0;
                                while (nBytesRead != -1 && !Thread.currentThread().isInterrupted()) {
                                    nBytesRead = dataIn.read(buffer, 0, buffer.length);
                                    if (nBytesRead != -1) {
                                        line.write(buffer, 0, nBytesRead);
                                    }
                                    Thread.sleep(audioBufDim / 90);
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
//                                line.drain();
                                line.stop();
                                line.close();
                            }
                        }
                    } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
                        e.printStackTrace();
                    }
                } while (isLooped && !Thread.currentThread().isInterrupted());

            });
            thread.start();
        } else {
            Print(getClass(), LEVEL.INFO, "FoxPlayer.play: The track '" + trackName + "' is absent in the trackMap.");
            throw new FoxPlayerException(String.format("FoxPlayer.play: The track '%s' is absent in the trackMap.", trackName));
        }

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

    @Override
    public void mute(boolean mute) {
        muteControl.setValue(mute);
        if (mute && isActive()) {
            stop();
        } else if (lastTrack != null) {
            play(lastTrack, isLooped);
        }
    }

    @Override
    public boolean isActive() {
        return thread != null && thread.isAlive() && !thread.isInterrupted();
    }

    @Override
    public void setVolume(float volume) {
        if (masterGain != null) {
            masterGain.setValue(VolumeConverter.volumePercentToGain(volume));
        }
    }

    @Override
    public void stop() {
        if (isActive()) {
            for (int i = 0; i < 3; i++) {
                System.out.println("Try to stop thread '" + thread.getName() + "'...");
                //        if (player == null) {
                //            return;
                //        }

                //        try {
                //            player.stop();
                //        } catch (Exception a) {/* IGNORE STOPPED ALREADY */}
                //        try {
                //            player.close();
                //        } catch (Exception a) {/* IGNORE CLOSED ALREADY */}

                //        try {
                //            if (dev != null && dev.isOpen()) {
                //                dev.close();
                //            }
                //        } catch (Exception e) {
                //            /* IGNORE */
                //        }
                thread.interrupt();

                try {
                    thread.join(500);
                } catch (InterruptedException e) {
                    /* IGNORE */
                }

                System.out.println("Thread '" + thread.getName() + "' was stopped: " + thread.isInterrupted());
                if (thread.isInterrupted()) {
                    break;
                }
            }

            if (!thread.isInterrupted()) {
                throw new RuntimeException("The thread '" + thread.getName() + "' isn`t interrupted!");
            }
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
}
