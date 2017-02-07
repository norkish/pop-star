package melody;

import java.util.ArrayList;
import java.util.List;

import composition.Score;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteTie;
import inspiration.Inspiration;
import utils.Triple;

public abstract class MelodyEngineer {

	// Legacy Code
//	private RhythmEngineer rhythmEngineer = null;
//	private PitchEngineer pitchEngineer = null;
//	
//	public void setRhythmEngineer(RhythmEngineer rhythmEngineer) {
//		this.rhythmEngineer = rhythmEngineer;
//	}
//
//	public void setPitchEngineer(PitchEngineer pitchEngineer) {
//		this.pitchEngineer = pitchEngineer;
//	}

	public abstract void addMelody(Inspiration inspiration, Score score);
	
	public static List<Note> createTiedNoteWithDuration(int durationInDivisions, int pitch, int divsPerQuarter) {
		List<Note> tiedNotes = new ArrayList<Note>();
		int divsLeftToAdd = durationInDivisions;
		while (divsLeftToAdd > 0) {
			Triple<Integer,Integer,Integer> biggestUntiedNote = getLongestIntegralNoteWithDurationLessThan(divsLeftToAdd,divsPerQuarter);
			int divsForNote = biggestUntiedNote.getFirst();
			int noteType = biggestUntiedNote.getSecond();
			int dots = biggestUntiedNote.getThird(); 
			Note newNote = new Note(pitch, divsForNote, noteType, null, true, dots, NoteTie.NONE, NoteTie.NONE, null, false);
			tiedNotes.add(newNote);
			divsLeftToAdd -= divsForNote;
		}
		// do we need a tie or is there just one note?
		if (tiedNotes.size() > 1) {
			tiedNotes.get(0).tie = NoteTie.START;
			tiedNotes.get(tiedNotes.size()-1).tie = NoteTie.STOP;
		}
		return tiedNotes;
	}

	private static Triple<Integer, Integer, Integer> getLongestIntegralNoteWithDurationLessThan(int divs,
			int divsPerQuarter) {
		int divsConsumed = 1;
		assert(divsPerQuarter == 1 || divsPerQuarter == 2 || divsPerQuarter == 4);
		int noteType = 4 * divsPerQuarter;
		int dots = 0;
		
		while (noteType != 1) {
			if (divsConsumed *2 > divs) {
				break;
			}
			divsConsumed *= 2;
			noteType /= 2;
		}
		
		int divsToAddFromNextDot = divsConsumed / 2;
		while (divsConsumed != divs && divsToAddFromNextDot > 0) {
			if (divsConsumed + divsToAddFromNextDot > divs) {
				break;
			}
			divsConsumed += divsToAddFromNextDot;
			divsToAddFromNextDot /= 2;
			dots++;
		}
		
		return new Triple<Integer,Integer, Integer>(divsConsumed, noteType, dots);
	}
	
	public static void main(String[] args) {
		MelodyEngineer e = new RandomMelodyEngineer();
		// Method testing
		List<Note> notes = e.createTiedNoteWithDuration(5, -1, 2);
		for (Note note : notes) {
			String dots = ""; 
			for (int i = 0; i < note.dots; i++) {
				dots += "•";
			}
			System.out.println(note.pitch + " " + note.type + dots + " " + note.duration + " " + note.tie);
		}
	}
}
