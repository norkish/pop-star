package melody;

import composition.Score;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteTie;
import inspiration.Inspiration;


public class TestMelodyEngineer extends MelodyEngineer {

	@Override
	public void addMelody(Inspiration inspiration, Score score) {
		//Note(int pitch, int duration, int type, NoteLyric lyric, int dots, NoteTie tie, 
		// 			NoteTimeModification timeModification, boolean isChordWithPreviousNote)

		// we're doing two divisions per quarter note–thus duration references the number of eighth notes
		score.addNote(0, 2.0, new Note(67, 2, 4, null, 0, NoteTie.NONE, null, false));//it's
		score.addNote(1, 0.0, new Note(67, 4, 2, null, 0, NoteTie.NONE, null, false));// nine
		score.addNote(1, 2.0, new Note(67, 2, 4, null, 0, NoteTie.NONE, null, false));//o'
		score.addNote(2, 0.0, new Note(67, 2, 4, null, 0, NoteTie.NONE, null, false));//clock
		score.addNote(2, 1.0, new Note(65, 3, 4, null, 1, NoteTie.NONE, null, false));//on
		score.addNote(2, 2.5, new Note(64, 1, 8, null, 0, NoteTie.NONE, null, false));//a
		score.addNote(3, 0.0, new Note(65, 1, 8, null, 0, NoteTie.NONE, null, false));//sat-
		score.addNote(3, 0.5, new Note(64, 1, 8, null, 0, NoteTie.NONE, null, false));//ur-
		score.addNote(3, 1.0, new Note(60, 4, 2, null, 0, NoteTie.NONE, null, false));//day
		score.addNote(4, 0.0, new Note(-1, 4, 2, null, 0, NoteTie.NONE, null, false));//rest
		score.addNote(4, 2.0, new Note(60, 2, 4, null, 0, NoteTie.NONE, null, false));//the
		score.addNote(5, 0.0, new Note(60, 3, 4, null, 1, NoteTie.NONE, null, false));//reg-
		score.addNote(5, 1.5, new Note(60, 1, 8, null, 0, NoteTie.NONE, null, false));//u-
		score.addNote(5, 2.0, new Note(60, 2, 4, null, 0, NoteTie.NONE, null, false));//lar
		score.addNote(6, 0.0, new Note(60, 4, 2, null, 0, NoteTie.NONE, null, false));//crowd
		score.addNote(6, 2.0, new Note(60, 1, 8, null, 0, NoteTie.NONE, null, false));//shuf-
		score.addNote(6, 2.5, new Note(62, 1, 8, null, 0, NoteTie.NONE, null, false));//fles
		score.addNote(7, 0.0, new Note(62, 6, 2, null, 1, NoteTie.NONE, null, false));//in
		score.addNote(8, 0.0, new Note(-1, 4, 2, null, 0, NoteTie.NONE, null, false));//rest
		score.addNote(8, 2.0, new Note(67, 1, 8, null, 0, NoteTie.NONE, null, false));//There's
		score.addNote(8, 2.5, new Note(67, 1, 8, null, 0, NoteTie.NONE, null, false));//an
		score.addNote(9, 0.0, new Note(67, 2, 4, null, 0, NoteTie.NONE, null, false));//old
		score.addNote(9, 1.0, new Note(67, 4, 2, null, 0, NoteTie.START, null, false));//man
		score.addNote(10, 0.0, new Note(67, 4, 2, null, 0, NoteTie.STOP, null, false));//-
		score.addNote(10, 2.0, new Note(65, 1, 8, null, 0, NoteTie.NONE, null, false));//sit-
		score.addNote(10, 2.5, new Note(64, 1, 8, null, 0, NoteTie.NONE, null, false));//ting
		score.addNote(11, 0.0, new Note(65, 1, 8, null, 0, NoteTie.NONE, null, false));//next
		score.addNote(11, 0.5, new Note(64, 1, 8, null, 0, NoteTie.NONE, null, false));//to
		score.addNote(11, 1.0, new Note(60, 4, 2, null, 0, NoteTie.NONE, null, false));//me
		score.addNote(12, 0.0, new Note(-1, 4, 2, null, 0, NoteTie.NONE, null, false));//rest
		score.addNote(12, 2.0, new Note(60, 1, 8, null, 0, NoteTie.NONE, null, false));//mak-
		score.addNote(12, 2.5, new Note(60, 1, 8, null, 0, NoteTie.NONE, null, false));//ing
		score.addNote(13, 0.0, new Note(57, 3, 4, null, 1, NoteTie.NONE, null, false));//love
		score.addNote(13, 1.5, new Note(65, 1, 8, null, 0, NoteTie.NONE, null, false));//to
		score.addNote(13, 2.0, new Note(65, 2, 4, null, 0, NoteTie.NONE, null, false));//his
		score.addNote(14, 0.0, new Note(65, 2, 4, null, 0, NoteTie.NONE, null, false));//ton-
		score.addNote(14, 1.0, new Note(64, 2, 4, null, 0, NoteTie.NONE, null, false));//ic
		score.addNote(14, 2.0, new Note(60, 2, 4, null, 0, NoteTie.NONE, null, false));//and
		score.addNote(15, 0.0, new Note(60, 6, 2, null, 1, NoteTie.NONE, null, false));//gin
		score.addNote(16, 0.0, new Note(-1, 6, 2, null, 1, NoteTie.NONE, null, false));//rest
	}

}
