package fox.player;

import fox.Out.LEVEL;
import lombok.Data;
import lombok.NonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static fox.Out.Print;

@Data
public class FoxPlayer implements iPlayer {
    private static final Map<String, File> trackMap = new LinkedHashMap<>();
    private static VolumeConverter vConv = new VolumeConverter();
    private volatile float currentPlayerVolume = vConv.getMinimum();
    private volatile boolean isCurrentPlayerMute = false;

    private String name;
    private String lastTrack;

    private ArrayList<PlayThread> threadList = new ArrayList<>();

    private boolean isParallelPlayable = false;
    private boolean showLineInfo = false;
    private boolean loop = true;



    public FoxPlayer(@NonNull String name) {
        this.name = name;
    }

    public static VolumeConverter getVolumeConverter() {
        return vConv;
    }

    @Override
    public synchronized void load(@NonNull Path audioDirectoryPath) {
        File[] files = audioDirectoryPath.toFile().listFiles();
        if (files == null || files.length == 0) {
            Print(getClass(), LEVEL.INFO, String.format("Media directory %s is empty?", audioDirectoryPath.toFile().getPath()));
            throw new FoxPlayerException(String.format("Media directory %s is empty?", audioDirectoryPath.toFile().getPath()));
        }
        Arrays.stream(files).forEach(file -> add(file.getName().substring(0, file.getName().length() - 4), file));
    }

    @Override
    public synchronized void add(@NonNull String trackName, @NonNull File sourceFile) {
        trackMap.put(trackName, sourceFile);
    }

    public synchronized void play(@NonNull String trackName) {
        try {
            this.play(trackName, loop);
        } catch (FoxPlayerException fpe) {
            fpe.printStackTrace();
        }
    }

    @Override
    public synchronized void play(@NonNull String trackName, boolean isLooped) throws FoxPlayerException {
        lastTrack = trackName;
        if (isCurrentPlayerMute) {return;}
        if (trackMap.containsKey(trackName)) {
//            Print(getClass(), LEVEL.DEBUG, "FoxPlayer.play: The track '" + trackName + "' was found in the trackMap.");
            if (!isParallelPlayable) {
                stop();
            }
            threadList.add(new PlayThread(getName(), trackMap.get(trackName), vConv.volumePercentToGain(currentPlayerVolume), isLooped));
        } else {
            stop();
            Print(getClass(), LEVEL.DEBUG, "FoxPlayer.play: The track '" + trackName + "' is absent in the trackMap.");
            throw new FoxPlayerException(String.format("FoxPlayer.play: The track '%s' is absent in the trackMap.", trackName));
        }

    }

    @Override
    public void mute(boolean mute) {
        isCurrentPlayerMute = mute;
        if (threadList != null && threadList.size() > 0) {
            for (PlayThread playThread : threadList) {
                playThread.mute(isCurrentPlayerMute);
            }
        }
    }

    @Override
    public void setVolume(float volume) {
        currentPlayerVolume = volume;
        for (PlayThread playThread : threadList) {
            playThread.setVolume(vConv.volumePercentToGain(currentPlayerVolume));
        }
    }

    @Override
    public void stop() {
        if (threadList != null && threadList.size() > 0) {
            for (PlayThread thread : threadList) {
                if (thread == null) {
                    continue;
                }
                thread.close();
                if (thread.getException() != null) {
                    thread.getException().printStackTrace();
                }
            }
            threadList.clear();
        }
    }

    public void setLooped(boolean b) {
        loop = b;
    }
}
