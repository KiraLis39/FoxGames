package fox.player;

import lombok.NonNull;

import javax.sound.sampled.EnumControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

public interface iPlayer {
    void load(@NonNull Path audioDirectoryPath);
    void add(@NonNull String trackName, @NonNull File sourceFile);
    void play(@NonNull String trackName, boolean isLooped);
    void mute(boolean mute);
    void setVolume(float volume);
    void stop();

    default void printLineInfo(SourceDataLine line) {
        if (line == null) {
            System.out.println("iPlayer.printLineInfo: Line is NULL");
            return;
        }
        System.out.println(" =================== CONTROLS: ===================== ");
        System.out.println(Arrays.asList(line.getControls()));
        System.out.println(line.isControlSupported(FloatControl.Type.BALANCE));
        System.out.println(line.isControlSupported(FloatControl.Type.PAN));
        System.out.println(line.isControlSupported(FloatControl.Type.REVERB_SEND));
        System.out.println(line.isControlSupported(FloatControl.Type.REVERB_RETURN));
        System.out.println(line.isControlSupported(EnumControl.Type.REVERB));
        System.out.println(" ==================================================== ");
    }
}
