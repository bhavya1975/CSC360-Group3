package com.rrrsvg.analysis;

/**
 * Statistical summary of an individual color or luminance channel
 * extracted using ImageJ.
 */
public class ChannelStatistics {

    private final String channelName;
    private final int min;
    private final int max;
    private final double mean;
    private final double stdDev;
    private final int median;
    private final long totalPixels;
    private final int[] histogram; // 256 bins

    public ChannelStatistics(String channelName, int min, int max, double mean,
                             double stdDev, int median, long totalPixels, int[] histogram) {
        this.channelName = channelName;
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stdDev = stdDev;
        this.median = median;
        this.totalPixels = totalPixels;
        this.histogram = (histogram != null) ? histogram.clone() : new int[256];
    }

    public String getChannelName() {
        return channelName;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public double getMean() {
        return mean;
    }

    public double getStdDev() {
        return stdDev;
    }

    public int getMedian() {
        return median;
    }

    public long getTotalPixels() {
        return totalPixels;
    }

    public int[] getHistogram() {
        return histogram.clone();
    }

    public String formatSummary() {
        return String.format("%s Channel -> Min: %d, Max: %d, Mean: %.2f, StdDev: %.2f, Median: %d",
                channelName, min, max, mean, stdDev, median);
    }

    @Override
    public String toString() {
        return formatSummary();
    }
}

