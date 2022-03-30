package fox.player;

import lombok.Data;

@Data
public class VolumeConverter {
    private float minimum = -80f;
    private float maximum = 6f;

    /**
     * Метод преобразовывает значения процентов громкости в
     * gain для аудио-устройств.
     * @param percent текущий процент громкости (от 0 до 100)
     * @return gain аудио-устройства (от {@param minimum} до {@param maximum}).
     */
    public float volumePercentToGain(float percent) {
//        float gain = (maximum - minimum) * (percent / 100f);
        float gain = minimum - ((minimum - maximum) * (percent / 100f));
//        System.out.println("Income percent: " + percent + "; Gain: " + gain);
        return gain;
    }

    /**
     * Метод преобразовывает gain аудио-устройства в
     * значение процентов громкости для ползунков.
     * @param gain текущий гейн аудио-устройства (от {@param minimum} до {@param maximum})
     * @return значение процентов (от 0 до 100).
     */
    public int gainToVolumePercent(float gain) {
//        int min = -80, max = 6;
        return 0;
    }
}
