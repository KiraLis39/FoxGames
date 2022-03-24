package fox.player;

public class VolumeConverter {

    private static final float gradientRange = -1f;
    private static final float minimum = -65f;
    /**
     * Метод преобразовывает значения процентов громкости в
     * gain для аудио-устройств.
     * @param percent текущий процент громкости (от 0 до 100)
     * @return gain аудио-устройства (от -80 до 6).
     */
    public static float volumePercentToGain(float percent) {
//        int min = -80, max = 6;
        float gain = minimum - (minimum * (percent / 100f));
//        System.out.println("Income percent: " + percent + "; Gain: " + gain);
        return gain;
    }

    /**
     * Метод преобразовывает gain аудио-устройства в
     * значение процентов громкости для ползунков.
     * @param gain текущий гейн аудио-устройства (от -80 до 6)
     * @return значение процентов (от 0 до 100).
     */
    public static int gainToVolumePercent(float gain) {
//        int min = -80, max = 6;
        return 0;
    }
}
