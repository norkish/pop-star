package utils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

public class Counter<T extends Comparable<T>> {

	Map<T, Integer> counter = new TreeMap<T, Integer>();

	public void incrementCountFor(T value) {
		Integer currCount = counter.get(value);
		if (currCount == null)
			counter.put(value, 1);
		else
			counter.put(value, currCount + 1);
	}

	public Map<T, Integer> getUnderlylingMap() {
		return counter;
	}

	public void createHistogram(String plotTitle, String xaxis, String yaxis, String filename) {
		final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		
		for (Entry<T, Integer> entry : counter.entrySet()) {
			dataset.addValue(entry.getValue(), "", entry.getKey());
		}
		
		JFreeChart barChart = ChartFactory.createBarChart(plotTitle, xaxis, yaxis, dataset,
				PlotOrientation.VERTICAL, true, true, false);

		int width = 3*640; /* Width of the image */
		int height = 3*480; /* Height of the image */
		File BarChart = new File(filename);
		if (BarChart.exists())
			BarChart.delete();
		try {
			ChartUtilities.saveChartAsJPEG(BarChart, barChart, width, height);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
