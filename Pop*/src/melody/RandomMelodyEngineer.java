package melody;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import composition.Measure;
import composition.Score;
import config.SongConfiguration;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.Time;
import globalstructure.SegmentType;
import inspiration.Inspiration;

public class RandomMelodyEngineer extends MelodyEngineer {

	Random rand = new Random(SongConfiguration.randSeed);
	
	@Override
	public void addMelody(Inspiration inspiration, Score score) {
		List<Measure> measures = score.getMeasures();
		
		int verseStartMeasure = -1;
		int verseMeasureCounter = -1;
		
		int chorusStartMeasure = -1;
		int chorusMeasureCounter = -1;
		
		SegmentType prevType = null;
		
		for (int currMeasureNumber = 0; currMeasureNumber < measures.size(); currMeasureNumber++) {
			Measure measure = measures.get(currMeasureNumber);
			
			boolean generateMelody = true;
			switch (measure.segmentType) {
			case INTERLUDE:
			case INTRO:
			case OUTRO:
				break; // generate new melody
			case CHORUS:
				if (prevType != SegmentType.CHORUS) {
					// beginning of chorus
					if (chorusStartMeasure == -1) {
						// first chorus
						chorusStartMeasure = currMeasureNumber;
						break; // go on to generate lyrics
					} else {
						chorusMeasureCounter = 0;
					}
				}
				
				if (chorusMeasureCounter != -1) {
					// we're on a repeat of the chorus, need to go back and copy previous chorus lyrics
					TreeMap<Double, Note> otherNotes = measures.get(chorusStartMeasure+chorusMeasureCounter).getNotes();
					for (Double offset: otherNotes.keySet()) {
						Note otherNote = otherNotes.get(offset);
						measure.addNote(offset, new Note(otherNote));
					}
					
					chorusMeasureCounter++;
					generateMelody = false;
				}
				break;
			case VERSE:
				if (prevType != SegmentType.VERSE) {
					// beginning of chorus
					if (verseStartMeasure == -1) {
						// first chorus
						verseStartMeasure = currMeasureNumber;
						break; // go on to generate lyrics
					} else {
						// reset verse measure counter
						verseMeasureCounter = 0;
					}
				}
				
				if (verseMeasureCounter != -1) {
					// we're on a repeat of the chorus, need to go back and copy previous chorus lyrics
					TreeMap<Double, Note> otherNotes = measures.get(verseStartMeasure+verseMeasureCounter).getNotes();
					for (Double offset: otherNotes.keySet()) {
						Note otherNote = otherNotes.get(offset);
						measure.addNote(offset, new Note(otherNote));
					}
					
					verseMeasureCounter++;
					generateMelody = false;
				}
				break;
			default:
				break;
			}
			
			if (generateMelody) {
				Time currTime = measure.time;
				Key currKey = measure.key;
				int divisionsPerQuarterNote = measure.divisionsPerQuarterNote;
				
				int accumulativeDivisions = 0;
				final int divsPerQuarter = divisionsPerQuarterNote;
				final int totalMeasureDivisions = (int) (currTime.beats * divsPerQuarter * (4.0/currTime.beatType));
				
				while (accumulativeDivisions < totalMeasureDivisions) {
					int pitch = rand.nextInt(17) + 56;
					if (pitch == 56) pitch = Note.REST; // rest
					int divisionsToAdd =  rand.nextInt(totalMeasureDivisions-accumulativeDivisions) + 1;
					List<Note> notesToAdd = createTiedNoteWithDuration(divisionsToAdd, pitch, divsPerQuarter);
					for (Note note : notesToAdd) {
						measure.addNote(((double)accumulativeDivisions)/divsPerQuarter, note);
						accumulativeDivisions += note.duration;
					}
				}
			}
			prevType = measure.segmentType;
		}
	}
}
