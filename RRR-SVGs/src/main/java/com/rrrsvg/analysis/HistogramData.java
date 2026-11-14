package com.rrrsvg.analysis;

/**
 * Encapsulates complete 256-bin RGB and Luminance histograms and statistical metrics
 * computed from an ImageJ {@link ij.process.ColorProcessor}.
 */
public class HistogramData {

    private final int[] redBins;
    private final int[] greenBins;
    private final int[] blueBins;
    private final int[] luminanceBins;
    private final int maxFrequency;

    private final ChannelStatistics redStats;
    private final ChannelStatistics greenStats;
    private final ChannelStatistics blueStats;
    private final ChannelStatistics luminanceStats;

    public HistogramData(int[] redBins, int[] greenBins, int[] blueBins, int[] luminanceBins,
                         ChannelStatistics redStats, ChannelStatistics greenStats,
                         ChannelStatistics blueStats, ChannelStatistics luminanceStats) {
        this.redBins = redBins != null ? redBins.clone() : new int[256];
        this.greenBins = greenBins != null ? greenBins.clone() : new int[256];
        this.blueBins = blueBins != null ? blueBins.clone() : new int[256];
        this.luminanceBins = luminanceBins != null ? luminanceBins.clone() : new int[256];

        this.redStats = redStats;
        this.greenStats = greenStats;
        this.blueStats = blueStats;
        this.luminanceStats = luminanceStats;

        int peak = 0;
        for (int i = 0; i < 256; i++) {
            peak = Math.max(peak, this.redBins[i]);
            peak = Math.max(peak, this.greenBins[i]);
            peak = Math.max(peak, this.blueBins[i]);
            peak = Math.max(peak, this.luminanceBins[i]);
        }
        this.maxFrequency = peak > 0 ? peak : 1;
    }

    public int[] getRedBins() {
        return redBins.clone();
    }

    public int[] getGreenBins() {
        return greenBins.clone();
    }

    public int[] getBlueBins() {
        return blueBins.clone();
    }

    public int[] getLuminanceBins() {
        return luminanceBins.clone();
    }

    public int getMaxFrequency() {
        return maxFrequency;
    }

    public ChannelStatistics getRedStats() {
        return redStats;
    }

    public ChannelStatistics getGreenStats() {
        return greenStats;
    }

    public ChannelStatistics getBlueStats() {
        return blueStats;
    }

    public ChannelStatistics getLuminanceStats() {
        return luminanceStats;
    }
}

