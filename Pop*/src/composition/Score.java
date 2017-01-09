package composition;

import java.util.ArrayList;
import java.util.List;

import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.Time;

public class Score {

	List<Measure> measures = new ArrayList<Measure>();
	private boolean hasOrchestration = false;

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

	public String partToXML(int indentationLevel, char part) {
		StringBuilder str = new StringBuilder();
		
		int currDivisions = -1;
		Key currKey = null;
		Time currTime = null;
		
		for (int i = 0; i < measures.size(); i++) {
			final Measure measure = measures.get(i);
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("<measure number=\"").append(i).append("\">\n");
			
			// if first bar
			// TODO: add barline 
			/*<barline location="left">
            <bar-style>heavy-light</bar-style>
            <repeat direction="forward"/>
            </barline>*/
			
			//if attributes have changed
			boolean divChange = (currDivisions != measure.divisionsPerQuarterNote && measure.divisionsPerQuarterNote != -1);
			boolean keyChange = (currKey != measure.key && measure.key != null);
			boolean timeChange = (currTime != measure.time && measure.time != null);
			
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
					str.append("<fifths>").append(currKey.fifths).append("</fifths>\n");
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
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</attributes>\n");
			}
			
			switch(part) {
			case 'l':
				str.append(measure.leadToXML(indentationLevel+1));
				break;
				default:
				str.append(measure.orchestrationToXML(indentationLevel+1,part));
				break;
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
}
