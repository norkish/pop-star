package segmentstructure;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

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
import org.tc33.jheatchart.HeatChart;

import composition.Measure;
import condition.ConstraintCondition;
import condition.DelayedConstraintCondition;
import condition.Rhyme;
import constraint.Constraint;
import data.MusicXMLModel;
import data.MusicXMLModelLearner;
import data.MusicXMLParser.KeyMode;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.ParsedMusicXMLObject;
import globalstructure.SegmentType;
import utils.Triple;
import utils.Utils;

public class DistributionalSegmentStructureEngineer extends SegmentStructureEngineer {

	public static class DistributionalSegmentStructureEngineerMusicXMLModel extends MusicXMLModel {

		private Map<SegmentType,Map<Integer,Integer>> measureCountDistribution = new EnumMap<SegmentType, Map<Integer,Integer>>(SegmentType.class);
		private Map<SegmentType, Integer> measureCountDistributionTotals = new EnumMap<SegmentType, Integer>(SegmentType.class); 
		// for a segment of a given length, a list of possible segment structures of that length, each defined as a list of constraints 
		private SortedMap<Integer, List<List<Triple<Integer, Double, Constraint<NoteLyric>>>>> lyricConstraintDistribution = new TreeMap<Integer, List<List<Triple<Integer, Double, Constraint<NoteLyric>>>>>();
		private Random rand = new Random();
		
		public DistributionalSegmentStructureEngineerMusicXMLModel() {
			for(SegmentType segType: SegmentType.values()) {
				measureCountDistribution.put(segType, new HashMap<Integer, Integer>());
				measureCountDistributionTotals.put(segType, 0);
			}
		}
		
		@Override
		public void trainOnExample(ParsedMusicXMLObject musicXML) {
			
			// train measure count distribution
			int prevMsr = 0;
			SegmentType prevType = null;
			final SortedMap<Integer, Triple<SegmentType, Integer, Double>> globalStructure = musicXML.getGlobalStructureByFormStart();
			for(Integer msr : globalStructure.keySet()) {
				if (prevType != null) {
					Utils.incrementValueForKey(measureCountDistribution.get(prevType), (msr-prevMsr));
					measureCountDistributionTotals.put(prevType, measureCountDistributionTotals.get(prevType)+1);
				}
				
				Triple<SegmentType, Integer, Double> globalStructureForMeasure = globalStructure.get(msr);
				if (globalStructureForMeasure != null) {
					prevType = globalStructureForMeasure.getFirst();
				} else {
					prevType = null;
				}
				prevMsr = msr;
			}

			if (prevType != null) {
				Utils.incrementValueForKey(measureCountDistribution.get(prevType), (musicXML.getMeasureCount()-prevMsr));
				measureCountDistributionTotals.put(prevType, measureCountDistributionTotals.get(prevType)+1);
			}
			
			// train constraint distribution
			SegmentType currSegType,prevSegType = null;
			List<Triple<Integer, Double, Constraint<NoteLyric>>> currentSegmentLyricConstraints = null;
			SortedMap<Integer, SortedMap<Integer, Note>> notesMap = musicXML.getNotesByPlayedMeasureAsMap();
			SortedMap<Integer, SortedMap<Double, List<Constraint<NoteLyric>>>> manuallyAnnotatedConstraints = musicXML.segmentStructure;
			
			int currentSegmentStartMeasure = 0;
			int measureIdxInSegment;
			for (int measure = 0; measure < musicXML.getMeasureCount(); measure++) {
				Triple<SegmentType, Integer, Double> globalStructureForMeasure = globalStructure.get(measure);
				if (globalStructureForMeasure != null) {
					currSegType = globalStructureForMeasure.getFirst();
				} else {
					currSegType = null;
				}
				measureIdxInSegment = measure - currentSegmentStartMeasure;
				if (currSegType == null) {
					currSegType = prevSegType;
				} else {
					System.out.println("Training on segment " + currSegType);
					// new segment
					if (prevSegType != null) { // if not first segment, add previous one.
						List<List<Triple<Integer, Double, Constraint<NoteLyric>>>> rhymeConstraintDistributionForSegLen = lyricConstraintDistribution.get(measureIdxInSegment);
						if (rhymeConstraintDistributionForSegLen == null) {
							rhymeConstraintDistributionForSegLen = new ArrayList<List<Triple<Integer, Double, Constraint<NoteLyric>>>>();
							lyricConstraintDistribution.put(measureIdxInSegment, rhymeConstraintDistributionForSegLen);
						}
						rhymeConstraintDistributionForSegLen.add(currentSegmentLyricConstraints);
					}
					
					prevSegType = currSegType;
					currentSegmentStartMeasure = measure;
					measureIdxInSegment = 0;
					currentSegmentLyricConstraints = new ArrayList<Triple<Integer, Double, Constraint<NoteLyric>>>();
				}
				
				
				SortedMap<Integer, Note> notesForMeasure = notesMap.get(measure);
				if (notesForMeasure != null) { 
					for(Integer offsetInDivs: notesForMeasure.keySet()) {
						SortedMap<Double, List<Constraint<NoteLyric>>> constraintsForMeasure = manuallyAnnotatedConstraints.get(measure);
						if (constraintsForMeasure != null) {
							double offsetInBeats = musicXML.divsToBeats(offsetInDivs,measure);
							List<Constraint<NoteLyric>> constraintsForOffset = constraintsForMeasure.get(offsetInBeats);
							if (constraintsForOffset != null) {
								for (Constraint<NoteLyric> constraint : constraintsForOffset) {
									Constraint<NoteLyric> modifiedConstraint = (Constraint<NoteLyric>) Utils.deepCopy(constraint);
//									System.out.println("Original constraint was m " + measure + " b" + offsetInBeats + " " + constraint);
									ConstraintCondition<NoteLyric> modifiedConstraintCondition = modifiedConstraint.getCondition();
									if (modifiedConstraintCondition instanceof DelayedConstraintCondition<?>) {
										// need to cast
										DelayedConstraintCondition delayedModifiedConstraintCondition = (DelayedConstraintCondition) modifiedConstraintCondition;
										int oldReferenceMeasure = delayedModifiedConstraintCondition.getReferenceMeasure();
										if (oldReferenceMeasure != DelayedConstraintCondition.PREV_VERSE) {
											delayedModifiedConstraintCondition.setReferenceMeasure(oldReferenceMeasure- currentSegmentStartMeasure);
										}
										System.out.println("In measure " + measureIdxInSegment + ", beat " + offsetInBeats + ", " + modifiedConstraint);
									}
									currentSegmentLyricConstraints.add(new Triple<Integer, Double, Constraint<NoteLyric>>(measureIdxInSegment, offsetInBeats, modifiedConstraint));
									// add phrase end constraint if it's phrase end
								}
							}
						}
					}
				}
			}
			if (prevSegType != null) { // if not first segment, add previous one.
				measureIdxInSegment = musicXML.getMeasureCount() - currentSegmentStartMeasure;
				List<List<Triple<Integer, Double, Constraint<NoteLyric>>>> rhymeConstraintDistributionForSegLen = lyricConstraintDistribution.get(measureIdxInSegment);
				if (rhymeConstraintDistributionForSegLen == null) {
					rhymeConstraintDistributionForSegLen = new ArrayList<List<Triple<Integer, Double, Constraint<NoteLyric>>>>();
					lyricConstraintDistribution.put(measureIdxInSegment, rhymeConstraintDistributionForSegLen);
				}
				rhymeConstraintDistributionForSegLen.add(currentSegmentLyricConstraints);
			}
		}

		public int sampleMeasureCountForSegmentType(SegmentType segmentType) {
			int offsetIntoDistribution = rand.nextInt(measureCountDistributionTotals.get(segmentType));
			
			Map<Integer, Integer> distForType = measureCountDistribution.get(segmentType);
			
			int accum = 0;
			for (Entry<Integer, Integer> entry : distForType.entrySet()) {
				accum += entry.getValue();
				if (accum >= offsetIntoDistribution) {
					return entry.getKey();
				}
			}
			
			throw new RuntimeException("Should never reach here");
		}

		public List<Triple<Integer, Double, Constraint<NoteLyric>>> sampleConstraints(int measureCount) {
			
			List<List<Triple<Integer, Double, Constraint<NoteLyric>>>> distForMeasureCount = lyricConstraintDistribution.get(measureCount);
			int offsetIntoDistribution = rand.nextInt(distForMeasureCount.size());
			List<Triple<Integer, Double, Constraint<NoteLyric>>> sampledLyricConstraints = distForMeasureCount.get(offsetIntoDistribution);
			
			return sampledLyricConstraints;
		}

		@Override
		public void toGraph() {
			measureCountDistributionToGraph();
			lyricConstraintDistributionToGraph();
		}

		private void measureCountDistributionToGraph() {
			int maxXValues = 20;
//			Map<SegmentType, Map<Integer, Integer>> measureCountDistribution;
			int maxLength = 0;
			for (Map<Integer, Integer> countDistributionForSegment : measureCountDistribution.values()) {
				for (Integer measureCount : countDistributionForSegment.keySet()) {
					if (measureCount > maxLength) {
						maxLength = measureCount;
					}
				}
			}
			
			// +1 for priors
			int chartXDimension = Math.min(maxXValues, maxLength); 
			int chartYDimension = measureCountDistribution.size(); 

			String[] yValues = new String[chartYDimension];
			double[][] chartValues = new double[chartYDimension][chartXDimension];
			
			// set axis labels
			int i = 0;
			for (SegmentType segmentType : measureCountDistribution.keySet()) {
				yValues[i++] = segmentType.toString();
			}
			// x-axis labels set automatically 0 to n-1
			
			// populate heatchart
			
			i = 0;
			for (SegmentType segmentType : measureCountDistribution.keySet()) {
				Map<Integer, Integer> measureCountDistributionForSegment = measureCountDistribution.get(segmentType);
				for (int j = 0; j < chartXDimension; j++) {
					Integer distributionValue = measureCountDistributionForSegment.get(j);
					chartValues[i][j] = distributionValue == null? 0.0: distributionValue;
				}
				i++;
			}
			
			Utils.normalizeByFirstDimension(chartValues);

			HeatChart chart = new HeatChart(chartValues);
			chart.setHighValueColour(Color.RED);
			chart.setLowValueColour(Color.BLUE);
			
			chart.setYAxisLabel("Segment Type");
			chart.setXAxisLabel("Segment Length");
			chart.setYValues(yValues);
			
			try {
				chart.saveToFile(new File(GRAPH_DIR + "/measure_count_by_segment.jpeg"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void lyricConstraintDistributionToGraph() {
			final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
			//			SortedMap<Integer, List<List<Triple<Integer, Double, Constraint<NoteLyric>>>>> lyricConstraintDistribution;
			
			Map<String, Integer> rhymeSchemeDistribution = new HashMap<String,Integer>();

			for (List<List<Triple<Integer, Double, Constraint<NoteLyric>>>> segmentStructuresForLength : lyricConstraintDistribution.values()) {
				for (List<Triple<Integer, Double, Constraint<NoteLyric>>> segmentStructure : segmentStructuresForLength) {
					String rhymeScheme = summarizeRhymeScheme(segmentStructure);
					if (rhymeScheme != null && !rhymeScheme.isEmpty())
						Utils.incrementValueForKey(rhymeSchemeDistribution, rhymeScheme);
				}
			}
			
			int maxNumberRangeValues = 20;
			
			List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(rhymeSchemeDistribution.entrySet());
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
			
			JFreeChart barChart = ChartFactory.createBarChart(null, "Segment Structure", "Probability", dataset,
					PlotOrientation.VERTICAL, false, false, false);
			
			CategoryPlot plot = barChart.getCategoryPlot();
			plot.setOutlineVisible(false);
			plot.setBackgroundPaint(Color.WHITE);

			BarRenderer renderer = (BarRenderer) plot.getRenderer();
			renderer.setBarPainter(new StandardBarPainter());
			renderer.setSeriesPaint(0,Color.BLUE);

			NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

			rangeAxis.setTickUnit(new NumberTickUnit(tickUnit));
			
			int width = 640; /* Width of the image */
			int height = 480; /* Height of the image */
			File BarChart = new File(GRAPH_DIR + "/segment_structure.jpeg");
			if (BarChart.exists())
				BarChart.delete();
			try {
				ChartUtilities.saveChartAsJPEG(BarChart, barChart, width, height);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private String summarizeRhymeScheme(List<Triple<Integer, Double, Constraint<NoteLyric>>> segmentStructure) {
			StringBuilder rhymeSchemeBuilder = new StringBuilder();
			
			Map<Integer,Map<Double,Integer>> rhymeGroupByMeasureOffset = new HashMap<Integer,Map<Double,Integer>>();

			Integer phraseRhymeGroup;
			int nextRhymeGroup = 0;
			//TODO: prevent the consecutive rhyme from counting twice.
			for (Triple<Integer, Double, Constraint<NoteLyric>> offsetConstraint : segmentStructure) {
				Constraint<NoteLyric> constraint = offsetConstraint.getThird();
				ConstraintCondition<NoteLyric> constraintCondition = constraint.getCondition();
				if (!(constraintCondition instanceof Rhyme<?>)) {
					continue;
				}
				Rhyme<NoteLyric> condition = (Rhyme<NoteLyric>) constraintCondition;
				if (!condition.isPhraseEndingRhyme()) {
					continue;
				}
				int rhymeDefiningMeasure = condition.getReferenceMeasure();
				double rhymeDefiningOffset = condition.getReferenceOffset();
				Map<Double, Integer> rhymeGroupByOffset = rhymeGroupByMeasureOffset.get(rhymeDefiningMeasure);
				if (rhymeGroupByOffset == null) {
					rhymeGroupByOffset = new HashMap<Double,Integer>();
					rhymeGroupByMeasureOffset.put(rhymeDefiningMeasure, rhymeGroupByOffset);
				}
				phraseRhymeGroup = rhymeGroupByOffset.get(rhymeDefiningOffset);
				if (phraseRhymeGroup == null) {
					phraseRhymeGroup = nextRhymeGroup;
					rhymeGroupByOffset.put(rhymeDefiningOffset,phraseRhymeGroup);
					nextRhymeGroup++;
				}
				rhymeSchemeBuilder.append((char) ('A' + phraseRhymeGroup));
			}
			
			return rhymeSchemeBuilder.toString();
		}
	}

	DistributionalSegmentStructureEngineerMusicXMLModel model;
	
	public DistributionalSegmentStructureEngineer() {
		model = (DistributionalSegmentStructureEngineerMusicXMLModel) MusicXMLModelLearner.getTrainedModel(this.getClass());
	}
	
	@Override
	public SegmentStructure defineSegmentStructure(SegmentType segmentType) {
		int measureCount = model.sampleMeasureCountForSegmentType(segmentType);
		
		SegmentStructure segmentStructure = new SegmentStructure(measureCount, segmentType);
		segmentStructure.addDivisionsPerQuarterNote(0,4);
		segmentStructure.addKey(0,0,KeyMode.MAJOR);
		segmentStructure.addTime(0,4,4);
		
		List<Triple<Integer, Double, Constraint<NoteLyric>>> constraints = model.sampleConstraints(measureCount);
		for (Triple<Integer, Double, Constraint<NoteLyric>> triple : constraints) {
			segmentStructure.addConstraint(triple.getFirst(), triple.getSecond(), triple.getThird());
		}
		
		return segmentStructure;
	}

	@Override
	public List<Measure> instantiateSegmentStructure(SegmentType segmentType, SegmentStructure segmentStructure,
			boolean lastOfKind, boolean lastSegment) {
		return instantiateExactSegmentStructure(segmentType, segmentStructure, lastOfKind, lastSegment);
	}
}
