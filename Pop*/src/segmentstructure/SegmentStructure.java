package segmentstructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import composition.Measure;
import constraint.Constraint;
import data.MusicXMLParser.KeyMode;
import globalstructure.SegmentType;

public class SegmentStructure implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public List<Measure> measures = new ArrayList<Measure>();

	public SegmentStructure(int measureCount, SegmentType segType) {
		for (int i = 0; i < measureCount; i++) {
			measures.add(new Measure(segType, i));
		}
	}

	public void addConstraint(int measureNumber, double offset, Constraint constraint) {
		Measure measure = measures.get(measureNumber);
		measure.addConstraint(offset, constraint);
	}

	public int getMeasureCount() {
		return measures.size();
	}

	public void addDivisionsPerQuarterNote(int measureNumber, int divisions) {
		measures.get(measureNumber).setDivions(divisions);
	}

	public void addKey(int measureNumber, int fifths, KeyMode mode) {
		measures.get(measureNumber).setKey(fifths, mode);
	}

	public void addTime(int measureNumber, int beats, int beatType) {
		measures.get(measureNumber).setTime(beats, beatType);
	}
}
