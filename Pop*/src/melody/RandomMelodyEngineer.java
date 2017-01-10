package melody;

import java.util.List;
import java.util.Random;

import composition.Measure;
import composition.Score;
import data.MusicXMLParser.Key;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.Time;
import inspiration.Inspiration;

public class RandomMelodyEngineer extends MelodyEngineer {

	Random rand = new Random();
	
	@Override
	public void addMelody(Inspiration inspiration, Score score) {
		
		for (Measure measure : score.getMeasures()) {
			Time currTime = measure.time;
			Key currKey = measure.key;
			int divisionsPerQuarterNote = measure.divisionsPerQuarterNote;
			
			int accumulativeDivisions = 0;
			final int divsPerQuarter = divisionsPerQuarterNote;
			final int totalMeasureDivisions = (int) (currTime.beats * divsPerQuarter * (4.0/currTime.beatType));
			
			while (accumulativeDivisions < totalMeasureDivisions) {
				int pitch = rand.nextInt(25) + 56;
				if (pitch == 56) pitch = -1; // rest
				int divisionsToAdd =  rand.nextInt(totalMeasureDivisions-accumulativeDivisions) + 1;
				List<Note> notesToAdd = createTiedNoteWithDuration(divisionsToAdd, pitch, divsPerQuarter);
				for (Note note : notesToAdd) {
					measure.addNote(((double)accumulativeDivisions)/divsPerQuarter, note);
					accumulativeDivisions += note.duration;
				}
			}
		}
	}

}
