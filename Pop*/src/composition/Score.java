package composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.Time;
import globalstructure.SegmentType;
import utils.Pair;
import utils.Utils;

public class Score {

	List<Measure> measures = new ArrayList<Measure>();
	private boolean hasOrchestration = false;
	private int transpose = 0;

	public void addMeasures(List<Measure> instantiatedMeasures) {
		measures.addAll(instantiatedMeasures);
	}

	public int length() {
		return measures.size();
	}

	public void addHarmony(int measureNumber, double offset, Harmony harmony) {
		measures.get(measureNumber).addHarmony(offset,harmony);
	}

	public void addNote(int measureNumber, double offset, Note note) {
		measures.get(measureNumber).addNote(offset,note);
	}

	public List<Measure> getMeasures() {
		return measures;
	}

	final static int measuresPerSystem = 3;
	public String partToXML(int indentationLevel, char part, int systemsPerPage) {
		StringBuilder str = new StringBuilder();
		
		int currDivisions = -1;
		Key currKey = null;
		Time currTime = null;
		SegmentType type = null;
		final int measuresPerPage = systemsPerPage*measuresPerSystem;
		
		for (int i = 0; i < measures.size(); i++) {
			final Measure measure = measures.get(i);
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("<measure number=\"").append(i).append("\">\n");
			
			if (part == 'l' && i % measuresPerSystem == 0) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				boolean newPage = (i == measuresPerPage) || (i > 2*measuresPerPage && (i - measuresPerPage) % (systemsPerPage+1) == 0);
				str.append("<print").append(i==0?"":" new-" + (newPage?"page":"system") + "=\"yes\"").append(">\n");
				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<system-layout>\n");
				for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
				str.append("<system-margins>\n");
				for (int j = 0; j <= indentationLevel+3; j++) str.append("    "); 
				str.append("<left-margin>0</left-margin>\n");
				for (int j = 0; j <= indentationLevel+3; j++) str.append("    "); 
				str.append("<right-margin>0</right-margin>\n");
				for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
				str.append("</system-margins>\n");
				for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
				if (i==0 || newPage) {
					str.append("<top-system-distance>").append(i==0?400:100).append("</top-system-distance>\n");
				} else {
					str.append("<system-distance>").append(hasOrchestration()?200:170).append("</system-distance>\n");
				}
				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("</system-layout>\n");
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</print>\n");
			}
			
			
			//if attributes have changed
			boolean divChange = (currDivisions != measure.divisionsPerQuarterNote && measure.divisionsPerQuarterNote != -1);
			boolean keyChange = ((currKey == null && measure.key != null) || (currKey != null) && !currKey.equals(measure.key));
			boolean timeChange = ((currTime == null && measure.time != null) || (currTime != null) && !currTime.equals(measure.time));
			boolean segmentTypeChange = type != measure.segmentType;
			
			if (divChange || keyChange || timeChange) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<attributes>\n");
				
				if (divChange) {
					currDivisions = measure.divisionsPerQuarterNote;
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<divisions>").append(currDivisions).append("</divisions>\n");
				}
				if (keyChange) {
					currKey = measure.key;
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<key>\n");
					for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
					str.append("<fifths>").append(((currKey.fifths + (transpose*7) + 6) %12) - 6).append("</fifths>\n");
					for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
					str.append("<mode>").append(currKey.mode.toString().toLowerCase()).append("</mode>\n");
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("</key>\n");
				}
				if (timeChange) {
					currTime = measure.time;
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<time>\n");
					for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
					str.append("<beats>").append(currTime.beats).append("</beats>\n");
					for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
					str.append("<beat-type>").append(currTime.beatType).append("</beat-type>\n");
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("</time>\n");
				}
				
				if (i == 0) {
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<clef>\n");
					char sign = part == 'b'? 'F' : 'G';
					int line = part == 'b'? 4 : 2;
					for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
					str.append("<sign>").append(sign).append("</sign>\n");
					for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
					str.append("<line>").append(line).append("</line>\n");
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("</clef>\n");
				}
				
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</attributes>\n");
			}
			
			if (segmentTypeChange) {
				type = measure.segmentType;
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<barline location=\"left\">\n");
				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<bar-style>").append(i == 0?"heavy-light":"heavy-heavy").append("</bar-style>\n");
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</barline>\n");
				if (part == 'l') {
					for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
					str.append("<direction placement=\"above\">\n");
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<direction-type>\n");
					for (int j = 0; j <= indentationLevel+2; j++) str.append("    "); 
					str.append("<words default-y=\"50\" font-size=\"16\" font-weight=\"bold\" color=\"red\" font-style=\"italic\">").append(StringUtils.capitalize(type.toString().toLowerCase())).append("</words>\n");
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("</direction-type>\n");
					for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
					str.append("</direction>\n");
				}
			}
			
			switch(part) {
			case 'l':
				str.append(measure.leadToXML(indentationLevel+1, transpose));
				break;
				default:
				str.append(measure.orchestrationToXML(indentationLevel+1,part, transpose));
				break;
			}
			
			if (i == measures.size()-1) {
				str.append("<barline location=\"right\">\n");
				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<bar-style>light-heavy</bar-style>\n");
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</barline>\n");
			}
			
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("</measure>\n");
		}
		
		return str.toString();
	}

	public boolean hasOrchestration() {
		return hasOrchestration ;
	}

	public void hasOrchestration(boolean b) {
		hasOrchestration = b;
	}

	public List<Pair<SegmentType, Integer>> getSegmentsLengths() {
		List<Pair<SegmentType, Integer>> segmentsLengths = new ArrayList<Pair<SegmentType, Integer>>();
		
		SegmentType prevType = null;
		int length = 0;
		for (Measure measure : measures) {
			if (measure.segmentType != prevType) {
				if (prevType != null) {
					segmentsLengths.add(new Pair<SegmentType, Integer>(prevType,length));
					length = 0;
				}
				prevType = measure.segmentType;
			}
			length++;
		}
		if (prevType != null) {
			segmentsLengths.add(new Pair<SegmentType, Integer>(prevType,length));
		}
		
		return segmentsLengths;
	}

	public Map<SegmentType, Time> getSegmentTimeSignatures() {
		Map<SegmentType, Time> segmentTimeSigs = new HashMap<SegmentType, Time>();
		
		for (Measure measure : measures) {
			if (!segmentTimeSigs.containsKey(measure.segmentType)) {
				segmentTimeSigs.put(measure.segmentType, measure.time);
			}
		}
		
		return segmentTimeSigs;
	}

	public Harmony getHarmonyPlayingAt(int measure, double beatsOffset) {
		Measure currMeasure = measures.get(measure);
		
		TreeMap<Double, Harmony> harmoniesForMeasure = currMeasure.getHarmonies();
		Harmony harmony = Utils.valueForKeyBeforeOrEqualTo(beatsOffset, harmoniesForMeasure);
		if (harmony != null || measure == 0)
			return harmony;
		
		do {
			measure--;
			currMeasure = measures.get(measure);
			harmoniesForMeasure = currMeasure.getHarmonies();
			if (!harmoniesForMeasure.isEmpty())
				harmony = harmoniesForMeasure.lastEntry().getValue();
		} while (harmony == null && measure > 0);
		
		return harmony;
	}

	public void deleteOrchestration() {
		for (Measure measure : measures) {
			measure.deleteOrchestration();
		}
		
		hasOrchestration = false;
	}

	public void transpose(int suggestedTransposition) {
		this.transpose = suggestedTransposition;
	}	
}
