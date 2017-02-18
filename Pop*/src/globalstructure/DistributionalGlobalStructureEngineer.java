package globalstructure;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import data.BackedDistribution;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.ParsedMusicXMLObject;
import utils.Triple;

public class DistributionalGlobalStructureEngineer extends GlobalStructureEngineer {

	public static class DistributionalGlobalStructureEngineerMusicXMLModel extends MusicXMLModel {

		BackedDistribution<GlobalStructure> distribution = null;
		private Map<GlobalStructure, List<Integer>> structureToSongIdx = new HashMap<GlobalStructure, List<Integer>>();
		private int trainCount = 0;
		
		@Override
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			distribution = null;
			
			SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructureByFormStart = musicXML.getGlobalStructureByFormStart();
			SegmentType[] globalStructure = new SegmentType[globalStructureByFormStart.size()];
			
			int i = 0;
			for (Integer segmentType : globalStructureByFormStart.keySet()) {
				globalStructure[i++] = globalStructureByFormStart.get(segmentType).getFirst();
			}
			GlobalStructure structureX = new GlobalStructure(globalStructure); 

			List<Integer> songsWithStructureX = structureToSongIdx.get(structureX);
			if (songsWithStructureX == null) {
				songsWithStructureX = new ArrayList<Integer>();
				structureToSongIdx.put(structureX, songsWithStructureX);
			}
			songsWithStructureX.add(trainCount);
			
			trainCount++;
		}

		public GlobalStructure sampleAccordingToDistribution() {
			if (distribution == null) {
				generateDistribution();
			}
			return distribution.sampleAccordingToDistribution();
		}

		private void generateDistribution() {
			distribution = new BackedDistribution<GlobalStructure>(structureToSongIdx );
		}

		@Override
		public void toGraph() {
			final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			
			int maxNumberRangeValues = 20;
			
			Map<String, Integer> sortedDistro = new HashMap<String,Integer>();
			
			for (GlobalStructure globalStructure : structureToSongIdx.keySet()) {
				StringBuilder globalStructureRepresentationBuilder = new StringBuilder(); 
				for (SegmentType segmentType : globalStructure) {
					if (segmentType != SegmentType.INTERLUDE) {
						globalStructureRepresentationBuilder.append(segmentType.toString().charAt(0));
					}
				}
				String globalStructureRepresentation = globalStructureRepresentationBuilder.toString();
				Integer count = sortedDistro.get(globalStructureRepresentation);
				if (count == null) {
					sortedDistro.put(globalStructureRepresentation, 1);
				} else {
					sortedDistro.put(globalStructureRepresentation, count + 1);
				}
			}
			
			List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(sortedDistro.entrySet());
			Collections.sort(list, new ValueThenKeyComparator<String, Integer>());
			
			List<Entry<String, Integer>> displayedItems = list.subList(0, Math.min(maxNumberRangeValues, list.size()));
			int totalItems = 0;
			for (Entry<String, Integer> entry : displayedItems) {
				totalItems += entry.getValue();
			}
			
			double maxValue = 0.;
			for (Entry<String, Integer> globalStructureRepresentation : displayedItems) {
				double value = globalStructureRepresentation.getValue() / (double) totalItems;
				if (value > maxValue) {
					maxValue = value;
				}
				dataset.addValue(value, "", globalStructureRepresentation.getKey());
			}
			int numberOfRangeValues = 5;
			double tickUnit = Math.floor((maxValue/numberOfRangeValues) * 100) / 100;
			
			JFreeChart barChart = ChartFactory.createBarChart(null, "Global Structure", "Probability", dataset,
					PlotOrientation.VERTICAL, false, false, false);
			
			CategoryPlot plot = barChart.getCategoryPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.setOutlineVisible(false);

			BarRenderer renderer = (BarRenderer) plot.getRenderer();
			renderer.setBarPainter(new StandardBarPainter());
			renderer.setSeriesPaint(0,Color.BLUE);

			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

			rangeAxis.setTickUnit(new NumberTickUnit(tickUnit));
			
			int width = 640; /* Width of the image */
			int height = 480; /* Height of the image */
			File BarChart = new File(GRAPH_DIR + "/global_structure.jpeg");
			if (BarChart.exists())
				BarChart.delete();
			try {
				ChartUtilities.saveChartAsJPEG(BarChart, barChart, width, height);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private DistributionalGlobalStructureEngineerMusicXMLModel model;

	public DistributionalGlobalStructureEngineer() {
		this.model = (DistributionalGlobalStructureEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	
	@Override
	public GlobalStructure generateStructure() {
		return model.sampleAccordingToDistribution();
	}

}
