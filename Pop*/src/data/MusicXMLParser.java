package data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import data.MusicXMLParser.DirectionType;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Note;
import data.MusicXMLParser.NoteLyric;
import data.MusicXMLParser.Barline.RepeatDirection;
import data.WikifoniaCorrection.CorrectionType;
import pitch.Pitch;
import syllabify.Syllabifier;
import tabcomplete.rhyme.Phonetecizer;
import tabcomplete.rhyme.StressedPhone;
import utils.Pair;
import utils.Triple;

public class MusicXMLParser {

	public enum DirectionType {
		DS_AL_CODA1, AL_CODA1, CODA1, DS_AL_CODA2, AL_CODA2, CODA2, SEGNO, IGNORE, DS_AL_FINE, FINE, DC_AL_FINE, DC_AL_CODA1;
		
		public static DirectionType parseDirectionType(Node node) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Text) continue;
				String childName = child.getNodeName();
				if (childName.equals("words")) {
					String words = child.getTextContent().toLowerCase().replaceAll("[^a-zA-Z0-9]", "").trim();
					if (words.equals("dsalcoda2")) { 
						return DirectionType.DS_AL_CODA2;
					} else if (words.equals("dsalcoda") || words.equals("dsalcoda1") || words.equals("dsacoda") 
							|| words.equals("solosaabcaftersolosdsalcoda")) {
						return DirectionType.DS_AL_CODA1;
					} else if (words.equals("dsalfine") || words.equals("solosonfbluesaftersolosdsalfine")) { 
						return DirectionType.DS_AL_FINE;
					} else if (words.equals("dcalfine") || words.equals("dcaalfine") || words.equals("dcalfinenorepeat")) { 
						return DirectionType.DC_AL_FINE;
					} else if (words.equals("dcalcoda") || words.equals("dcalcodawrepeat") ) { 
						return DirectionType.DC_AL_CODA1;
					} else if (words.equals("alcoda2") || words.equals("tocoda2")) {
						return DirectionType.AL_CODA2;
					} else if (words.equals("alcoda") || words.equals("tocoda") || 
							words.equals("alcoda1") || words.equals("tocoda1") || words.equals("zurcoda")) {
						return DirectionType.AL_CODA1;
					} else if (words.equals("fine") || words.equals("alfine")) {
						return DirectionType.FINE;
					} else if (words.equals("coda")) {
						return DirectionType.CODA1;
					} else if (words.equals("voicetacet")) {
						return DirectionType.IGNORE;
					} else if (words.equals("rubato")) { 
						return DirectionType.IGNORE;
					} else if ((words.contains("coda") || words.contains("fine")) && 
							!words.equals("repeat4timesthendsalfine") && !words.equals("4xfine") && !words.equals("lasttimetocoda")) { 
						throw new RuntimeException("catchall:" + words);
					} else {
						System.err.println("Unknown text content of words node in direction:" + words);
					}
				} else if (childName.equals("segno")) {
					return DirectionType.SEGNO;
				} else if (childName.equals("coda")) {
					return DirectionType.CODA1;
				} else if (childName.equals("bracket") || childName.equals("dynamics") 
						|| childName.equals("metronome")|| childName.equals("rehearsal")
						|| childName.equals("dashes")|| childName.equals("octave-shift")
						|| childName.equals("pedal")|| childName.equals("image")
						|| childName.equals("wedge") || childName.equals("other-direction")) {
//					return null;
				} else {
//					throw new RuntimeException("Unknown direction-type node:" + childName);
				}
			}
			
			return null;
		}

	}

	public static class Barline {
		public enum RepeatDirection {
			FORWARD, BACKWARD, NO_REPEAT;

			public static RepeatDirection parse(String direction) {
				if (direction.equals("forward")) {
					return FORWARD;
				} else if (direction.equals("backward")) {
					return BACKWARD;
				} else {
					throw new RuntimeException("Unhandled repeat direction:" + direction);
				}
			}

		}

		int[] numbers = null;
		RepeatDirection direction = RepeatDirection.NO_REPEAT;
		
		public void addRepeatDirection(RepeatDirection direction) {
			this.direction = direction;
		}
		
		public void addNumbers(int[] numbers) {
			this.numbers = numbers;
		}

		public RepeatDirection getRepeatDirection() {
			return direction;
		}

		public int[] getNumbers() {
			return numbers;
		}
		
	}

	public enum DegreeType {
		ADD, ALTER, SUBTRACT

	}

	public static class NoteTimeModification {

		final public int actualNotes;
		final public int normalNotes;
		final public int normalType;
		final public boolean normalDot;

		public NoteTimeModification(int actualNotes, int normalNotes, int normalType, boolean normalDot) {
			this.actualNotes = actualNotes;
			this.normalNotes = normalNotes;
			this.normalType = normalType;
			this.normalDot = normalDot;
		}
	}

	public enum NoteTie {
		NONE, START, STOP;

		public static NoteTie parse(String text) {
			if (text.equals("start")) {
				return START;
			} else if (text.equals("stop")) {
				return STOP;
			} else {
				throw new RuntimeException("Unknown NoteTie value:" + text);
			}
		}
	}

	public enum Syllabic {
		SINGLE, BEGIN, MIDDLE, END;

		public static Syllabic parse(String text) {
			if (text.equals("single")) {
				return SINGLE;
			} else if (text.equals("begin")) {
				return BEGIN;
			} else if (text.equals("middle")) {
				return MIDDLE;
			} else if (text.equals("end")) {
				return END;
			} else {
				throw new RuntimeException("Unknown syllabic value:" + text);
			}
		}

	}

	public static class NoteLyric {

		@Override
		public String toString() {
			return "NoteLyric [" + text + "]";
		}

		final public Syllabic syllabic;
		final public String text;
		final public boolean extend;
		final public boolean elision;
		public Triple<String, StressedPhone[], Integer> syllableStress;

		public NoteLyric(Syllabic syllabic, String text, boolean extend, boolean elision) {
			this.syllabic = syllabic;
			this.text = text;
			this.extend = extend;
			this.elision = elision;
		}

		public NoteLyric(NoteLyric other) {
			this.syllabic = other.syllabic;
			this.text = other.text;
			this.extend = other.extend;
			this.elision = other.elision;
			this.syllableStress = other.syllableStress;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (elision ? 1231 : 1237);
			result = prime * result + (extend ? 1231 : 1237);
			result = prime * result + ((syllabic == null) ? 0 : syllabic.hashCode());
			result = prime * result + ((text == null) ? 0 : text.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof NoteLyric))
				return false;
			NoteLyric other = (NoteLyric) obj;
			if (elision != other.elision)
				return false;
			if (extend != other.extend)
				return false;
			if (syllabic != other.syllabic)
				return false;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			return true;
		}

		public void addSyllableStress(Triple<String, StressedPhone[], Integer> triple) {
			this.syllableStress = triple;
		}
	}

	public static class Note {

		public static final int REST = -2;
		
		public int pitch;
		public int duration;
		public int type; // represents the denominator in the note (e.g., quarter = 1/4)
		private NoteLyric lyric;
		public boolean lyricVerseMatchesRepeatCount;
		public int dots;
		public NoteTie tie;
		public NoteTie slur;
		public NoteTimeModification timeModification;
		public boolean isChordWithPrevious;
		
		public Note(int pitch, int duration, int type, NoteLyric lyric, boolean lyricVerseMatchesRepeatCount, int dots, NoteTie tie, NoteTie slur, NoteTimeModification timeModification, boolean isChordWithPreviousNote) {
			this.pitch = pitch;
			this.duration = duration;
			this.type = type;
			this.lyric = lyric;
			this.lyricVerseMatchesRepeatCount = lyricVerseMatchesRepeatCount;
			this.dots = dots;
			this.tie = tie;
			this.slur = slur;
			this.timeModification = timeModification;
			this.isChordWithPrevious = isChordWithPreviousNote;
		}
		
		public Note(Note other) {
			this.pitch = other.pitch;
			this.duration = other.duration;
			this.type = other.type;
			this.lyric = other.lyric;
			this.dots = other.dots;
			this.tie = other.tie;
			this.slur = other.slur;
			this.timeModification = other.timeModification;
			this.isChordWithPrevious = other.isChordWithPrevious;
		}

		@Override
		public String toString() {
			return "Note [p=" + pitch + ", dur=" + duration + ", " + type + ", "
					+ (lyric != null ? "lyric=" + lyric + ", " : "") + "dots=" + dots + ", "
					+ (tie != null ? "tie=" + tie + ", " : "") + (slur != null ? "slur=" + slur + ", " : "")
					+ (timeModification != null ? "timeModification=" + timeModification + ", " : "")
					+ "isChordWithPrevious=" + isChordWithPrevious + "]";
		}

		public String toXML(int indentationLevel, boolean allowDownStem) {
			StringBuilder str = new StringBuilder();
			
			//open note tag
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("<note>\n");
			
			// chord
			if (isChordWithPrevious) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<chord/>\n");
			}
			
			int alter = 0;
			//pitch
			if (pitch == REST) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<rest/>\n");
			} else {
				String pitchString = Pitch.getPitchName((pitch+3)%12);
				char step = pitchString.charAt(0); 
				int octave = pitch/12;
				
				while(pitchString.length() > 1) {
					switch(pitchString.charAt(1)) {
					case 'b':
						alter--;
						break;
					case '#':
						alter++;
						break;
						default:
							throw new RuntimeException();
					}
					pitchString = pitchString.substring(1);
				}
				
				// step, alter, octave
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<pitch>\n");

				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<step>").append(step).append("</step>\n");
				
				if (alter != 0) {
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<alter>").append(alter).append("</alter>\n");
				}

				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<octave>").append(octave).append("</octave>\n");
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</pitch>\n");
			}

			//duration
			for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
			str.append("<duration>").append(duration).append("</duration>\n");
		
			//tie
			if (tie != NoteTie.NONE) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<tie type=\"").append(tie.toString().toLowerCase()).append("\"/>\n");
			}
		
			//type
			for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
			str.append("<type>").append(interpretNoteType(type)).append("</type>\n");
			
			//dot
			for (int i = 0; i < dots; i++) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<dot/>\n");
			}

			//accidental
			if(alter != 0) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<accidental>").append(alter == 1?"sharp":"flat").append("</accidental>\n");
			}
			
			//stem
			if (pitch != REST) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<stem>").append(pitch > 60 && allowDownStem?"down":"up").append("</stem>\n");
			}

			// notations
			if (tie != NoteTie.NONE) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<notations>\n");
				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<tied type=\"").append(tie.toString().toLowerCase()).append("\"/>\n");
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</notations>\n");
			}
			
			//time modification (not sure of order here)
			
			//lyric
			if (lyric != null) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<lyric name=\"verse\" number=\"1\">\n");

				// syllabic, text
				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<syllabic>").append(lyric.syllabic.toString().toLowerCase()).append("</syllabic>\n");
				
				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<text>").append(lyric.text).append("</text>\n");
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</lyric>\n");
			}
			
			//close note tag
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("</note>\n");
			
			return str.toString();
		}

		/**
		 * is this note a played note (not sustained from being tied to previous note or a rest)
		 * @return
		 */
		public boolean isPlayedNoteOnset() {
			if (pitch == REST)
				return false;
			if (tie == NoteTie.STOP)
				return false;
			return true;
		}

		public NoteLyric getLyric(boolean requireLyricVerseMatchesRepeatCount) {
			if (requireLyricVerseMatchesRepeatCount && !lyricVerseMatchesRepeatCount)
				return null;
			else {
				return lyric;
			}
		}

		public void setLyric(NoteLyric lyric, boolean lyricVerseMatchesRepeatCount) {
			this.lyric = lyric;
			this.lyricVerseMatchesRepeatCount = lyricVerseMatchesRepeatCount;
		}
		
	}

	
	public static class Quality {
		private static final int FLAT_SECONDi = 0, SECONDi = 1, FLAT_THIRDi = 2, THIRDi = 3, FOURTHi = 4, FLAT_FIFTHi = 5,
				FIFTHi = 6, AUGMENTED_FIFTHi = 7, SIXTHi = 8, FLAT_SEVENTHi = 9, SEVENTHi = 10, FLAT_NINTHi = 11, 
				NINTHi = 12, SHARP_NINTHi = 13, FLAT_ELEVENTHi = 14, ELEVENTHi = 15, SHARP_ELEVENTHi = 16, 
				FLAT_THIRTEENTHi = 17, THIRTEENTHi = 18, SHARP_THIRTEENTHi = 19;
		// this needs to reflect the intervals of the above constants
		public static final int[] HARMONY_CONSTANT_INTERVALS = new int[]{1,2,3,4,5,6,7,8,9,10,11,13,14,15,17,18,19,20,21,22};

		private static final String MAJOR = "major", MINOR = "minor", MINOR_MAJOR = "major-minor", 
				DOMINANT = "dominant", HALF_DIMINISHED = "half-diminished", DIMINISHED = "diminished", AUGMENTED = "augmented", SUSPENDED = "suspended",
				POWER = "power", NO_CHORD = "none", PEDAL = "pedal";
		
		private static final String SECOND = "second", FOURTH = "fourth", SIXTH = "sixth", SEVENTH = "seventh", NINTH = "ninth", 
				ELEVENTH = "eleventh", THIRTEENTH = "thirteenth";
		
		boolean[] notesOn = new boolean[SHARP_THIRTEENTHi+1];
		// as per https://en.wikipedia.org/wiki/Chord_names_and_symbols_(popular_music)
		String kind = MAJOR;
		String kindInterval = null;
		DegreeType degreeType = null;
		int degreeAlter = 0;
		int degreeValue = 0;
		
		public Quality() {
			notesOn[THIRDi] = true;
			notesOn[FIFTHi] = true;
		}
		
		public boolean parseKindTextAttribute(Node node) {
			if (node == null) {
				return false;
			} else {
				String text = node.getTextContent().trim();
				if (text.isEmpty() || text.equals("Maj") || text.equals("ma")) {
					kind = MAJOR;
					// do nothing
				} else if (text.equals("°") || text.equals("dim")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					notesOn[FIFTHi] = false;
					notesOn[FLAT_FIFTHi] = true;
					kind = DIMINISHED;
				} else if (text.equals("5b")) {
					notesOn[FIFTHi] = false;
					notesOn[FLAT_FIFTHi] = true;
					degreeValue = 5;
					degreeAlter = -1;
					degreeType = DegreeType.ALTER;
				} else if (text.equals("m")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					kind = MINOR;
				} else if (text.equals("7")) {
					notesOn[FLAT_SEVENTHi] = true;
					kind = DOMINANT;
				} else if (text.equals("m7")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					kind = MINOR;
					kindInterval = SEVENTH;
				} else if (text.equals("sus7")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = false;
					notesOn[FOURTHi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					kind = SUSPENDED;
					kindInterval = SEVENTH;
				} else if (text.matches("^[A-G].*")) {
					// ignore it
				} else { 
					throw new RuntimeException("TEXT ATTR:" + text);
				}
			}
			return true;
		}

		public boolean parseKindContentText(String text) {
			if (text.equals("6"))
				text = "major-sixth";
			else if (text.equals("7"))
				text = "dominant-seventh";
			else if (text.equals("9"))
				text = "dominant-ninth";
			else if (text.equals("minMaj7"))
				text = "minor-major";
			else if (text.equals("min"))
				text = "minor";
			else if (text.equals("min/G"))
				text = "minor";
			else if (text.equals("maj7"))
				text = "major-seventh";
			else if (text.equals("maj9"))
				text = "major-ninth";
			else if (text.equals("min7"))
				text = "minor-seventh";
			else if (text.equals("min9"))
				text = "minor-ninth";
			else if (text.equals("dim7"))
				text = "diminished-seventh";
			else if (text.equals("min6"))
				text = "minor-sixth";
			else if (text.equals("maj69"))
				text = "major-sixnine";
			else if (text.equals("dim"))
				text = "diminished";
			else if (text.equals("aug"))
				text = "augmented";
			else if (text.equals("m7b5"))
				text = "half-diminished";
			else if (text.equals("sus47") || text.equals("7sus"))
				text = "suspended-fourseven";
			
			if (text.startsWith("minor")) {
				notesOn[THIRDi] = false;
				notesOn[FLAT_THIRDi] = true;
				kind = MINOR;
				text = text.substring(5);
				if (text.isEmpty()) {
					return true;
				} else if (text.equals("-major")) {
					notesOn[SEVENTHi] = true;
					kind = MINOR_MAJOR;
				} else if (text.equals("-seventh")) {
					notesOn[FLAT_SEVENTHi] = true;
					kindInterval = SEVENTH;
				} else if (text.equals("-sixth")) {
					notesOn[SIXTHi] = true;
					kindInterval = SIXTH;
				} else if (text.equals("-ninth")){
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[NINTHi] = true;
					kindInterval = NINTH;
				} else if (text.equals("-11th")){
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					kindInterval = ELEVENTH;
				} else if (text.equals("-13th")){
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					notesOn[THIRTEENTHi] = true;
					kindInterval = THIRTEENTH;
				} else {
					throw new RuntimeException("Unknown chord: \"minor" + text + "\"");
				}
			} else if (text.startsWith("major")) {
				text = text.substring(5);
				if (text.isEmpty()) {
					kind = MAJOR;
					return true;
				} else if (text.equals("-minor")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					notesOn[SEVENTHi] = true;
					kind = MINOR_MAJOR;
					kindInterval = SEVENTH;
				} else if (text.equals("-sixth")) {
					notesOn[SIXTHi] = true;
					kind = MAJOR;
					kindInterval = SIXTH;
				} else if (text.equals("-sixnine")) {
					notesOn[SIXTHi] = true;
					notesOn[NINTHi] = true;
					kind = MAJOR;
					kindInterval = SIXTH;
					// add flat nine
					degreeValue = 9;
					degreeAlter = -1;
					degreeType = DegreeType.ADD;
				} else if (text.equals("-seventh")) {
					notesOn[SEVENTHi] = true;
					kind = MAJOR;
					kindInterval = SEVENTH;
				} else if (text.equals("-ninth")) {
					notesOn[SEVENTHi] = true;
					notesOn[NINTHi] = true;
					kind = MAJOR;
					kindInterval = NINTH;
				} else if (text.equals("-11th")) {
					notesOn[SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					kind = MAJOR;
					kindInterval = ELEVENTH;
				} else if (text.equals("-13th")) {
					notesOn[SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					notesOn[THIRTEENTHi] = true;
					kind = MAJOR;
					kindInterval = THIRTEENTH;
				} else {
					throw new RuntimeException("Unknown chord: \"major" + text + "\"");
				}
			} else if (text.startsWith("dominant")) {
				notesOn[FLAT_SEVENTHi] = true;
				text = text.substring(8);
				kind = DOMINANT;
				if (text.equals("") || text.equals("-seventh")) {
					return true;
				} else if (text.equals("-ninth")) {
					kindInterval = NINTH;
					notesOn[NINTHi] = true;
				} else if (text.equals("-11th")) {
					kindInterval = ELEVENTH;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
				} else if (text.equals("-13th")) {
					kindInterval = THIRTEENTH;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					notesOn[THIRTEENTHi] = true;
				} else {
					throw new RuntimeException("Unknown chord: dominant" + text);
				}
			} else if (text.startsWith("augmented")) {
				text = text.substring(9);
				kind = AUGMENTED;
				if (text.isEmpty()) {
					return true;
				} else if (text.equals("-seventh")) {
					notesOn[FIFTHi] = false;
					notesOn[AUGMENTED_FIFTHi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					kindInterval = SEVENTH;
				} else if (text.equals("-ninth")) {
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[SHARP_NINTHi] = true;
					kindInterval = NINTH;
				} else {
					throw new RuntimeException("Unknown chord: augmented" + text);
				}
			} else if (text.startsWith("suspended")) {
				notesOn[THIRDi] = false;
				notesOn[FLAT_THIRDi] = false;
				kind = SUSPENDED;
				text = text.substring(9);
				if (text.equals("-fourth")) {
					notesOn[FOURTHi] = true;
					kindInterval = FOURTH;
				} else if (text.equals("-second")) {
					notesOn[SECONDi] = true;
					kindInterval = SECOND;
				} else if (text.equals("-fourseven")) {
					notesOn[FOURTHi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					kindInterval = FOURTH;
					// add flat seventh
					degreeValue = 7;
					degreeAlter = -1;
					degreeType = DegreeType.ADD;
				} else {
					throw new RuntimeException("Unknown chord: dominant" + text);
				}
			} else if (text.startsWith("diminished")) {
				notesOn[THIRDi] = false;
				notesOn[FLAT_THIRDi] = true;
				notesOn[FIFTHi] = false;
				notesOn[FLAT_FIFTHi] = true;
				kind = DIMINISHED;
				text = text.substring(10);
				if (text.isEmpty()) {
					return true;
				} else if (text.equals("-seventh")) {
					notesOn[SIXTHi] = true;
					kindInterval = SEVENTH;
				} else {
					throw new RuntimeException("Unknown chord: diminished" + text);
				}
			} else if (text.startsWith("half-diminished")) {
				notesOn[THIRDi] = false;
				notesOn[FLAT_THIRDi] = true;
				notesOn[FIFTHi] = false;
				notesOn[FLAT_FIFTHi] = true;
				notesOn[FLAT_SEVENTHi] = true;
				kind = HALF_DIMINISHED;
				text = text.substring(15);
				if (text.isEmpty()) {
					return true;
				} else {
					throw new RuntimeException("Unknown chord: half-diminished" + text);
				}
			} else if (text.startsWith("power")) {
				notesOn[THIRDi] = false;
				kind = POWER;
				text = text.substring(5);
				if (text.isEmpty()) {
					return true;
				} else {
					throw new RuntimeException("Unknown chord: power" + text);
				}
			} else if (text.equals("none")) {
				notesOn = null;
				kind = NO_CHORD;
			} else if (text.startsWith("/")) {
				// do nothing, quality does not parse bass notes
			} else if (text.equals("pedal")) {
				notesOn[THIRDi] = false;
				notesOn[FIFTHi] = false;
				kind = PEDAL;
			} else if (text.isEmpty() || text.equals("other")) {
				return false;
			} else {
				throw new RuntimeException("KIND CONTENT:" + text);
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + degreeAlter;
			result = prime * result + ((degreeType == null) ? 0 : degreeType.hashCode());
			result = prime * result + degreeValue;
			result = prime * result + ((kind == null) ? 0 : kind.hashCode());
			result = prime * result + ((kindInterval == null) ? 0 : kindInterval.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Quality other = (Quality) obj;
			if (degreeAlter != other.degreeAlter)
				return false;
			if (degreeType != other.degreeType)
				return false;
			if (degreeValue != other.degreeValue)
				return false;
			if (kind == null) {
				if (other.kind != null)
					return false;
			} else if (!kind.equals(other.kind))
				return false;
			if (kindInterval == null) {
				if (other.kindInterval != null)
					return false;
			} else if (!kindInterval.equals(other.kindInterval))
				return false;
			return true;
		}

		public void parseDegreeNode(Node node) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Text) continue;
				String childName = child.getNodeName();
				if (childName.equals("degree-value")) {
					degreeValue = Integer.parseInt(child.getTextContent());
				} else if (childName.equals("degree-alter")) {
					degreeAlter = Integer.parseInt(child.getTextContent());
				} else if (childName.equals("degree-type")) {
					String type = child.getTextContent().trim();
					if (type.equals("add")) {
						degreeType = DegreeType.ADD;
					} else if (type.equals("alter")) {
						degreeType = DegreeType.ALTER;
					} else if (type.equals("subtract")) {
						degreeType = DegreeType.SUBTRACT;
					} else {
						throw new RuntimeException("Unknown degree-type:" + type);
					}
				} else {
					MusicXMLSummaryGenerator.printNode(node, System.err);
					throw new RuntimeException("Unknown KeyMode:" + childName);
				}
			}
			
			switch (degreeType) {
			case ADD:
				notesOn[getNoteIndex(degreeValue,degreeAlter)] = true;
				break;
			case ALTER:
				notesOn[getNoteIndex(degreeValue,0)] = false;
				notesOn[getNoteIndex(degreeValue,degreeAlter)] = true;
				break;
			case SUBTRACT:
				if (!notesOn[getNoteIndex(degreeValue,degreeAlter)]) {
					throw new RuntimeException("Instruction to turn off note that's not on:" + getNoteIndex(degreeValue,degreeAlter));
				}
				notesOn[getNoteIndex(degreeValue,degreeAlter)] = false;
				break;
			}
		}

		private int getNoteIndex(int degreeValue, int degreeAlter) {
			int idx = getDegreeValueNoteIndex(degreeValue);
			
			if (degreeAlter < -1 || degreeAlter > 1) {
				throw new RuntimeException("Abrnomal degree alter:" + degreeAlter);
			}
			
			return idx + degreeAlter;
		}

		/**
		 * @param degreeValue
		 */
		private int getDegreeValueNoteIndex(int degreeValue) {
			switch(degreeValue) {
			case 2:
				return SECONDi;
			case 3:
				return THIRDi;
			case 4:
				return FOURTHi;
			case 5:
				return FIFTHi;
			case 7:
				return SEVENTHi;
			case 9:
				return NINTHi;
			case 11:
				return ELEVENTHi;
			case 13:
				return THIRTEENTHi;
			default:
				throw new RuntimeException("Unhandled degree value:" + degreeValue);
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			if (kind != null)
				builder.append(kind);
			if (kindInterval != null)
				builder.append(kindInterval);
			if (degreeType != null)
				builder.append(degreeType);
			if (degreeValue > 0)
				builder.append(degreeValue);
			return builder.toString();
		}

		public boolean[] getPitches() {
			return notesOn;
		}

		/**
		 * Given the scale defined by this quality, what step in the 8-note scale is halfStepsFromRoot?
		 * @param halfStepsFromRoot
		 * @return
		 */
		public int getScaleStep(int halfStepsFromRoot) {
			switch (halfStepsFromRoot % 12) {
			case 0:
				return 0;
			case 1:
			case 2:
				return 1;
			case 3:
				if (notesOn[SHARP_NINTHi]){ // if there's a sharp ninth, then three half steps could represent...
					return 1; //... the second step
				} else {
					return 2; // otherwise it's the minor third
				}
			case 4:
				if (notesOn[THIRDi]) {
					return 2; // major third
				} else {
					assert(notesOn[FLAT_ELEVENTHi]);
					return 3; // flat fourth
				}
			case 5:
				return 3;
			case 6: // tritone
				if (notesOn[FLAT_FIFTHi] && notesOn[FIFTHi]) { // fourth-step if both fifth and flat fifth on
					return 3; // sharp fourth step
				} else { // fifth step
					return 4; // flat fifth step
				}
			case 7:
				return 4; // perfect fifth
			case 8:
				if (notesOn[FLAT_THIRTEENTHi] || notesOn[AUGMENTED_FIFTHi] && notesOn[FIFTHi]) { // if fifth and flat sixth are on, then it's a flat sixth, not a sharp five
					return 5; // flat sixth
				} else {
					return 4; // augmented fifth
				}
			case 9:
				return 5;
			case 10:
				if (notesOn[FLAT_SEVENTHi]) { // if flat seventh 
					return 6; // then it's a seventh
				} else if (notesOn[SHARP_THIRTEENTHi]) { // sharp sixth
					return 5; // sixth
				} else {
					return 6;
				}
			case 11:
				return 6;
			}
			
			
			return 0;
		}
		
		public int getIntervalForScaleStep(int scaleStep) {
			switch (scaleStep) {
			case 0:
				return 0;
			case 1: // second
				if (notesOn[FLAT_SECONDi] || notesOn[FLAT_NINTHi]) {
					return HARMONY_CONSTANT_INTERVALS[FLAT_SECONDi];
				} else if (notesOn[SHARP_NINTHi]){
					return HARMONY_CONSTANT_INTERVALS[FLAT_THIRDi];
				} else {
					return HARMONY_CONSTANT_INTERVALS[SECONDi];
				}
			case 2: // third
				if (notesOn[THIRDi]) { // third should be on unless it was changed
					return HARMONY_CONSTANT_INTERVALS[THIRDi];
				} else {
					return HARMONY_CONSTANT_INTERVALS[FLAT_THIRDi];
				}
			case 3: // fourth
				if (notesOn[FLAT_ELEVENTHi]) {
					return HARMONY_CONSTANT_INTERVALS[THIRDi];
				} else if (notesOn[SHARP_ELEVENTHi]){
					return HARMONY_CONSTANT_INTERVALS[FLAT_FIFTHi];
				} else {
					return HARMONY_CONSTANT_INTERVALS[FOURTHi];
				}
			case 4: // fifth
				if (notesOn[FIFTHi]) { // fifth should be on unless it was changed
					return HARMONY_CONSTANT_INTERVALS[FIFTHi];
				} else if (notesOn[FLAT_FIFTHi]) {
					return HARMONY_CONSTANT_INTERVALS[FLAT_FIFTHi];
				} else {
					return HARMONY_CONSTANT_INTERVALS[AUGMENTED_FIFTHi];
				}
			case 5: // sixth
				if (notesOn[FLAT_THIRTEENTHi] || notesOn[AUGMENTED_FIFTHi] && notesOn[FIFTHi]) { // if fifth and flat sixth are on, then it's a flat sixth, not a sharp five
					return HARMONY_CONSTANT_INTERVALS[AUGMENTED_FIFTHi];
				} else if (notesOn[SHARP_THIRTEENTHi]){
					return HARMONY_CONSTANT_INTERVALS[FLAT_SEVENTHi];
				} else {
					return HARMONY_CONSTANT_INTERVALS[SIXTHi];
				}
			case 6: // seventh
				if (notesOn[FLAT_SEVENTHi]) {
					return HARMONY_CONSTANT_INTERVALS[FLAT_SEVENTHi];
				} else {
					return HARMONY_CONSTANT_INTERVALS[SEVENTHi];
				}
			}
			throw new RuntimeException("Illegal scale step:" + scaleStep);
		}
	}

	public static class Bass {
		public int bassStep;
		public Bass(int bassStep) {
			this.bassStep = bassStep;
		}
		@Override
		public String toString() {
			return Pitch.getPitchName(bassStep);
		}
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + bassStep;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Bass))
				return false;
			Bass other = (Bass) obj;
			if (bassStep != other.bassStep)
				return false;
			return true;
		}
		
	}

	public static class Harmony {
		public Root root;
		public Quality quality;
		public Bass bass;
		public Harmony(Root root, Quality quality, Bass bass) {
			this.root = root;
			this.quality = quality;
			this.bass = bass;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(root).append(quality);
			if (bass != null) {
				builder.append('/').append(bass);
			}
			return builder.toString();
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bass == null) ? 0 : bass.hashCode());
			result = prime * result + ((quality == null) ? 0 : quality.hashCode());
			result = prime * result + ((root == null) ? 0 : root.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Harmony))
				return false;
			Harmony other = (Harmony) obj;
			if (bass == null) {
				if (other.bass != null)
					return false;
			} else if (!bass.equals(other.bass))
				return false;
			if (quality == null) {
				if (other.quality != null)
					return false;
			} else if (!quality.equals(other.quality))
				return false;
			if (root == null) {
				if (other.root != null)
					return false;
			} else if (!root.equals(other.root))
				return false;
			return true;
		}

		public String toXML(int indentationLevel, int offset) {
			StringBuilder str = new StringBuilder();
			
			//open harmony tag
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("<harmony default-y=\"25\" font-size=\"12\" print-frame=\"no\" relative-x=\"8\">\n");
			
			//root
			if (root != null) {
				String rootString = Pitch.getPitchName(root.rootStep);
				char rootStep = rootString.charAt(0); 
				int rootAlter = 0;
				
				while(rootString.length() > 1) {
					switch(rootString.charAt(1)) {
					case 'b':
						rootAlter--;
						break;
					case '#':
						rootAlter++;
						break;
						default:
							throw new RuntimeException();
					}
					rootString = rootString.substring(1);
				}
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<root>\n");

				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<root-step>").append(rootStep).append("</root-step>\n");
				
				if (rootAlter != 0) {
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<root-alter>").append(rootAlter).append("</root-alter>\n");
				}
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</root>\n");
			}
			
			//kind
			if (quality != null) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<kind>").append(quality.kind);
				if (quality.kindInterval != null) str.append('-').append(quality.kindInterval);
				str.append("</kind>\n");
			}
			
			//bass
			if (bass != null) {
				String bassString = Pitch.getPitchName(bass.bassStep);
				char bassStep = bassString.charAt(0); 
				int bassAlter = 0;
				
				while(bassString.length() > 1) {
					switch(bassString.charAt(1)) {
					case 'b':
						bassAlter--;
						break;
					case '#':
						bassAlter++;
						break;
						default:
							throw new RuntimeException();
					}
					bassString = bassString.substring(1);
				}
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<bass>\n");

				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<bass-step>").append(bassStep).append("</bass-step>\n");
				
				if (bassAlter != 0) {
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<bass-alter>").append(bassAlter).append("</bass-alter>\n");
				}
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</bass>\n");
			}
			
			//degree
			if (quality.degreeType != null) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<degree>\n");

				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<degree-value>").append(quality.degreeValue).append("</degree-value>\n");
				
				if (quality.degreeAlter != 0) {
					for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
					str.append("<degree-alter>").append(quality.degreeAlter).append("</degree-alter>\n");
				}

				for (int j = 0; j <= indentationLevel+1; j++) str.append("    "); 
				str.append("<degree-type>").append(quality.degreeType).append("</degree-type>\n");
				
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("</degree>\n");
			}
			
			//offset
			if (offset != 0.0) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<offset>").append(offset).append("</offset>\n");
			}
			
			//close harmony tag
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("</harmony>\n");
			
			return str.toString();
		}

		/**
		 * returns the scale step (0-6) for the seven steps in the scale defined by this harmony's quality
		 * @param pitch
		 * @return
		 */
		public int getScaleStep(int pitch) {
			if (pitch < 0) return pitch;
			int halfStepsFromRoot = (pitch - (root.rootStep+9)) % 12;
			
			return quality.getScaleStep(halfStepsFromRoot);
		}
		
		public int getIntervalForScaleStep(int step) {
			return quality.getIntervalForScaleStep(step);
		}
	}

	public static class Root {
		//
		public int rootStep;
		public Root(int rootStep) {
			this.rootStep = rootStep;
		}
		
		@Override
		public String toString() {
			return Pitch.getPitchName(rootStep);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + rootStep;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Root))
				return false;
			Root other = (Root) obj;
			if (rootStep != other.rootStep)
				return false;
			return true;
		}
	}

	public static enum KeyMode {
		MAJOR, MINOR;
		
		public static KeyMode parseMode(String textContent) {
			textContent = textContent.toLowerCase();
			if (textContent.equals("major"))
				return MAJOR;
			else if (textContent.equals("minor"))
				return MINOR;
			else
				throw new RuntimeException("Unknown KeyMode:" + textContent); 
		}
	}

	public static class Key {
		public int fifths;
		public KeyMode mode;
		public Key(int fifths, KeyMode mode) {
			this.fifths = fifths;
			this.mode = mode;
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Key [fifths=").append(fifths).append(", ");
			if (mode != null)
				builder.append("mode=").append(mode);
			builder.append("]");
			return builder.toString();
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + fifths;
			result = prime * result + ((mode == null) ? 0 : mode.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Key))
				return false;
			Key other = (Key) obj;
			if (fifths != other.fifths)
				return false;
			if (mode != other.mode)
				return false;
			return true;
		}
		
	}

	public static class Time {
		
		public static final Time FOUR_FOUR = new Time(4,4);
		public static final Time TWO_TWO = new Time(2,2);
		
		public int beats;
		public int beatType;
		public Time(int beats, int beatType) {
			this.beats = beats;
			this.beatType = beatType;
		}
		
		public String toString() {
			return this.beats + "/" + this.beatType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + beatType;
			result = prime * result + beats;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Time))
				return false;
			Time other = (Time) obj;
			if (beatType != other.beatType)
				return false;
			if (beats != other.beats)
				return false;
			return true;
		}
	}

	Document xml;
	String filename;

	public MusicXMLParser(String filename, Document xml) {
		this.filename = filename;
		this.xml = xml;
	}

	public String toMelogenString() {
		StringBuilder str = new StringBuilder();

		ParsedMusicXMLObject musicXML = parse(true);
		if (musicXML == null) 
			return null;
			
		return str.toString();
	}

	// this set is to help verify the assertion that there is only one instrument per leadsheet
	private static Set<String> instruments = null;
	private static int unparseableHarmonies = 0;
	
	public ParsedMusicXMLObject parse(boolean followRepeats) {
		ParsedMusicXMLObject musicXML = new ParsedMusicXMLObject(filename, followRepeats);
		List<Node> measures = MusicXMLSummaryGenerator.getMeasuresForPart(this,0);

		// Harmony indexed by measure and by the offset (in divisions) from the beginning of the measure
		List<Triple<Integer,Integer, List<Harmony>>> harmonyByPlayedMeasure = new ArrayList<Triple<Integer,Integer,List<Harmony>>>();
		// notes indexed by measure and by the offset (in divisions) from the beginning of the measure
		// this data structure represents order as regards repeats, but has no (simple) way of looking at how many times a note has been repeated or what the lyric for the last repetition was
		List<Triple<Integer, Integer, Note>> notesByPlayedMeasure = new ArrayList<Triple<Integer, Integer, Note>>();
		// For each offset in each measure, we keep track of how many times we've visited a note at that offset in that measure and what the previous notelyric was for the note
		// note that this data structure has no sense of order as regards repeats, but can easily check the number of times it has been repeated and the previous notelyric
		Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo = new TreeMap<Integer, Map<Integer, Pair<Integer, NoteLyric>>>();
		
		instruments = new HashSet<String>();
		SortedMap<Integer,Time> timeByAbsoluteMeasure = new TreeMap<Integer,Time>(); 
		Time currTime = null;
		SortedMap<Integer,Key> keyByAbsoluteMeasure = new TreeMap<Integer,Key>(); 
		Key currKey = null;
		SortedMap<Integer,Integer> divsPerQuarterByAbsoluteMeasure = new TreeMap<Integer,Integer>(); 
		int currDivisionsPerQuarterNote = -1;
		Stack<Integer> forwardRepeatMeasures = new Stack<Integer>();
		int smallestNoteType = 0;
		int nextMeasure = -1;
		int prevPlayedMeasure = -1;
		int segno = -1;
		int coda1 = findDirectionTypeStartingFrom(measures, DirectionType.CODA1, measures.size());
		int coda2 = (coda1 == -1? -1 : findDirectionTypeStartingFrom(measures, DirectionType.CODA1, coda1));
		int dsalcoda = findDirectionTypeStartingFrom(measures, DirectionType.DS_AL_CODA1, measures.size());
		int dcalcoda = findDirectionTypeStartingFrom(measures, DirectionType.DC_AL_CODA1, measures.size());
		if (coda1 != -1 && dsalcoda == -1 && dcalcoda == -1) {
			throw new RuntimeException("Found coda at measure " + coda1 + ", but no D.S. or D.C. al Coda");
//			System.err.println("Found coda at measure " + coda1 + ", but no D.S. or D.C. al Coda. Assuming it directly precedes coda");
//			dsalcoda = coda1 - 1;
//			return null; // not gonna deal with it.
		}
		if (coda1 == -1 && (dsalcoda != -1 || dcalcoda != -1)) {
			throw new RuntimeException("Found ds or dc at measure " + (dsalcoda == -1 ? dcalcoda : dsalcoda) + ", but no coda");
//			System.err.println("Found ds or dc at measure " + (dsalcoda == -1 ? dcalcoda : dsalcoda) + ", but no coda. Assuming it directly follows ds");
//			coda1 = (dsalcoda == -1 ? dcalcoda : dsalcoda) + 1;
//			return null; // not gonna deal with it.
		}
		if (coda1 != -1 && (dsalcoda != -1 || dcalcoda != -1) && coda1 <= (dsalcoda == -1 ? dcalcoda : dsalcoda)) {
			throw new RuntimeException("coda ("+coda1+") precedes " + (dsalcoda == -1 ? "dcalcoda" : "dsalcoda") + "(" + (dsalcoda == -1 ? dcalcoda : dsalcoda) + ")");
//			return null;
		}
		boolean followCoda1 = false;
		boolean followCoda2 = false;
		boolean endAtFine = false;
		boolean skippingEnding = false;
		int loops = 0;
		boolean barMarkedBeforeNotes = false;
		int playedMeasureIdx = 0;
		for (int absoluteMeasureIdx = 0; absoluteMeasureIdx < measures.size(); absoluteMeasureIdx = (nextMeasure != -1 ? nextMeasure : absoluteMeasureIdx + 1)) {
//			System.err.println("Now parsing measure " + i);
			loops++;
			if (loops > 1000) {
				throw new RuntimeException("Excessive loops");
			}
			nextMeasure = -1;
			int currMeasurePositionInDivisions = 0; // in divisions
			Node measure = measures.get(absoluteMeasureIdx);
			try {
				NodeList mChildren = measure.getChildNodes();
				for (int j = 0; j < mChildren.getLength(); j++) {
					Node mChild = mChildren.item(j);
					if (mChild instanceof Text) continue;
					String nodeName = mChild.getNodeName();
//					System.err.println("Now parsing child " + j + ": " + nodeName);
					NodeList mGrandchildren = mChild.getChildNodes();
					if (nodeName.equals("barline") || skippingEnding) {
						if (!nodeName.equals("barline")) {
							continue;
						}
						Barline barline = null;
						try {
							barline = parseBarline(mChild);
						} catch (NumberFormatException e) {
							return null;
						}
						RepeatDirection direction = barline.getRepeatDirection();
						int[] numbers = barline.getNumbers();
						if (direction == RepeatDirection.FORWARD) {
							if (!measureOffsetInfo.containsKey(absoluteMeasureIdx))
								forwardRepeatMeasures.push(absoluteMeasureIdx);
						} else if (direction == RepeatDirection.BACKWARD) {
							int takeRepeat = 0;
							final int LAST_TIME = 2;
							if (skippingEnding) { 
								takeRepeat = 0;
							} else if (numbers == null) {
								if (!measureOffsetInfo.containsKey(absoluteMeasureIdx)) {
									takeRepeat = LAST_TIME;
									barMarkedBeforeNotes = true;
								} else  if (barMarkedBeforeNotes){
									takeRepeat = 0;
								} else {
									takeRepeat = measureOffsetInfo.get(absoluteMeasureIdx).get(0).getFirst() == 1 ? LAST_TIME : 0;
								}
								if (takeRepeat > 0) System.err.println("Repeating at measure " + absoluteMeasureIdx);
							} else {
								int ending = measureOffsetInfo.get(prevPlayedMeasure).get(0).getFirst();
								System.err.println("Seen prev measure (" + prevPlayedMeasure + ") " + ending + " times");
								for (int k = 0; k < numbers.length; k++) {
									if (ending == numbers[k]) {
										System.err.println("Repeating at ending " + k);
										takeRepeat = (k == numbers.length - 1 ? LAST_TIME : 1);
										break;
									}
								}
							}
							if (takeRepeat > 0) {
								if (forwardRepeatMeasures.size() > 1) {
									System.err.println("Multiple forward repeats on stack");
								}
								Integer lastForwardRepeat = forwardRepeatMeasures.isEmpty() ? 0 : (takeRepeat == LAST_TIME ? forwardRepeatMeasures.pop() : forwardRepeatMeasures.peek());
								nextMeasure = lastForwardRepeat;
							} else {
								break;
							}
						} else if (direction == RepeatDirection.NO_REPEAT) {
							if (numbers != null) { // ending
								System.err.println("Encountered endings " + Arrays.toString(numbers) + " in measure " + absoluteMeasureIdx);
								int ending = measureOffsetInfo.get(prevPlayedMeasure).get(0).getFirst();
								System.err.println("Seen prev measure (" + prevPlayedMeasure + ") " + ending + " times");
								skippingEnding = true;
								for (int k : numbers) {
									if (ending == k) {
										System.err.println("Taking ending " + k);
										skippingEnding = false;
										break;
									}
								}
								if (skippingEnding) {
									System.err.println("Skipping this ending");
									break;
								}
							}
						} else if (direction == null) {
							
						} else {
							throw new RuntimeException("Unknown repeat direction command:" + direction);
						}
					} else if (nodeName.equals("attributes")) {
						for (int k = 0; k < mGrandchildren.getLength(); k++) {
							Node mGrandchild = mGrandchildren.item(k);
							if (mGrandchild instanceof Text) continue;
							String gNodeName = mGrandchild.getNodeName();
							if (gNodeName.equals("divisions")) {
								currDivisionsPerQuarterNote = Integer.parseInt(mGrandchild.getTextContent());
								divsPerQuarterByAbsoluteMeasure.put(absoluteMeasureIdx, currDivisionsPerQuarterNote);
								System.err.println("Divs per quarter note:" +currDivisionsPerQuarterNote);
							} else if (gNodeName.equals("key")) {
								currKey = parseKey(mGrandchild);
								if (currKey.mode == null) {
									songsWithMissingMode.put(MusicXMLSummaryGenerator.getTitle(this),currKey.fifths);
								}
								keyByAbsoluteMeasure.put(absoluteMeasureIdx, currKey);
							} else if (gNodeName.equals("time")) {
								currTime = parseTime(mGrandchild);
								System.err.println("Current time sig:" +currTime);
								timeByAbsoluteMeasure.put(absoluteMeasureIdx, currTime);
							} else if (gNodeName.equals("clef")) {
								// do nothing
							} else if (gNodeName.equals("staff-details")) {
								// do nothing
							} else if (gNodeName.equals("transpose")) {
								// do nothing (this is for if the music is written for a non-C instrument, e.g.)
							} else if (gNodeName.equals("measure-style")) {
								parseMeasureStyle(mGrandchild);
								// do nothing 
							} else if (gNodeName.equals("staves")) {
								// do nothing 
							} else {
								throw new RuntimeException("measure \"" + nodeName + "\" node has child with unrecognized name:" + gNodeName);
							}
						}
					} else if (nodeName.equals("direction")) {
						// do nothing, tempo would be found here, theoretically
						for (int k = 0; k < mGrandchildren.getLength(); k++) {
							Node mGrandchild = mGrandchildren.item(k);
							if (mGrandchild instanceof Text) continue;
							String gNodeName = mGrandchild.getNodeName();
//							System.err.println("Now parsing grandchild " + k + ": " + gNodeName);
							if (gNodeName.equals("direction-type")) {
								DirectionType type = DirectionType.parseDirectionType(mGrandchild);
								if (type == DirectionType.DS_AL_CODA1) {
									if (followCoda1) {
										throw new RuntimeException("Detected loop (found D.S. twice without al coda");
									}
									nextMeasure = (segno == -1 ? 0 : segno);
									System.err.println("Found D.S. al Coda — skipping back to measure " + nextMeasure + " (coda=" + coda1 + ")");
									followCoda1 = true;
								} else if (type == DirectionType.DS_AL_CODA2) {
									if (followCoda2) {
										throw new RuntimeException("Detected loop (found D.S. al Coda 2 twice without al coda");
									}
									nextMeasure = (segno == -1 ? 0 : segno);
									System.err.println("Found D.S. al Coda 2 — skipping back to measure " + nextMeasure);
									followCoda2 = true;
								} else if (type == DirectionType.DS_AL_FINE) {
									if (endAtFine) {
										throw new RuntimeException("Detected loop (found D.S. twice without al coda");
									}
									nextMeasure = (segno == -1 ? 0 : segno);
									System.err.println("Found D.S. al Fine — skipping back to measure " + nextMeasure);
									endAtFine = true;
								} else if (type == DirectionType.DC_AL_FINE) {
									if (endAtFine) {
										throw new RuntimeException("Detected loop (found D.C. twice without al coda");
									}
									nextMeasure = 0;
									System.err.println("Found D.C. al Fine — skipping back to measure " + nextMeasure);
									endAtFine = true;
								}  else if (type == DirectionType.DC_AL_CODA1) {
									if (followCoda1) {
										throw new RuntimeException("Detected loop (found D.C. twice without al coda");
									}
									if (nextMeasure == -1) {
										nextMeasure = 0;
										System.err.println("Found D.C. al Coda in measure " + absoluteMeasureIdx + " — skipping back to measure " + nextMeasure);
										followCoda1 = true;
									}
								} else if (type == DirectionType.SEGNO) {
									segno = absoluteMeasureIdx;
									System.err.println("Found segno at measure " + segno);
								} else if (type == DirectionType.IGNORE) { 
									// do nothing
								} else if (type == DirectionType.AL_CODA1) { 
									// skip to coda
									if (followCoda1) {
										nextMeasure = coda1;
										System.err.println("Found coda at measure " + absoluteMeasureIdx + ", skipping to " + nextMeasure);
										followCoda1 = false;
									}
								} else if (type == DirectionType.CODA1) { 
									// at coda
									if (followCoda1 || followCoda2) {
										throw new RuntimeException("Missing Al coda; found coda instead at " + absoluteMeasureIdx);
									}
									System.err.println("Entered coda 1 at measure " + absoluteMeasureIdx);
								} else if (type == DirectionType.AL_CODA2) { 
									// skip to coda
									if (followCoda2) {
										nextMeasure = coda2;
										System.err.println("Found coda 2 at measure " + absoluteMeasureIdx + ", skipping to " + nextMeasure);
										followCoda1 = false;
									}
								} else if (type == DirectionType.CODA2) { 
									// at coda2
									if (followCoda1 || followCoda2) {
										throw new RuntimeException("Missing al coda 2; found coda 2 instead");
									}
									System.err.println("Entered coda 2 at absolute measure " + absoluteMeasureIdx);
								} else if (type == DirectionType.FINE) { 
									// skip to coda
									if (endAtFine) {
										nextMeasure = measures.size();
										System.err.println("Found fine at absolute measure " + absoluteMeasureIdx + ", skipping to " + nextMeasure);
									}
								} else {
									//throw new RuntimeException("Unhandled direction type:" + type);
								}
							} else if (gNodeName.equals("offset")||gNodeName.equals("staff")||gNodeName.equals("voice")) {

							} else if (gNodeName.equals("sound")) {
								if  (!mGrandchild.getTextContent().isEmpty()) {
//									throw new RuntimeException("Sound node stuff:" + gNodeName);
								}
							} else {
								throw new RuntimeException("Unknown direction node:" + gNodeName);
							}
						}
					} else if (nodeName.equals("harmony")) {
						Pair<Integer, Harmony> offsetHarmony = parseHarmony(mChild);
						if (offsetHarmony == null) {
							unparseableHarmonies ++;
							return null;
						}
						int divisionsOffset = offsetHarmony.getFirst();
						int measurePositionInDivisions = currMeasurePositionInDivisions + divisionsOffset;
						if (measurePositionInDivisions < 0 ) {
							throw new RuntimeException("measurePosition is negative:" + measurePositionInDivisions);
						}
						Harmony harmony = offsetHarmony.getSecond();
						addHarmonyToMeasure(playedMeasureIdx, measurePositionInDivisions, normalizeHarmony(harmony, currKey), harmonyByPlayedMeasure);
					} else if (nodeName.equals("note")) {
						Note note = parseNote(mChild, 1+getRepeatForMeasureOffset(measureOffsetInfo, absoluteMeasureIdx, currMeasurePositionInDivisions), currKey);
						if (note == null) continue;
						if (note.type > smallestNoteType) {
							smallestNoteType = note.type;
						}
						if (note.lyric != null) {
							musicXML.lyricCount++;
						}
						
						if (!note.isChordWithPrevious) {
							addNoteToMeasure(playedMeasureIdx, absoluteMeasureIdx, currMeasurePositionInDivisions, note, notesByPlayedMeasure, measureOffsetInfo);
							currMeasurePositionInDivisions += note.duration;
						}
					} else if (nodeName.equals("print")) {
						// do nothing?
					} else if (nodeName.equals("sound")) {
						// do nothing?
					} else if (nodeName.equals("backup")) {
						if (currMeasurePositionInDivisions == calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote)) {
							break;
						} else {
//							throw new RuntimeException("backup occurred mid-measure, which we don't handle");
							return null;
						}
					} else if (nodeName.equals("forward")) {
						int duration = Integer.parseInt(((Element)mChild).getElementsByTagName("duration").item(0).getTextContent());
						Note note = new Note(-1,duration,-1,null,false, 0,NoteTie.NONE,NoteTie.NONE,null,false);
						currMeasurePositionInDivisions += note.duration;
						if (currMeasurePositionInDivisions == calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote)) {
							break;
						} else {
//							throw new RuntimeException("backup occurred mid-measure, which we don't handle");
							return null;
						}
					} else {
						MusicXMLSummaryGenerator.printNode(mChild, System.err);
						throw new RuntimeException("mChild with unrecognized name:" + nodeName);
					}
				}
				if (instruments.size() > 1) {
					throw new RuntimeException("measure introduces second instrument");
				} else if (!skippingEnding && absoluteMeasureIdx != 0 && absoluteMeasureIdx != measures.size()-1 && currMeasurePositionInDivisions != calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote)) {
//					MusicXMLAnalyzer.printNode(measure, System.err);
//					throw new RuntimeException("events in measure " + i + " fill " + currMeasurePositionInDivisions + " of " + calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote) + " divisions");
//					System.err.println("Current Time Signature = " + currTime);
					return null;
				}
			} catch (RuntimeException e) {
				MusicXMLSummaryGenerator.printNode(measure, System.out);
				System.err.println("In absolute measure number " + absoluteMeasureIdx);
				throw e;
			}
			if (!skippingEnding){ // I assume this means that we "played" this measure
				prevPlayedMeasure = absoluteMeasureIdx;
				musicXML.playedToAbsoluteMeasureNumberMap.add(absoluteMeasureIdx);
				if (absoluteMeasureIdx >= musicXML.absoluteToPlayedMeasureNumbersMap.size()) {
					SortedSet<Integer> newSet = new TreeSet<Integer>();
					newSet.add(playedMeasureIdx);
					musicXML.absoluteToPlayedMeasureNumbersMap.add(newSet);
				} else {
					musicXML.absoluteToPlayedMeasureNumbersMap.get(absoluteMeasureIdx).add(playedMeasureIdx);
				}
				playedMeasureIdx++;
			}
			if (!skippingEnding && absoluteMeasureIdx == dsalcoda) {
				nextMeasure = (segno == -1 ? 0 : segno);
				System.err.println("Found D.S. al Coda in absolute measure " + absoluteMeasureIdx + " — skipping back to measure " + nextMeasure);
				followCoda1 = true;
			}
		}
		
		Integer distribution = smallestNoteTypePerSong.get(smallestNoteType);
		if (distribution == null) {
			smallestNoteTypePerSong.put(smallestNoteType,1);
		} else {
			smallestNoteTypePerSong.put(smallestNoteType,distribution+1);
		}
		
		//Resolve co-occuring harmonies
		List<Triple<Integer,Integer, Harmony>> unoverlappingHarmonyByPlayedMeasure = resolveOverlappingHarmonies(harmonyByPlayedMeasure, measureOffsetInfo, timeByAbsoluteMeasure, divsPerQuarterByAbsoluteMeasure, musicXML);
		
		// ADD SYLLABLE STRESS
		addStressToSyllables(notesByPlayedMeasure, musicXML);
		
		musicXML.setTimeByAbsoluteMeasure(timeByAbsoluteMeasure);
		normalizeKeysByOriginalKey(keyByAbsoluteMeasure);
		musicXML.normalizedKeyByAbsoluteMeasure = keyByAbsoluteMeasure;
		
		musicXML.setNotesByPlayedMeasure(notesByPlayedMeasure);
		musicXML.unoverlappingHarmonyByPlayedMeasure = unoverlappingHarmonyByPlayedMeasure;
		musicXML.setDivsPerQuarterByAbsoluteMeasure(divsPerQuarterByAbsoluteMeasure);
		
		return musicXML;
	}

	/** 
	 * all songs should start in C (or Am), therefore all keys in the song should be transposed accordingly
	 * @param keyByMeasure
	 */
	private void normalizeKeysByOriginalKey(SortedMap<Integer, Key> keyByMeasure) {
		
		Key originalKey = keyByMeasure.get(keyByMeasure.firstKey());
		
		for (Integer msr : keyByMeasure.keySet()) {
			normalizeKeyByOriginalKey(originalKey, keyByMeasure.get(msr));
		}
		
	}

	private void normalizeKeyByOriginalKey(Key normalizingKey, Key keyToNormalize) {
		keyToNormalize.fifths -= normalizingKey.fifths;
	}

	private static void addStressToSyllables(List<Triple<Integer, Integer, Note>> notesByMeasure, ParsedMusicXMLObject musicXML) {
		List<NoteLyric> currentWordNotes = null;
		int totalSyllablesWithStress = 0;
		int totalSyllables = 0;
		List<StressedPhone[]> phones;
		for (Triple<Integer,Integer,Note> triple : notesByMeasure) {
			final Note note = triple.getThird();
			if ( note == null ) {
				continue;
			}
			NoteLyric currNoteLyric = note.lyric;
			if ( currNoteLyric == null || currNoteLyric.text == null) {
				continue;
			}
			totalSyllables++;
			if (currNoteLyric.syllabic == null) {
				phones = Phonetecizer.getPhones(currNoteLyric.text,true);
				if (phones.size() == 1) {
					List<Triple<String, StressedPhone[], Integer>> syllables = Syllabifier.syllabify(currNoteLyric.text, phones.get(0));
					if (syllables.size() == 1) {
						currNoteLyric.addSyllableStress(syllables.get(0));
						totalSyllablesWithStress++;
					} else {
						System.err.println("Multiple syllables for note with lyric \"" + currNoteLyric.text + "\"");
					}
				} else {
					if (phones.isEmpty()) {
						musicXML.lyricsWithoutStress.add(currNoteLyric.text);
					} else 
						System.err.println("" + phones.size() + " entries in phone dict for \"" + currNoteLyric.text + "\"");
				}
				currentWordNotes = null;
				continue;
			}
			switch(currNoteLyric.syllabic) {
			case BEGIN:
				if (currentWordNotes != null) {
					musicXML.syllablesNotLookedUp.addAll(currentWordNotes);
				}
				
				currentWordNotes = new ArrayList<NoteLyric>();
				currentWordNotes.add(currNoteLyric);
				break;
			case END:
				StringBuilder wordBuilder = new StringBuilder();
				if (currentWordNotes == null) {
					currentWordNotes = new ArrayList<NoteLyric>();
				}
				currentWordNotes.add(currNoteLyric);
				for (NoteLyric noteLyric : currentWordNotes) {
					wordBuilder.append(noteLyric.text);
				}
				final String word = wordBuilder.toString();
				phones = Phonetecizer.getPhones(word,true);
				if (phones.size() == 1 || allPhonesHaveSameSyllablesAndStress(word,phones)) {
					List<Triple<String, StressedPhone[], Integer>> syllables = Syllabifier.syllabify(word, phones.get(0));
					if (syllables.size() == currentWordNotes.size()) {
						for (int i = 0; i < currentWordNotes.size(); i++) {
							currentWordNotes.get(i).addSyllableStress(syllables.get(i));
							totalSyllablesWithStress++;
						}
					} else {
						musicXML.lyricsWithDifferentSyllableCountThanAssociatedNotes.add(new Pair<List<NoteLyric>, List<Triple<String, StressedPhone[], Integer>>>(currentWordNotes,syllables));
						System.err.println("" + currentWordNotes.size() + " notes mismatch with " + syllables.size() + " syllables:" + word);
					}
				} else {
					if (phones.isEmpty()) {
						musicXML.lyricsWithoutStress.add(word);
					} else 
						System.err.println("" + phones.size() + " entries with different stresses in phone dict for multi-syllable \"" + word + "\"");
				}
				currentWordNotes = null;
				break;
			case MIDDLE:
				if (currentWordNotes == null) {
					currentWordNotes = new ArrayList<NoteLyric>();
				}
				currentWordNotes.add(currNoteLyric);
				break;
			case SINGLE:
				phones = Phonetecizer.getPhones(currNoteLyric.text,true);
				if (phones.size() == 1) {
					List<Triple<String, StressedPhone[], Integer>> syllables = Syllabifier.syllabify(currNoteLyric.text, phones.get(0));
					if (syllables.size() == 1) {
						currNoteLyric.addSyllableStress(syllables.get(0));
						totalSyllablesWithStress++;
					} else {
						System.err.println("Multiple syllables for note with lyric \"" + currNoteLyric.text + "\"");
					}
				} else {
					if (phones.isEmpty()) {
						musicXML.lyricsWithoutStress.add(currNoteLyric.text);
					} else 
						System.err.println("" + phones.size() + " entries in phone dict for \"" + currNoteLyric.text + "\"");
				}
				if (currentWordNotes != null) {
					musicXML.syllablesNotLookedUp.addAll(currentWordNotes);
				}
				currentWordNotes = null;
				break;
			default:
				//check elision and extend
				break;
			
			}
		}
		System.err.println("For " + notesByMeasure.size() + " notes with " + totalSyllables + " syllables, " + totalSyllablesWithStress + " had stress info from phone dict");
		musicXML.totalSyllables = totalSyllables;
		musicXML.totalSyllablesWithStressFromEnglishDictionary = totalSyllablesWithStress;
	}

	private static boolean allPhonesHaveSameSyllablesAndStress(String word, List<StressedPhone[]> phones) {
		if (phones.size() == 0) return false;
		
		List<Triple<String, StressedPhone[], Integer>> first = Syllabifier.syllabify(word, phones.get(0));
		
		for (int i = 1; i < phones.size(); i++) {
			List<Triple<String, StressedPhone[], Integer>> next = Syllabifier.syllabify(word, phones.get(i));
			if (first.size() != next.size()) return false;
			
			for (int j = 0; j < next.size(); j++) {
				if (first.get(j).getThird() != next.get(j).getThird()) return false;
			}
		}
		
		return true;
	}

	public static Harmony normalizeHarmony(Harmony currHarmony, Key currKey) {
		if (currHarmony == null || currHarmony.root == null || currHarmony.root.rootStep == Pitch.NO_KEY || currKey == null || currKey.fifths == 0) return currHarmony;
		int newRootStep = normalizePitch(currHarmony.root.rootStep, currKey);
		if (newRootStep < 0) {
			newRootStep += 12;
		} else if (newRootStep >= 12) {
			newRootStep -= 12;
		}
		Root newRoot = new Root(newRootStep);
		
		Bass newBass = null;
		if (currHarmony.bass != null) {
			int newBassStep = normalizePitch(currHarmony.bass.bassStep, currKey);
			if (newBassStep < 0) {
				newBassStep += 12;
			} else if (newBassStep >= 12) {
				newBassStep -= 12;
			}
			newBass = new Bass(newBassStep);
			if (newBassStep == currHarmony.bass.bassStep)
				throw new RuntimeException("Shouldn't happen");
		}
		
		if (newRootStep == currHarmony.root.rootStep)
			throw new RuntimeException("Shouldn't happen");
		return new Harmony(newRoot,currHarmony.quality,newBass);
	}

	public static int normalizePitch(int pitch, Key currKey) {
		if (pitch < 0 || currKey == null) return pitch;
		
		int modification = (7*currKey.fifths + 144) % 12;
		if (modification > 6) {
			modification -= 12;
		}
		return (pitch - modification);
	}

//	public static int normalizePitchByHarmonyRoot(int pitch, Harmony currHarmony) {
//		if (pitch < 0 || currHarmony == null) {
//			return pitch;
//		}
//		
//		int modification = (currHarmony.root.rootStep + 9) % 12;
//		if (modification > 6) {
//			modification -= 12;
//		}
//		return (pitch - modification);
//	}
	
	private static int findDirectionTypeStartingFrom(List<Node> measures, DirectionType directionType, int startPoint) {
		for (int i = startPoint-1; i >= 0; i--) {
			Node measure = measures.get(i);
			NodeList mChildren = measure.getChildNodes();
			for (int j = 0; j < mChildren.getLength(); j++) {
				Node mChild = mChildren.item(j);
				if (mChild instanceof Text) continue;
				String nodeName = mChild.getNodeName();
				if (nodeName.equals("direction")) {
					NodeList mGrandchildren = mChild.getChildNodes();
					// do nothing, tempo would be found here, theoretically
					for (int k = 0; k < mGrandchildren.getLength(); k++) {
						Node mGrandchild = mGrandchildren.item(k);
						if (mGrandchild instanceof Text) continue;
						String gNodeName = mGrandchild.getNodeName();
						if (gNodeName.equals("direction-type")) {
							try {
								DirectionType type = DirectionType.parseDirectionType(mGrandchild);
								if (type == directionType) { 
									return i;
								} 
							} catch (Exception e) {
								MusicXMLSummaryGenerator.printNode(measure, System.out);
								System.err.println("In measure number " + i);
								throw e;
							}
						} 
					}
				} 
			}
		}
		return -1;
	}

	private static Barline parseBarline(Node node) {
		Barline barline = new Barline();
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("bar-style")) {
				// do nothing
			} else if (childName.equals("repeat")) {
				String direction = child.getAttributes().getNamedItem("direction").getTextContent();
				barline.addRepeatDirection(RepeatDirection.parse(direction));
			} else if (childName.equals("ending")) { 
				final String textContent = child.getAttributes().getNamedItem("number").getTextContent().trim();
				if (textContent.isEmpty())
					continue;
				String[] numberStrings = textContent.split("\\s*[,.]\\s*");
				int[] numbers = new int[numberStrings.length];
				for (int j = 0; j < numberStrings.length; j++) {
					numbers[j] = Integer.parseInt(numberStrings[j]);
				}
				barline.addNumbers(numbers);
			} else {
				throw new RuntimeException("Unhandled barline child node:" + childName);
			}
		}
		return barline;
	}

	private static int getRepeatForMeasureOffset(Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo, int measure,
			int offset) {
		Map<Integer, Pair<Integer, NoteLyric>> repeatsByOffset = measureOffsetInfo.get(measure);
		if (repeatsByOffset == null)
			return 0;
		else {
			Pair<Integer, NoteLyric> repeat = repeatsByOffset.get(offset);
			if (repeat == null) 
				return 0;
			else
				return repeat.getFirst();
		}
	}

	private static List<Triple<Integer, Integer, Harmony>> resolveOverlappingHarmonies(List<Triple<Integer, Integer, List<Harmony>>> harmonyByMeasure, 
			Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo, Map<Integer,Time> timeByMeasure, Map<Integer, Integer> divsByMeasure, ParsedMusicXMLObject musicXML) {
		List<Triple<Integer, Integer, Harmony>> unoverlappingHarmonyByMeasure = new ArrayList<Triple<Integer, Integer, Harmony>>(); 
		// If there are multiple chords, they must all occur before the next note occurs after the first chord,
		// otherwise they'd have marked the chord later.
		
		for (Triple<Integer, Integer, List<Harmony>> triple : harmonyByMeasure) {
			Integer measure = triple.getFirst();
			Integer offset = triple.getSecond();
			List<Harmony> harmonies = triple.getThird();
			if (harmonies.size() == 1) {
				unoverlappingHarmonyByMeasure.add(new Triple<Integer, Integer, Harmony>(measure, offset, harmonies.get(0)));
			} else {
				Time currTime = timeAtMeasure(timeByMeasure,measure);
				int totalDivisionsInCurrMeasure = calculateTotalDivisionsInMeasure(currTime,divsAtMeasure(divsByMeasure,measure));
				Integer offsetLimit = Integer.min(nextOccurringNoteEvent(measureOffsetInfo, measure, offset),
						totalDivisionsInCurrMeasure);
				unoverlappingHarmonyByMeasure.addAll(resolveOverlappingHarmonies(harmonies, measure, offset, offsetLimit, totalDivisionsInCurrMeasure/currTime.beats, currTime));
			}
		}
		
		return unoverlappingHarmonyByMeasure;
	}

	private static Integer divsAtMeasure(Map<Integer, Integer> divsByMeasure, Integer measure) {
		Integer lastDivs = null;
		for (Integer lastMeasure : divsByMeasure.keySet()) {
			if (lastMeasure > measure) {
				return lastDivs;
			}
			lastDivs = divsByMeasure.get(lastMeasure);
		}
		
		assert lastDivs != null;
		return lastDivs;
	}

	private static Time timeAtMeasure(Map<Integer, Time> timeByMeasure, Integer measure) {
		Time lastTime = null;
		for (Integer lastMeasure : timeByMeasure.keySet()) {
			if (lastMeasure > measure) {
				return lastTime;
			}
			lastTime = timeByMeasure.get(lastMeasure);
		}
		
		assert lastTime != null;
		return lastTime;
	}

	static int removedChordsCount = 0;
	
	private static Collection<? extends Triple<Integer, Integer, Harmony>> resolveOverlappingHarmonies(List<Harmony> harmonies, Integer nonrepeatedMeasureNumber, Integer startOffset, Integer offsetLimit, int divsPerBeat, Time currTime) {
		List<Triple<Integer, Integer, Harmony>> resolvedHarmonies = new ArrayList<Triple<Integer, Integer, Harmony>>();
		
		resolvedHarmonies.add(new Triple<Integer, Integer, MusicXMLParser.Harmony>(nonrepeatedMeasureNumber, startOffset, harmonies.remove(0)));

		List<Integer> validChordOffsets = null;
		
		validChordOffsets = getValidChordOffsets(startOffset+1, offsetLimit, divsPerBeat, currTime, harmonies.size());
		
//		assert validChordOffsets.size() != 0 : "no valid chord positions in interval";
		while (validChordOffsets.size() < harmonies.size()) {
			System.err.println("insufficient measure divisions for overlapping chord resolution");
			harmonies.remove(0);
			removedChordsCount++;
		}
		
		for (int i = 1; i < harmonies.size(); i++) {
			resolvedHarmonies.add(new Triple<Integer, Integer, MusicXMLParser.Harmony>(nonrepeatedMeasureNumber, validChordOffsets.get(validChordOffsets.size()-i), harmonies.get(harmonies.size()-i)));
		}
		
		return resolvedHarmonies;
	}

	
	private static List<Integer> getValidChordOffsets(Integer startOffset, Integer offsetLimit, int divsPerBeat, Time currTime, int itemsToAdd) {
		// don't add front to back, only add as many as we need and add them in order of their importance... beat 3, then 4, then 2, 
		Double[] bestChordOffsets = null;
		switch (currTime.beats) {
		case 2:
			assert divsPerBeat > 1 && divsPerBeat % 2 == 0;
			bestChordOffsets = new Double[]{0.0,1.0,1.5,0.5};
			break;
		case 3:
			bestChordOffsets = new Double[]{0.0,2.0,1.0};
			break;
		case 4:
			bestChordOffsets = new Double[]{0.0,2.0,3.0,1.0};
			break;
		case 6:
			bestChordOffsets = new Double[]{0.0,3.0,5.0,2.0};
			break;
		case 9:
			bestChordOffsets = new Double[]{0.0,3.0,6.0};
			break;
		case 12:
			bestChordOffsets = new Double[]{0.0,6.0,3.0,9.0};
			break;
		}
		
		List<Integer> validChordOffsets = new ArrayList<Integer>();
		for (Double beat : bestChordOffsets) {
			int offset = (int) (beat * divsPerBeat);
			if (offset >= startOffset && offset < offsetLimit) {
				validChordOffsets.add(offset);
				if (validChordOffsets.size() == itemsToAdd)
					return validChordOffsets;
			}
		}
		return validChordOffsets;
	}

	private static Integer nextOccurringNoteEvent(
			Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo, Integer startMeasure, Integer startOffset) {
		
		for (Integer measure : measureOffsetInfo.keySet()) {
			Map<Integer, Pair<Integer, NoteLyric>> offsetInfo = measureOffsetInfo.get(measure);
			for (Integer offset : offsetInfo.keySet()) {
				if (measure == startMeasure && offset > startOffset) {
					return offset;
				} else if (measure > startMeasure) {
					return Integer.MAX_VALUE;
				}
			}
		}
		
		return Integer.MAX_VALUE;
	}

	private static final NoteLyric MULTIVERSE = new NoteLyric(null,"VERSE LYRIC",false,false);
	private static void addNoteToMeasure(int playedMeasureNumber, int absoluteMeasureNumber, int currMeasurePositionInDivisions, Note note,
			List<Triple<Integer, Integer, Note>> notesByMeasure, Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo) {
		// add note to the play order
		notesByMeasure.add(new Triple<Integer, Integer, Note>(playedMeasureNumber, currMeasurePositionInDivisions, note));
		
		// adjust how many times this note has been seen and whether the lyrics are the same across all repeats
		Map<Integer, Pair<Integer, NoteLyric>> offsetInfo = measureOffsetInfo.get(absoluteMeasureNumber);
		if (offsetInfo == null) {
			offsetInfo = new TreeMap<Integer, Pair<Integer, NoteLyric>>();
			measureOffsetInfo.put(absoluteMeasureNumber, offsetInfo);
		}
		
		Pair<Integer, NoteLyric> info = offsetInfo.get(currMeasurePositionInDivisions);
		if (info == null) {
			info = new Pair<Integer, NoteLyric>(1, note.lyric);
			offsetInfo.put(currMeasurePositionInDivisions, info);
		} else {
			info.setFirst(info.getFirst()+1);
			NoteLyric prevLyric = info.getSecond();
			if (prevLyric != MULTIVERSE) {
				if (prevLyric == null && note.lyric != null || prevLyric != null && !prevLyric.equals(note.lyric)) {
					info.setSecond(MULTIVERSE);
				}
			}
		}
	}

	private static void addHarmonyToMeasure(int nonrepeatedMeasureNumber, Integer offset, Harmony harmony, List<Triple<Integer, Integer, List<Harmony>>> harmonyByMeasure) {
		Triple<Integer, Integer, List<Harmony>> msrOffset = null;
		List<Harmony> harmoniesAtMsrOffset;
		
		if (!harmonyByMeasure.isEmpty()) {
			msrOffset = harmonyByMeasure.get(harmonyByMeasure.size()-1);
		}
		
		if (msrOffset == null || msrOffset.getFirst() != nonrepeatedMeasureNumber || msrOffset.getSecond() != offset) {
			// add a new list
			harmoniesAtMsrOffset = new ArrayList<Harmony>();
			harmoniesAtMsrOffset.add(harmony);
			msrOffset = new Triple<Integer, Integer, List<Harmony>>(nonrepeatedMeasureNumber, offset, harmoniesAtMsrOffset);
			harmonyByMeasure.add(msrOffset);
		} else {
			harmoniesAtMsrOffset = msrOffset.getThird();
		}
		
		// We use a list in case multiple harmonies co-occur; these will be resolved (i.e., evenly distributed) later 
		if (harmoniesAtMsrOffset.get(harmoniesAtMsrOffset.size()-1).equals(harmony)) {
			return;
		}
		harmoniesAtMsrOffset.add(harmony);
	}

	private static int calculateTotalDivisionsInMeasure(Time currTime, int currDivisionsPerQuarterNote) {
		// how many quarter notes?
		double quarterNoteCount = currTime.beats * 4.0 / currTime.beatType;
		int totalDivisionsInMeasure = (int) Math.round(quarterNoteCount * currDivisionsPerQuarterNote);
		return totalDivisionsInMeasure;
	}

	private static void parseMeasureStyle(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("multiple-rest")) {
				// do nothing
			} else if (childName.equals("slash")) {
				// do nothing
			} else if (childName.equals("measure-repeat")) {
				// do nothing
			} else {
				throw new RuntimeException("Unrecognized measure style node:" + childName);
			}
		}
	}

	private static final int UNPITCHED_RHYTHM = -3;
	private static Note parseNote(Node node, int currVerse, Key currKey) {
		int pitch = -1;
		int duration = -1;
		int type = -1;
		NoteLyric lyric = null;
		boolean lyricVerseMatchesRepeatCount = false;
		int dots = 0;
		NoteTie tie = NoteTie.NONE;
		NoteTie slur = NoteTie.NONE;
		NoteTimeModification timeModification = null;
		boolean isChordWithPreviousNote = false;
		boolean isGrace = false;
				
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("pitch")) {
				pitch = parseNotePitch(child);
			} else if (childName.equals("duration")) {
				duration = Integer.parseInt(child.getTextContent());
			} else if (childName.equals("voice")) {
				// do nothing (we only have one voice in our application...)
			} else if (childName.equals("type")) {
				type = parseNoteType(child.getTextContent());
			} else if (childName.equals("stem")) {
				// do nothing (this is for visualizing music)
			} else if (childName.equals("accidental")) {
				// do nothing (this is for visualizing music)
			} else if (childName.equals("beam")) {
				// do nothing (this is for visualizing music)
			} else if (childName.equals("lyric")) {
				int parsedVerse = Integer.parseInt(child.getAttributes().getNamedItem("number").getTextContent());
				if (parsedVerse <= currVerse && !lyricVerseMatchesRepeatCount) { // we take any verse before or up to the current verse, assuming that if there is a closer matching verse, it will replace lyric
					if (parsedVerse == currVerse) {
						lyricVerseMatchesRepeatCount = true;
					}
					lyric = parseNoteLyric(child);
				}
			} else if (childName.equals("dot")) {
				dots++;
			} else if (childName.equals("tie")) {
				NoteTie tie2 = NoteTie.parse(child.getAttributes().getNamedItem("type").getTextContent());
				if (tie != NoteTie.NONE && tie2 != tie)
					tie = NoteTie.NONE;
				else
					tie = tie2;
			} else if (childName.equals("notations")) {
				NodeList grandchildren = child.getChildNodes();
				for (int j = 0; j < grandchildren.getLength(); j++) {
					Node grandchild = grandchildren.item(j);
					if (grandchild instanceof Text) continue;
					String grandchildName = grandchild.getNodeName();
					if (grandchildName.equals("slur")) {
						NoteTie slur2 = NoteTie.parse(grandchild.getAttributes().getNamedItem("type").getTextContent());
						if (slur != NoteTie.NONE && slur2 != slur)
							slur = NoteTie.NONE;
						else 
							slur = slur2;
					} else if (grandchildName.equals("tied")) {
						NoteTie tie2 = NoteTie.parse(grandchild.getAttributes().getNamedItem("type").getTextContent());
						if (tie != NoteTie.NONE && tie2 != tie)
							tie = NoteTie.NONE;
						else
							tie = tie2;
					} else if (grandchildName.equals("fermata") || grandchildName.equals("tuplet") 
							|| grandchildName.equals("articulations") || grandchildName.equals("technical")
							|| grandchildName.equals("glissando") || grandchildName.equals("ornaments") 
							|| grandchildName.equals("arpeggiate")) {
						// do nothing
					} else {
						MusicXMLSummaryGenerator.printNode(child, System.err);
						throw new RuntimeException("Unknown child of node notations:" + grandchildName);
					}
				}
			} else if (childName.equals("staff")) {
				// do nothing (this is for visualizing music)
			} else if (childName.equals("instrument")) {
				// do nothing (we only have one instrument)
				instruments.add(child.getAttributes().getNamedItem("id").getTextContent());
			} else if (childName.equals("rest")) {
				pitch = Note.REST;
			} else if (childName.equals("time-modification")) {
				timeModification = parseNoteTimeModification(child);
			} else if (childName.equals("chord")) {
				isChordWithPreviousNote = true;
			} else if (childName.equals("grace")) {
				isGrace = true;
			} else if (childName.equals("cue")) {
				pitch = Note.REST;
			} else if (childName.equals("notehead")) {
				String text = child.getTextContent();
				if (text.equals("x") || text.equals("slash") || text.equals("triangle")) {
					pitch = UNPITCHED_RHYTHM;
				} else if (text.equals("normal") || text.equals("mi") || text.equals("diamond") || text.equals("circle-x")) {
					// do nothing
				} else {
					throw new RuntimeException("Unknown value for notehead:" + child.getTextContent());
				}
			} else {
				throw new RuntimeException("Unknown child of note node:" + childName);
			}
		}
		
		if (isGrace && duration == -1)
			return null;
		
		if (pitch == -1 || duration == -1) {
			MusicXMLSummaryGenerator.printNode(node, System.err);
			throw new RuntimeException("Note missing pitch or duration or type");
		}
		
		return new Note(normalizePitch(pitch, currKey), duration, type, lyric, lyricVerseMatchesRepeatCount, dots, tie, slur, timeModification, isChordWithPreviousNote); 
	}
	
	private static NoteTimeModification parseNoteTimeModification(Node node) {
		int actualNotes = -1;
		int normalNotes = -1;
		int normalType = -1;
		boolean normalDot = false;
		
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("actual-notes")) {
				actualNotes = Integer.parseInt(child.getTextContent());
			} else if (childName.equals("normal-notes")) {
				normalNotes = Integer.parseInt(child.getTextContent());
			} else if (childName.equals("normal-type")) {
				normalType = parseNoteType(child.getTextContent());
			} else if (childName.equals("normal-dot")) {
				normalDot = true;
			} else {
				MusicXMLSummaryGenerator.printNode(node, System.err);
				throw new RuntimeException("Unknown child of note time modification:" + childName);
			}
		}
		
		if (actualNotes == -1 || normalNotes == -1) {
			MusicXMLSummaryGenerator.printNode(node, System.err);
			throw new RuntimeException("NoteTimeModification missing actual/normal notes");
		}
		
		return new NoteTimeModification(actualNotes, normalNotes, normalType, normalDot);
	}

	private static NoteLyric parseNoteLyric(Node node) {
		Syllabic syllabic = null;
		String text = null;
		boolean extend = false;
		boolean elision = false;
		
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("syllabic")) {
				syllabic = Syllabic.parse(child.getTextContent());
			} else if (childName.equals("text")) {
				text = child.getTextContent();
			} else if (childName.equals("extend")) {
				extend = true;
			} else if (childName.equals("elision")) {
				elision = true;
			} else {
				MusicXMLSummaryGenerator.printNode(node, System.err);
				throw new RuntimeException("Unknown child of note node:" + childName);
			}
		}
		
		if (text == null && !extend) {
			MusicXMLSummaryGenerator.printNode(node, System.err);
			throw new RuntimeException("Note lyric missing text or extend");
		}
		
		return new NoteLyric(syllabic, text, extend, elision);
	}

	private static int parseNotePitch(Node node) {
		int step = -1;
		int alter = 0;
		int octave = -1;
		
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("step")) {
				step = (Pitch.getPitchValue(child.getTextContent()) + 9) % 12;
			} else if (childName.equals("alter")) {
				alter = Integer.parseInt(child.getTextContent());
			} else if (childName.equals("octave")) {
				octave = Integer.parseInt(child.getTextContent());
			} else {
				throw new RuntimeException("Unknown child of pitch node:" + childName);
			}
		}
		
		if (step == -1 || step == Pitch.NO_KEY) {
			MusicXMLSummaryGenerator.printNode(node, System.err);
			throw new RuntimeException("Harmony missing root");
		}
		
		return (step + alter + 12) % 12 + 12*octave;
	}

	private static int parseNoteType(String text) {
		if (text.equals("whole"))
			return 1;
		else if (text.equals("half"))
			return 2;
		else if (text.equals("quarter"))
			return 4;
		else if (text.equals("eighth"))
			return 8;
		else if (text.equals("16th"))
			return 16;
		else if (text.equals("32nd"))
			return 32;
		else if (text.equals("64th"))
			return 64;
		else if (text.equals("128th"))
			return 128;
		else 
			throw new RuntimeException("Unknown note type:" + text);
	}
	
	private static String interpretNoteType(int type) {
		switch(type){
		case 1:
			return "whole";
		case 2:
			return "half";
		case 4:
			return "quarter";
		case 8:
			return "eighth";
		case 32:
			return "32nd";
			default:
				return "" + type + "th";
		}
	}

	private static Pair<Integer, Harmony> parseHarmony(Node node) {
		Root root = null;
		Quality quality = new Quality();
		Bass bass = null;
		int offset = 0;

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("root")) {
				root = parseRoot(child);
			} else if (childName.equals("kind")) {
				if (!quality.parseKindContentText(child.getTextContent().trim())) {
					if(!quality.parseKindTextAttribute(child.getAttributes().getNamedItem("text"))){
						//quality is assumed to be major
					}
				}
			} else if (childName.equals("degree")) {
				quality.parseDegreeNode(child);
			} else if (childName.equals("bass")) {
				bass = parseBass(child);
			} else if (childName.equals("offset")) {
				offset = Integer.parseInt(child.getTextContent());
			} else if (childName.equals("staff")) {
				// do nothing
			} else if (childName.equals("level")) {
				// do nothing
			} else if (childName.equals("frame")) {
				// do nothing
			} else {
				MusicXMLSummaryGenerator.printNode(node, System.err);
				throw new RuntimeException("Unknown child of harmony node:" + childName);
			}
		}
		
		if (root == null) {
			MusicXMLSummaryGenerator.printNode(node, System.err);
			throw new RuntimeException("Harmony missing root");
		}
		
		return new Pair<Integer, Harmony>(offset,new Harmony(root, quality, bass));
	}

	private static Bass parseBass(Node node) {
		int bassStep = -1;
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("bass-step")) {
				bassStep = Pitch.getPitchValue(child.getTextContent());
				if (bassStep == Pitch.NO_KEY)
					throw new RuntimeException("Invalid root-step value:"+child.getTextContent());
			} else if (childName.equals("bass-alter")){
				bassStep += Integer.parseInt(child.getTextContent())+12;
				bassStep %= 12;
			} else {
				throw new RuntimeException("Unknown child of harmony node:" + childName);
			}
		}
		
		if (bassStep == -1) {
			MusicXMLSummaryGenerator.printNode(node, System.err);
			throw new RuntimeException("Time node missing beats or beatType");
		}
		
		return new Bass(bassStep);
	}

	private static Root parseRoot(Node node) {
		int rootStep = -1;
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("root-step")) {
				rootStep = Pitch.getPitchValue(child.getTextContent());
				if (rootStep == Pitch.NO_KEY)
					throw new RuntimeException("Invalid root-step value:"+child.getTextContent());
			} else if (childName.equals("root-alter")){
				rootStep += Integer.parseInt(child.getTextContent())+12;
				rootStep %= 12;
			} else {
				throw new RuntimeException("Unknown child of harmony node:" + childName);
			}
		}
		
		if (rootStep == -1) {
			MusicXMLSummaryGenerator.printNode(node, System.err);
			throw new RuntimeException("Time node missing beats or beatType");
		}
		
		return new Root(rootStep);
	}

	static Map<String,Integer> songsWithMissingMode = new HashMap<String,Integer>();
	private static Key parseKey(Node node) {
		int fifths = -100;
		KeyMode mode = null;
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("fifths")) {
				fifths = Integer.parseInt(child.getTextContent());
			} else if (childName.equals("mode")) {
				mode = KeyMode.parseMode(child.getTextContent());
			} else if (childName.equals("cancel")) {
				//do nothing (just indicates to visually show naturals to demonstrate end of prev key)
			} else {
				throw new RuntimeException("Unknown child of time node:" + childName);
			}
		}
		
		if (fifths == -100) {
			throw new RuntimeException("Key node missing fifths");
		}
		if (mode == null) {
			System.err.println("Key node missing mode. Fifths = " + fifths);
		}
		
		return new Key(fifths, mode);
	}
	
	static Map<Integer,Integer> smallestNoteTypePerSong = new TreeMap<Integer,Integer>();


	private static Time parseTime(Node node) {
		int beats = -1;
		int beatType = -1;
		
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) continue;
			String childName = child.getNodeName();
			if (childName.equals("beats")) {
				beats = Integer.parseInt(child.getTextContent());
			} else if (childName.equals("beat-type")) {
				beatType = Integer.parseInt(child.getTextContent());
			} else {
				throw new RuntimeException("Unknown child of time node:" + childName);
			}
		}
		
		if (beats == -1 || beatType == -1) {
			throw new RuntimeException("Time node missing beats or beatType");
		}
		return new Time(beats, beatType);
	}

	
	public static void main(String[] args) throws ZipException, IOException, ParserConfigurationException, SAXException, TransformerException {
		int unrecoverableXML = 0;
		File[] files = new File("/Users/norkish/Archive/2017_BYU/ComputationalCreativity/data/Wikifonia").listFiles();
		for (File file : files) {
			 if (!file.getName().equals("Billy Joel - Just The Way You Are.mxl"))
				 continue;
//			if (file.getName().charAt(0) < 'T') {
//				continue;
//			}
//			if (file.getName().compareTo("Astor Piazzolla - Oblivion.mxl") < 0) {
//				continue;
//			}
			 System.out.println(file.getName());
			 MusicXMLParser musicXMLParser = new MusicXMLParser(file.getName(),MusicXMLSummaryGenerator.mxlToXML(file));
			 WikifoniaCorrection.applyManualCorrections(musicXMLParser, file.getName());
//			 MusicXMLSummaryGenerator.printDocument(musicXML.xml, System.out);
			 ParsedMusicXMLObject parsedObject;
			 try {
				 parsedObject = musicXMLParser.parse(true);
			 } catch (AssertionError e) {
				 System.err.println(file.getName());
				 throw e;
			 } catch (Exception e) {
				 System.err.println(file.getName());
				 throw e;
			 }
			 if (parsedObject == null)
				 unrecoverableXML++;
			 System.out.println(parsedObject);
		}
		
		System.out.println("badXMLs:" + unrecoverableXML);
		System.out.println("Songs with missing key mode:");
		for (Entry<String, Integer> s : songsWithMissingMode.entrySet()) {
			System.out.println("\t" + s.getKey() + " :: " + s.getValue());
		}

		System.out.println("Smallest note per song:");
		for (Integer i : smallestNoteTypePerSong.keySet()) {
			System.out.println("\t" + i + ":" + smallestNoteTypePerSong.get(i));
		}
		System.out.println("Chords removed in resolving overlapping chords:" + removedChordsCount);
		System.out.println("Songs nixed for unparseable harmony:" + unparseableHarmonies);
	}
}
