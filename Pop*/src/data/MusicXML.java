package data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.zip.ZipException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import data.MusicXML.Barline.RepeatDirection;
import pitch.Pitch;
import syllabify.Syllabifier;
import tabcomplete.rhyme.Phonetecizer;
import tabcomplete.rhyme.StressedPhone;
import tabcomplete.utils.Utils;
import utils.Pair;
import utils.Triple;

public class MusicXML {

	public enum DirectionType {
		DS_AL_CODA1, DS_AL_CODA2, SEGNO, IGNORE, CODA1, CODA2, DS_AL_FINE, FINE, DC_AL_FINE, DC_AL_CODA;
		
		public static DirectionType parseDirectionType(Node node) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Text) continue;
				String childName = child.getNodeName();
				if (childName.equals("words")) {
					String words = child.getTextContent().toLowerCase().replaceAll("[^a-zA-Z0-9]", "").trim();
					if (words.contains("dsalcoda2")) { 
						return DirectionType.DS_AL_CODA2;
					} else if (words.contains("dsalcoda")) {
						return DirectionType.DS_AL_CODA1;
					} else if (words.contains("dsalfine")) { 
						return DirectionType.DS_AL_FINE;
					} else if (words.contains("dcalfine") || words.contains("dcaalfine")) { 
						return DirectionType.DC_AL_FINE;
					} else if (words.contains("dcalcoda")) { 
						return DirectionType.DC_AL_CODA;
					} else if (words.contains("coda") || words.contains("tocoda")) {
						return DirectionType.CODA1;
					} else if (words.contains("coda2") || words.contains("tocoda2")) {
						return DirectionType.CODA2;
					} else if (words.contains("fine") || words.contains("alfine")) {
						return DirectionType.FINE;
					} else if (words.contains("voicetacet")) {
						return DirectionType.IGNORE;
					} else if (words.contains("rubato")) { 
						return DirectionType.IGNORE;
					} else if (words.contains("dcalcoda") || words.contains("fine") && 
							!words.equals("repeat4timesthendsalfine") && !words.equals("4xfine")) { 
						throw new RuntimeException("dcalcoda:" + words);
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

	public class NoteTimeModification {

		public int actualNotes;
		public int normalNotes;
		public int normalType;
		public boolean normalDot;

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

		public Syllabic syllabic;
		public String text;
		public boolean extend;
		public boolean elision;
		public Triple<String, StressedPhone[], Integer> syllableStress;

		public NoteLyric(Syllabic syllabic, String text, boolean extend, boolean elision) {
			this.syllabic = syllabic;
			this.text = text;
			this.extend = extend;
			this.elision = elision;
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

		public int pitch;
		public int duration;
		public int type; // represents the denominator in the note (e.g., quarter = 1/4)
		public NoteLyric lyric;
		public int dots;
		public NoteTie tie;
		public NoteTimeModification timeModification;
		public boolean isChordWithPrevious;
		
		public Note(int pitch, int duration, int type, NoteLyric lyric, int dots, NoteTie tie, NoteTimeModification timeModification, boolean isChordWithPreviousNote) {
			this.pitch = pitch;
			this.duration = duration;
			this.type = type;
			this.lyric = lyric;
			this.dots = dots;
			this.tie = tie;
			this.timeModification = timeModification;
			this.isChordWithPrevious = isChordWithPreviousNote;
		}
		
		public String toString() {
			StringBuilder str = new StringBuilder();
			
			str.append(Pitch.getPitchName((pitch+3)%12));
			str.append(" ");
			for (int i = 0; i < dots; i++) {
				str.append('•');	
			}
			str.append(type);
			if (timeModification != null) {
				str.append("*");
			}
			str.append(' ');
			if (lyric == null) {
				str.append("\t");
			} else {
				str.append('\"');
				str.append(lyric.text);
				str.append('\"');
				if (lyric.syllabic == Syllabic.BEGIN || lyric.syllabic == Syllabic.MIDDLE)
					str.append('-');
				str.append(' ');
				str.append(lyric.syllableStress);
			}
			
			return str.toString();
		}

		public String toXML(int indentationLevel) {
			StringBuilder str = new StringBuilder();
			
			//open note tag
			for (int j = 0; j < indentationLevel; j++) str.append("    "); 
			str.append("<note>\n");
			
			//pitch
			if (pitch == -1) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<rest/>\n");
			} else {
				String pitchString = Pitch.getPitchName((pitch+3)%12);
				char step = pitchString.charAt(0); 
				int alter = 0;
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
			
			//type
			for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
			str.append("<type>").append(interpretNoteType(type)).append("</type>\n");
			
			//dot
			for (int i = 0; i < dots; i++) {
				for (int j = 0; j <= indentationLevel; j++) str.append("    "); 
				str.append("<dot/>\n");
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
		
	}

	
	public static class Quality {

		private static final int FLAT_SECONDi = 0, SECONDi = 1, FLAT_THIRDi = 2, THIRDi = 3, FOURTHi = 4, FLAT_FIFTHi = 5,
				FIFTHi = 6, FLAT_SIXTHi = 7, SIXTHi = 8, FLAT_SEVENTHi = 9, SEVENTHi = 10, FLAT_NINTHi = 11, 
				NINTHi = 12, SHARP_NINTHi = 13, FLAT_ELEVENTHi = 14, ELEVENTHi = 15, SHARP_ELEVENTHi = 16, 
				FLAT_THIRTEENTHi = 17, THIRTEENTHi = 18, SHARP_THIRTEENTHi = 19;

		private static final String IMPLICIT_MAJOR = "", EXPLICIT_MAJOR = "M", MINOR = "m", MINOR_MAJOR = "mM", 
				DOMINANT = "", HALF_DIMINISHED = "ø", DIMINISHED = "°", AUGMENTED = "+", SUSPENDED = "sus",
				POWER = "5", NO_CHORD = "N.C.", PEDAL = " pedal";
		
		private static final String SECOND = "2", THIRD = "3", FOURTH = "4",  
				FOURSEVEN = "47", FLAT_FIFTH = "b5", FIFTH = "5", SHARP_FIFTH = "#5", SIXTH = "6", 
				SEVENTH = "7", FLAT_NINTH = "b9", NINTH = "9", SHARP_NINTH = "#9", 
				ELEVENTH = "11", SHARP_ELEVENTH = "#11", FLAT_THIRTEENTH = "b13", THIRTEENTH = "13", 
				SHARP_THIRTEENTH = "#13", SIXNINE = "69";

		private static final String ADD = "add", ALTER = "", SUBTRACT = "sin"; 
		
		boolean[] notesOn = new boolean[SHARP_THIRTEENTHi+1];
		// as per https://en.wikipedia.org/wiki/Chord_names_and_symbols_(popular_music)
		String quality = IMPLICIT_MAJOR;
		String interval = null;
		String alteredFifth = null;
		String additionalIntervalInstruction = null;
		String additionalInterval = null;
		
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
					quality = IMPLICIT_MAJOR;
					// do nothing
				} else if (text.equals("°") || text.equals("dim")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					notesOn[FIFTHi] = false;
					notesOn[FLAT_FIFTHi] = true;
					quality = DIMINISHED;
				} else if (text.equals("5b")) {
					notesOn[FIFTHi] = false;
					notesOn[FLAT_FIFTHi] = true;
					alteredFifth = "5b";
				} else if (text.equals("m")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					quality = MINOR;
				} else if (text.equals("7")) {
					notesOn[FLAT_SEVENTHi] = true;
					quality = DOMINANT;
					interval = SEVENTH;
				} else if (text.equals("m7")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					quality = MINOR;
					interval = SEVENTH;
				} else if (text.equals("sus7")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = false;
					notesOn[FOURTHi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					quality = SUSPENDED;
					interval = FOURSEVEN;
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
				quality = MINOR;
				text = text.substring(5);
				if (text.isEmpty()) {
					return true;
				} else if (text.equals("-major")) {
					notesOn[SEVENTHi] = true;
					quality = MINOR_MAJOR;
					interval = SEVENTH;
				} else if (text.equals("-seventh")) {
					notesOn[FLAT_SEVENTHi] = true;
					interval = SEVENTH;
				} else if (text.equals("-sixth")) {
					notesOn[SIXTHi] = true;
					interval = SIXTH;
				} else if (text.equals("-ninth")){
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[NINTHi] = true;
					interval = NINTH;
				} else if (text.equals("-11th")){
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					interval = ELEVENTH;
				} else if (text.equals("-13th")){
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					notesOn[THIRTEENTHi] = true;
					interval = THIRTEENTH;
				} else {
					throw new RuntimeException("Unknown chord: \"minor" + text + "\"");
				}
			} else if (text.startsWith("major")) {
				text = text.substring(5);
				if (text.isEmpty()) {
					quality = IMPLICIT_MAJOR;
					return true;
				} else if (text.equals("-minor")) {
					notesOn[THIRDi] = false;
					notesOn[FLAT_THIRDi] = true;
					notesOn[SEVENTHi] = true;
					quality = MINOR_MAJOR;
					interval = SEVENTH;
				} else if (text.equals("-sixth")) {
					notesOn[SIXTHi] = true;
					quality = IMPLICIT_MAJOR;
					interval = SIXTH;
				} else if (text.equals("-sixnine")) {
					notesOn[SIXTHi] = true;
					notesOn[NINTHi] = true;
					quality = IMPLICIT_MAJOR;
					interval = SIXNINE;
				} else if (text.equals("-seventh")) {
					notesOn[SEVENTHi] = true;
					quality = EXPLICIT_MAJOR;
					interval = SEVENTH;
				} else if (text.equals("-ninth")) {
					notesOn[SEVENTHi] = true;
					notesOn[NINTHi] = true;
					quality = EXPLICIT_MAJOR;
					interval = NINTH;
				} else if (text.equals("-11th")) {
					notesOn[SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					quality = EXPLICIT_MAJOR;
					interval = ELEVENTH;
				} else if (text.equals("-13th")) {
					notesOn[SEVENTHi] = true;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					notesOn[THIRTEENTHi] = true;
					quality = EXPLICIT_MAJOR;
					interval = THIRTEENTH;
				} else {
					throw new RuntimeException("Unknown chord: \"major" + text + "\"");
				}
			} else if (text.startsWith("dominant")) {
				notesOn[FLAT_SEVENTHi] = true;
				text = text.substring(8);
				quality = DOMINANT;
				if (text.equals("") || text.equals("-seventh")) {
					interval = SEVENTH;
					return true;
				} else if (text.equals("-ninth")) {
					interval = NINTH;
					notesOn[NINTHi] = true;
				} else if (text.equals("-11th")) {
					interval = ELEVENTH;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
				} else if (text.equals("-13th")) {
					interval = THIRTEENTH;
					notesOn[NINTHi] = true;
					notesOn[ELEVENTHi] = true;
					notesOn[THIRTEENTHi] = true;
				} else {
					throw new RuntimeException("Unknown chord: dominant" + text);
				}
			} else if (text.startsWith("augmented")) {
				text = text.substring(9);
				quality = AUGMENTED;
				if (text.isEmpty()) {
					return true;
				} else if (text.equals("-seventh")) {
					notesOn[FIFTHi] = false;
					notesOn[FLAT_SIXTHi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					interval = SEVENTH;
				} else if (text.equals("-ninth")) {
					notesOn[FLAT_SEVENTHi] = true;
					notesOn[SHARP_NINTHi] = true;
					interval = NINTH;
				} else {
					throw new RuntimeException("Unknown chord: augmented" + text);
				}
			} else if (text.startsWith("suspended")) {
				notesOn[THIRDi] = false;
				notesOn[FLAT_THIRDi] = false;
				quality = SUSPENDED;
				text = text.substring(9);
				if (text.equals("-fourth")) {
					notesOn[FOURTHi] = true;
					interval = FOURTH;
				} else if (text.equals("-second")) {
					notesOn[SECONDi] = true;
					interval = SECOND;
				} else if (text.equals("-fourseven")) {
					notesOn[FOURTHi] = true;
					notesOn[FLAT_SEVENTHi] = true;
					interval = FOURSEVEN;
				} else {
					throw new RuntimeException("Unknown chord: dominant" + text);
				}
			} else if (text.startsWith("diminished")) {
				notesOn[THIRDi] = false;
				notesOn[FLAT_THIRDi] = true;
				notesOn[FIFTHi] = false;
				notesOn[FLAT_FIFTHi] = true;
				quality = DIMINISHED;
				text = text.substring(10);
				if (text.isEmpty()) {
					return true;
				} else if (text.equals("-seventh")) {
					notesOn[SIXTHi] = true;
					interval = SEVENTH;
				} else {
					throw new RuntimeException("Unknown chord: diminished" + text);
				}
			} else if (text.startsWith("half-diminished")) {
				notesOn[THIRDi] = false;
				notesOn[FLAT_THIRDi] = true;
				notesOn[FIFTHi] = false;
				notesOn[FLAT_FIFTHi] = true;
				notesOn[FLAT_SEVENTHi] = true;
				quality = HALF_DIMINISHED;
				text = text.substring(15);
				if (text.isEmpty()) {
					return true;
				} else {
					throw new RuntimeException("Unknown chord: half-diminished" + text);
				}
			} else if (text.startsWith("power")) {
				notesOn[THIRDi] = false;
				quality = POWER;
				text = text.substring(5);
				if (text.isEmpty()) {
					return true;
				} else {
					throw new RuntimeException("Unknown chord: power" + text);
				}
			} else if (text.equals("none")) {
				notesOn = null;
				quality = NO_CHORD;
			} else if (text.startsWith("/")) {
				// do nothing, quality does not parse bass notes
			} else if (text.equals("pedal")) {
				notesOn[THIRDi] = false;
				notesOn[FIFTHi] = false;
				quality = PEDAL;
			} else if (text.isEmpty() || text.equals("other")) {
				return false;
			} else {
				throw new RuntimeException("KIND CONTENT:" + text);
			}
			return true;
		}

		public void parseDegreeNode(Node node) {
			NodeList children = node.getChildNodes();
			int degreeValue = -1;
			int degreeAlter = 0;
			DegreeType degreeType = null;
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
						additionalIntervalInstruction = ADD;
					} else if (type.equals("alter")) {
						degreeType = DegreeType.ALTER;
						additionalIntervalInstruction = ALTER;
					} else if (type.equals("subtract")) {
						degreeType = DegreeType.SUBTRACT;
						additionalIntervalInstruction = SUBTRACT;
					} else {
						throw new RuntimeException("Unknown degree-type:" + type);
					}
				} else {
					MusicXMLAnalyzer.printNode(node, System.err);
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
			additionalInterval = getNoteInterval(degreeValue, degreeAlter);
		}

		private String getNoteInterval(int degreeValue, int degreeAlter) {
			switch(degreeValue) {
			case 2:
				if (degreeAlter == -1)
					throw new RuntimeException("flat " + degreeValue + "?");
				else if (degreeAlter == 1)
					throw new RuntimeException("sharp " + degreeValue + "?");
				return SECOND;
			case 3:
				if (degreeAlter == -1)
					throw new RuntimeException("flat " + degreeValue + "?");
				else if (degreeAlter == 1)
					throw new RuntimeException("sharp " + degreeValue + "?");
				return THIRD;
			case 4:
				if (degreeAlter == -1)
					throw new RuntimeException("flat " + degreeValue + "?");
				else if (degreeAlter == 1)
					throw new RuntimeException("sharp " + degreeValue + "?");
				return FIFTH;
			case 5:
				if (degreeAlter == -1)
					return FLAT_FIFTH;
				else if (degreeAlter == 1)
					return SHARP_FIFTH;
				return FIFTH;
			case 7:
				if (degreeAlter == -1)
					throw new RuntimeException("flat " + degreeValue + "?");
				else if (degreeAlter == 1)
					throw new RuntimeException("sharp " + degreeValue + "?");
				return SEVENTH;
			case 9:
				if (degreeAlter == -1)
					return FLAT_NINTH;
				else if (degreeAlter == 1)
					return SHARP_NINTH;
				return NINTH;
			case 11:
				if (degreeAlter == -1)
					throw new RuntimeException("flat " + degreeValue + "?");
				else if (degreeAlter == 1)
					return SHARP_ELEVENTH;
				return ELEVENTH;
			case 13:
				if (degreeAlter == -1)
					return FLAT_THIRTEENTH;
				else if (degreeAlter == 1)
					return SHARP_THIRTEENTH;
				return THIRTEENTH;
			default:
				throw new RuntimeException("Unhandled degree value:" + degreeValue);
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
				return FIFTHi;
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
			if (quality != null)
				builder.append(quality);
			if (interval != null)
				builder.append(interval);
			if (alteredFifth != null)
				builder.append(alteredFifth);
			if (additionalIntervalInstruction != null)
				builder.append(additionalIntervalInstruction);
			if (additionalInterval != null)
				builder.append(additionalInterval);
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((additionalInterval == null) ? 0 : additionalInterval.hashCode());
			result = prime * result
					+ ((additionalIntervalInstruction == null) ? 0 : additionalIntervalInstruction.hashCode());
			result = prime * result + ((alteredFifth == null) ? 0 : alteredFifth.hashCode());
			result = prime * result + ((interval == null) ? 0 : interval.hashCode());
			result = prime * result + Arrays.hashCode(notesOn);
			result = prime * result + ((quality == null) ? 0 : quality.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Quality))
				return false;
			Quality other = (Quality) obj;
			if (additionalInterval == null) {
				if (other.additionalInterval != null)
					return false;
			} else if (!additionalInterval.equals(other.additionalInterval))
				return false;
			if (additionalIntervalInstruction == null) {
				if (other.additionalIntervalInstruction != null)
					return false;
			} else if (!additionalIntervalInstruction.equals(other.additionalIntervalInstruction))
				return false;
			if (alteredFifth == null) {
				if (other.alteredFifth != null)
					return false;
			} else if (!alteredFifth.equals(other.alteredFifth))
				return false;
			if (interval == null) {
				if (other.interval != null)
					return false;
			} else if (!interval.equals(other.interval))
				return false;
			if (!Arrays.equals(notesOn, other.notesOn))
				return false;
			if (quality == null) {
				if (other.quality != null)
					return false;
			} else if (!quality.equals(other.quality))
				return false;
			return true;
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
			str.append("<harmony default-y=\"25\" print-frame=\"no\" relative-x=\"8\">\n");
			
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
				str.append("<kind text=\"").append(quality).append("\">").append(quality).append("</kind>\n");
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
	}

	public static class Root {
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

	public class MusicXMLEvent {
		int measure;
		double measureOffset;
		int repitition;
		int key;
		String harmony;
		String note;
		int syllableStress;
	}

	Document xml;

	public MusicXML(Document xml) {
		this.xml = xml;
	}

	public String toMelogenString() {
		StringBuilder str = new StringBuilder();

		List<MusicXMLEvent> allEventsChronologically = getAllEventsChronologically(true);
		if (allEventsChronologically == null) 
			return null;
		for (MusicXMLEvent events : allEventsChronologically){
			
		}
			
		return str.toString();
	}

	// this set is to help verify the assertion that there is only one instrument per leadsheet
	private Set<String> instruments = null;
	private static int unparseableHarmonies = 0;
	private List<MusicXMLEvent> getAllEventsChronologically(boolean followRepeats) {
		List<MusicXMLEvent> events = new ArrayList<MusicXMLEvent>();
		List<Node> measures = MusicXMLAnalyzer.getMeasuresForPart(this,0);

		// Harmony indexed by measure and by the offset (in divisions) from the beginning of the measure
		Map<Integer, Map<Integer, List<Harmony>>> harmonyByMeasure = new TreeMap<Integer, Map<Integer,List<Harmony>>>();
		// notes indexed by measure and by the offset (in divisions) from the beginning of the measure
		// this data structure represents order as regards repeats, but has no (simple) way of looking at how many times a note has been repeated or what the lyric for the last repetition was
		List<Triple<Integer, Integer, Note>> notesByMeasure = new ArrayList<Triple<Integer, Integer, Note>>();
		// TODO: account for global structure
		// For each offset in each measure, we keep track of how many times we've visited a note at that offset in that measure and what the previous notelyric was for the note
		// note that this data structure has no sense of order as regards repeats, but can easily check the number of times it has been repeated and the previous notelyric
		Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo = new TreeMap<Integer, Map<Integer, Pair<Integer, NoteLyric>>>();
		
		boolean hasLyrics = false;
		instruments = new HashSet<String>();
		Map<Integer,Time> timeByMeasure = new TreeMap<Integer,Time>(); 
		Time currTime = null;
		Map<Integer,Key> keyByMeasure = new TreeMap<Integer,Key>(); 
		Key currKey = null;
		Map<Integer,Integer> divsByMeasure = new TreeMap<Integer,Integer>(); 
		int currDivisionsPerQuarterNote = -1;
		Stack<Integer> forwardRepeatMeasures = new Stack<Integer>();
		int smallestNoteType = 0;
		int nextMeasure = -1;
		int prevPlayedMeasure = -1;
		int segno = -1;
		int coda1 = findDirectionTypeStartingFrom(measures, DirectionType.CODA1, measures.size());
		int coda2 = (coda1 == -1? -1 : findDirectionTypeStartingFrom(measures, DirectionType.CODA1, coda1));
		int dsalcoda = findDirectionTypeStartingFrom(measures, DirectionType.DS_AL_CODA1, measures.size());
		int dcalcoda = findDirectionTypeStartingFrom(measures, DirectionType.DC_AL_CODA, measures.size());
		if (coda1 != -1 && dsalcoda == -1 && dcalcoda == -1) {
			System.err.println("Found coda at measure " + coda1 + ", but no D.S. al Coda. Assuming it directly precedes coda");
			dsalcoda = coda1 - 1;
			return null; // not gonna deal with it.
		}
		if (coda1 == -1 && (dsalcoda != -1 || dcalcoda != -1)) {
			System.err.println("Found ds or dc at measure " + (dsalcoda == -1 ? dcalcoda : dsalcoda) + ", but no coda. Assuming it directly follows ds");
			coda1 = (dsalcoda == -1 ? dcalcoda : dsalcoda) + 1;
//			return null; // not gonna deal with it.
		}
		if (coda1 != -1 && (dsalcoda != -1 || dcalcoda != -1) && coda1 <= (dsalcoda == -1 ? dcalcoda : dsalcoda)) {
			return null;
		}
		int maxMeasure = -1;
		boolean followCoda = false;
		boolean followCoda2 = false;
		boolean endAtFine = false;
		boolean skippingEnding = false;
		int loops = 0;
		boolean barMarkedBeforeNotes = false;
		for (int i = 0; i < measures.size(); i = (nextMeasure != -1 ? nextMeasure : i + 1)) {
			loops++;
			if (loops > 1000) {
				throw new RuntimeException("Excessive loops");
			}
			if (i > maxMeasure) {
				maxMeasure = i;
			}
			nextMeasure = -1;
			int currMeasurePositionInDivisions = 0; // in divisions
			Node measure = measures.get(i);
			try {
				NodeList mChildren = measure.getChildNodes();
				for (int j = 0; j < mChildren.getLength(); j++) {
					Node mChild = mChildren.item(j);
					if (mChild instanceof Text) continue;
					String nodeName = mChild.getNodeName();
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
							if (!measureOffsetInfo.containsKey(i))
								forwardRepeatMeasures.push(i);
						} else if (direction == RepeatDirection.BACKWARD) {
							int takeRepeat = 0;
							final int LAST_TIME = 2;
							if (skippingEnding) { 
								takeRepeat = 0;
							} else if (numbers == null) {
								if (!measureOffsetInfo.containsKey(i)) {
									takeRepeat = LAST_TIME;
									barMarkedBeforeNotes = true;
								} else  if (barMarkedBeforeNotes){
									takeRepeat = 0;
								} else {
									takeRepeat = measureOffsetInfo.get(i).get(0).getFirst() == 1 ? LAST_TIME : 0;
								}
								if (takeRepeat > 0) System.err.println("Repeating at measure " + i);
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
								System.err.println("Encountered endings " + Arrays.toString(numbers) + " in measure " + i);
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
								divsByMeasure.put(i, currDivisionsPerQuarterNote);
								System.err.println("Divs per quarter note:" +currDivisionsPerQuarterNote);
							} else if (gNodeName.equals("key")) {
								currKey = parseKey(mGrandchild);
								if (currKey.mode == null) {
									songsWithMissingMode.put(MusicXMLAnalyzer.getTitle(this),currKey.fifths);
								}
								keyByMeasure.put(i, currKey);
							} else if (gNodeName.equals("time")) {
								currTime = parseTime(mGrandchild);
								System.err.println("Current time sig:" +currTime);
								timeByMeasure.put(i, currTime);
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
							if (gNodeName.equals("direction-type")) {
								DirectionType type = DirectionType.parseDirectionType(mGrandchild);
								if (type == DirectionType.DS_AL_CODA1) {
									if (followCoda) {
										System.err.println("Detected loop (found D.S. twice without coda");
										return null; // malformatted, already looking for coda and didn't find it.
									}
									nextMeasure = (segno == -1 ? 0 : segno);
									System.err.println("Found D.S. al Coda — skipping back to measure " + nextMeasure + " (coda=" + coda1 + ")");
									followCoda = true;
								} else if (type == DirectionType.DS_AL_CODA2) {
									if (followCoda2) {
										System.err.println("Detected loop (found D.S. twice without coda");
										return null;
									}
									nextMeasure = (segno == -1 ? 0 : segno);
									System.err.println("Found D.S. al Coda 2 — skipping back to measure " + nextMeasure);
									followCoda2 = true;
								} else if (type == DirectionType.DS_AL_FINE) {
									if (endAtFine) {
										System.err.println("Detected loop (found D.S. twice without coda");
										return null;
									}
									nextMeasure = (segno == -1 ? 0 : segno);
									System.err.println("Found D.S. al Fine — skipping back to measure " + nextMeasure);
									endAtFine = true;
								} else if (type == DirectionType.DC_AL_FINE) {
									if (endAtFine) {
										System.err.println("Detected loop (found D.C. twice without coda");
										return null;
									}
									nextMeasure = 0;
									System.err.println("Found D.C. al Fine — skipping back to measure " + nextMeasure);
									endAtFine = true;
								}  else if (type == DirectionType.DC_AL_CODA) {
									if (followCoda) {
										System.err.println("Detected loop (found D.C. twice without coda");
										return null;
									}
									if (nextMeasure == -1) {
										nextMeasure = 0;
										System.err.println("Found D.C. al Coda in measure " + i + " — skipping back to measure " + nextMeasure);
										followCoda = true;
									}
								} else if (type == DirectionType.SEGNO) {
									segno = i;
									System.err.println("Found segno at measure " + segno);
								} else if (type == DirectionType.IGNORE) { 
									// do nothing
								} else if (type == DirectionType.CODA1) { 
									// skip to coda
									if (followCoda) {
										nextMeasure = coda1;
										System.err.println("Found coda at measure " + i + ", skipping to " + nextMeasure);
										followCoda = false;
									}
								} else if (type == DirectionType.CODA2) { 
									// skip to coda
									if (followCoda2) {
										nextMeasure = coda2;
										System.err.println("Found coda 2 at measure " + i + ", skipping to " + nextMeasure);
										followCoda = false;
									}
								} else if (type == DirectionType.FINE) { 
									// skip to coda
									if (endAtFine) {
										nextMeasure = measures.size();
										System.err.println("Found fine at measure " + i + ", skipping to " + nextMeasure);
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
						addHarmonyToMeasure(i, measurePositionInDivisions, harmony, harmonyByMeasure);
					} else if (nodeName.equals("note")) {
						Note note = parseNote(mChild, 1+getRepeatForMeasureOffset(measureOffsetInfo, i, currMeasurePositionInDivisions));
						if (note == null) continue;
						if (note.type > smallestNoteType) {
							smallestNoteType = note.type;
						}
						if (note.lyric != null) {
							hasLyrics = true;
						}
						
						if (!note.isChordWithPrevious) {
							addNoteToMeasure(i, currMeasurePositionInDivisions, note, notesByMeasure, measureOffsetInfo);
							currMeasurePositionInDivisions += note.duration;
						}
					} else if (nodeName.equals("print")) {
						// TODO
					} else if (nodeName.equals("sound")) {
						// TODO
					} else if (nodeName.equals("backup")) {
						if (currMeasurePositionInDivisions == calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote)) {
							break;
						} else {
//							throw new RuntimeException("backup occurred mid-measure, which we don't handle");
							return null;
						}
					} else if (nodeName.equals("forward")) {
						int duration = Integer.parseInt(((Element)mChild).getElementsByTagName("duration").item(0).getTextContent());
						Note note = new Note(-1,duration,-1,null,0,NoteTie.NONE,null,false);
						currMeasurePositionInDivisions += note.duration;
						if (currMeasurePositionInDivisions == calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote)) {
							break;
						} else {
//							throw new RuntimeException("backup occurred mid-measure, which we don't handle");
							return null;
						}
					} else {
						MusicXMLAnalyzer.printNode(mChild, System.err);
						throw new RuntimeException("mChild with unrecognized name:" + nodeName);
					}
				}
				if (instruments.size() > 1) {
					throw new RuntimeException("measure introduces second instrument");
				} else if (!skippingEnding && i != 0 && i != measures.size()-1 && currMeasurePositionInDivisions != calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote)) {
//					MusicXMLAnalyzer.printNode(measure, System.err);
//					throw new RuntimeException("events in measure " + i + " fill " + currMeasurePositionInDivisions + " of " + calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote) + " divisions");
//					System.err.println("Current Time Signature = " + currTime);
					return null;
				}
			} catch (RuntimeException e) {
				MusicXMLAnalyzer.printNode(measure, System.out);
				System.err.println("In measure number " + i);
				throw e;
			}
			if (!skippingEnding)
				prevPlayedMeasure = i;
			if (!skippingEnding && i == dsalcoda) {
				nextMeasure = (segno == -1 ? 0 : segno);
				System.err.println("Found D.S. al Coda in measure " + i + " — skipping back to measure " + nextMeasure);
				followCoda = true;
			}
		}
		
		Integer distribution = smallestNoteTypePerSong.get(smallestNoteType);
		if (distribution == null) {
			smallestNoteTypePerSong.put(smallestNoteType,1);
		} else {
			smallestNoteTypePerSong.put(smallestNoteType,distribution+1);
		}
		
		//Resolve co-occuring harmonies
		Map<Integer, Map<Integer, Harmony>> unoverlappingHarmonyByMeasure = resolveOverlappingHarmonies(harmonyByMeasure, measureOffsetInfo, timeByMeasure, divsByMeasure);
		
		// print harmonies
//		for (Entry<Integer, Map<Integer, Harmony>> first : unoverlappingHarmonyByMeasure.entrySet()) {
//			for (Entry<Integer, Harmony> second : first.getValue().entrySet()) {
//				System.out.println(first.getKey() + "\t" + second.getKey() + "\t" + second.getValue());
//			}
//		}
		
		// print notes with harmonies
		if (unoverlappingHarmonyByMeasure.isEmpty()) {
			System.err.println("no chords");
			return null;
		}
		
		// ADD SYLLABLE STRESS
		if(!addStressToSyllables(notesByMeasure)) {
			return null;
		}
		
		// ELUCIDATE SEGMENT STRUCTURE AND RESOLVE REPEATS IN LYRICS
		
		// DETECT PATTERNS IN SUB-REPETITION
		
		//print pitch (-1 if rest) and chord (-1 if no chord) for each 16th note in a 256-len sequence
		int interval = 16;
		int len_measures = 4;
		
		// only 4/4 examples
		System.err.println("checking time sig and key sig");
		if (timeByMeasure.size() != 1) return null;
		if (!((currTime.beats == 4 && currTime.beatType == 4) || (currTime.beats == 2 && currTime.beatType == 2))) return null;
		if (keyByMeasure.size() != 1) return null;
		int len = currTime.beats * len_measures * interval / 4;
		
		List<String> tokens = new ArrayList<String>();
		List<String> tokens2 = new ArrayList<String>();
		// normalize by key signature, include start token (%.%)
		String START = "%.%";
		tokens.add(START);
		tokens2.add(START);
		
		for (Triple<Integer, Integer, Note> triple : notesByMeasure) {
			Integer measure = triple.getFirst();
			final Integer divsPerQuarter = divsAtMeasure(divsByMeasure, measure);
			Double divsPerInterval = divsPerQuarter / (interval/4.0);
			double divsPerMeasure = calculateTotalDivisionsInMeasure(currTime, divsPerQuarter);
			Integer divOffset = triple.getSecond();
			
			final Note note = triple.getThird();
			for (double j = 0.; j < divsPerMeasure; j += divsPerInterval) {
				if (j >= divOffset && j < divOffset + note.duration) {
					tokens.add("" + normalizePitch(note.pitch, currKey));
					tokens2.add("" + normalizeHarmony(getCurrHarmony(unoverlappingHarmonyByMeasure,measure,(int) j), currKey));
				}
			}
			
			System.out.println(measure + "\t" + divOffset + "\t" + note + "\t" + getCurrHarmony(unoverlappingHarmonyByMeasure,measure,divOffset));
		}
		
//		for (int seqOffset = 0; seqOffset < tokens.size(); seqOffset++) {
//			tmpDataWriter.print(tokens.get(seqOffset));
//			tmp2DataWriter.print(tokens2.get(seqOffset));
//			tmpDataWriter.print(" ");
//			tmp2DataWriter.print(" ");
//		}
//		tmpDataWriter.println();
//		tmp2DataWriter.println();
//		for (int seqOffset = 0; seqOffset < tokens.size()-len; seqOffset++) {
//			for (int pos = seqOffset; pos < seqOffset+len; pos++) {
//				tmpDataWriter.print(tokens.get(pos));
//				tmpDataWriter.print(" ");
//			}
//			tmpDataWriter.println();
//		}
		
		return events;
	}

	private boolean addStressToSyllables(List<Triple<Integer, Integer, Note>> notesByMeasure) {
		List<NoteLyric> currentWordNotes = null;
		int totalSyllablesWithStress = 0;
		int totalSyllables = 0;
		List<StressedPhone[]> phones;
		for (Triple<Integer,Integer,Note> triple : notesByMeasure) {
			System.out.println(triple);
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
					System.err.println("" + phones.size() + " entries in phone dict for \"" + currNoteLyric.text + "\"");
				}
				currentWordNotes = null;
				continue;
			}
			switch(currNoteLyric.syllabic) {
			case BEGIN:
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
						System.err.println("" + currentWordNotes.size() + " notes mismatch with " + syllables.size() + " syllables:" + word);
					}
				} else {
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
					System.err.println("" + phones.size() + " entries in phone dict for \"" + currNoteLyric.text + "\"");
				}
				currentWordNotes = null;
				break;
			default:
				//check elision and extend
				break;
			
			}
		}
		System.err.println("For " + notesByMeasure.size() + " notes with " + totalSyllables + " syllables, " + totalSyllablesWithStress + " had stress info from phone dict");
		return totalSyllables > .1 * notesByMeasure.size() && ((double)totalSyllablesWithStress)/totalSyllables > .25;
	}

	private boolean allPhonesHaveSameSyllablesAndStress(String word, List<StressedPhone[]> phones) {
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

	private Harmony normalizeHarmony(Harmony currHarmony, Key currKey) {
		if (currHarmony == null || currHarmony.root == null || currHarmony.root.rootStep == Pitch.NO_KEY || currKey.fifths == 0) return currHarmony;
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

	private int normalizePitch(int pitch, Key currKey) {
		if (pitch < 0) return pitch;
		
		int modification = (7*currKey.fifths + 144) % 12;
		if (modification > 6) {
			modification -= 12;
		}
		return (pitch - modification);
	}

	private int findDirectionTypeStartingFrom(List<Node> measures, DirectionType directionType, int startPoint) {
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
							DirectionType type = DirectionType.parseDirectionType(mGrandchild);
							if (type == directionType) { 
								return i;
							} 
						} 
					}
				} 
			}
		}
		return -1;
	}

	private Barline parseBarline(Node node) {
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

	private Harmony getCurrHarmony(Map<Integer, Map<Integer, Harmony>> harmonyByMeasure, Integer measure,
			Integer divOffset) {
		Harmony lastHarmony = null;
		for (Integer lastMeasure : harmonyByMeasure.keySet()) {
			if (lastMeasure > measure) {
				return lastHarmony;
			}
			
			Map<Integer, Harmony> harmonyByOffset = harmonyByMeasure.get(lastMeasure);
			for (Integer lastDivOffset : harmonyByOffset.keySet()) {
				if (lastMeasure == measure && lastDivOffset > divOffset) {
					return lastHarmony;
				}
				lastHarmony = harmonyByOffset.get(lastDivOffset);
			}
			
		}
		
		assert lastHarmony != null;
		return lastHarmony;
	}

	private int getRepeatForMeasureOffset(Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo, int measure,
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

	private Map<Integer, Map<Integer, Harmony>> resolveOverlappingHarmonies(Map<Integer, Map<Integer, List<Harmony>>> harmonyByMeasure, 
			Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo, Map<Integer,Time> timeByMeasure, Map<Integer, Integer> divsByMeasure) {
		Map<Integer, Map<Integer, Harmony>> unoverlappingHarmonyByMeasure = new TreeMap<Integer, Map<Integer, Harmony>>(); 
		// If there are multiple chords, they must all occur before the next note occurs after the first chord,
		// otherwise they'd have marked the chord later.
		
		for (Integer measure : harmonyByMeasure.keySet()) {
			Map<Integer, List<Harmony>> harmoniesByOffset = harmonyByMeasure.get(measure);
			Map<Integer, Harmony> harmonyByOffset = new TreeMap<Integer, Harmony>();
			unoverlappingHarmonyByMeasure.put(measure, harmonyByOffset);
			for (Integer offset : harmoniesByOffset.keySet()) {
				List<Harmony> harmonies = harmoniesByOffset.get(offset);
				if (harmonies.size() == 1) {
					harmonyByOffset.put(offset, harmonies.get(0));
				} else {
					Time currTime = timeAtMeasure(timeByMeasure,measure);
					int totalDivisionsInCurrMeasure = calculateTotalDivisionsInMeasure(currTime,divsAtMeasure(divsByMeasure,measure));
					Integer offsetLimit = Integer.min(nextOccurringNoteEvent(measureOffsetInfo, measure, offset),
							totalDivisionsInCurrMeasure);
					harmonyByOffset.putAll(resolveOverlappingHarmonies(harmonies, offset, offsetLimit, totalDivisionsInCurrMeasure/currTime.beats, currTime));
				}
			}
		}
		
		return unoverlappingHarmonyByMeasure;
	}

	private Integer divsAtMeasure(Map<Integer, Integer> divsByMeasure, Integer measure) {
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

	private Time timeAtMeasure(Map<Integer, Time> timeByMeasure, Integer measure) {
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
	
	private Map<Integer, Harmony> resolveOverlappingHarmonies(List<Harmony> harmonies, Integer startOffset, Integer offsetLimit, int divsPerBeat, Time currTime) {
		Map<Integer, Harmony> harmonyByOffset = new TreeMap<Integer, Harmony>();
		harmonyByOffset.put(startOffset, harmonies.remove(0));

		List<Integer> validChordOffsets = null;
		
		validChordOffsets = getValidChordOffsets(startOffset+1, offsetLimit, divsPerBeat, currTime, harmonies.size());
		
//		assert validChordOffsets.size() != 0 : "no valid chord positions in interval";
		while (validChordOffsets.size() < harmonies.size()) {
			System.err.println("insufficient measure divisions for overlapping chord resolution");
			harmonies.remove(0);
			removedChordsCount++;
		}
		
		for (int i = 1; i < harmonies.size(); i++) {
			harmonyByOffset.put(validChordOffsets.get(validChordOffsets.size()-i), harmonies.get(harmonies.size()-i));
		}
		
		return harmonyByOffset;
	}

	
	private List<Integer> getValidChordOffsets(Integer startOffset, Integer offsetLimit, int divsPerBeat, Time currTime, int itemsToAdd) {
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

	private Integer nextOccurringNoteEvent(
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
	private void addNoteToMeasure(int measureNumber, int currMeasurePositionInDivisions, Note note,
			List<Triple<Integer, Integer, Note>> notesByMeasure, Map<Integer, Map<Integer, Pair<Integer, NoteLyric>>> measureOffsetInfo) {
		// add note to the play order
		notesByMeasure.add(new Triple<Integer, Integer, Note>(measureNumber, currMeasurePositionInDivisions, note));
		
		// adjust how many times this note has been seen and whether the lyrics are the same across all repeats
		Map<Integer, Pair<Integer, NoteLyric>> offsetInfo = measureOffsetInfo.get(measureNumber);
		if (offsetInfo == null) {
			offsetInfo = new TreeMap<Integer, Pair<Integer, NoteLyric>>();
			measureOffsetInfo.put(measureNumber, offsetInfo);
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

	private void addHarmonyToMeasure(int measureNumber, Integer offset, Harmony harmony, Map<Integer, Map<Integer, List<Harmony>>> harmonyByMeasure) {
		Map<Integer, List<Harmony>> harmonyByOffset = harmonyByMeasure.get(measureNumber);
		if (harmonyByOffset == null) {
			harmonyByOffset = new TreeMap<Integer, List<Harmony>>();
			harmonyByMeasure.put(measureNumber, harmonyByOffset);
		}
		// We use a list in case multiple harmonies co-occur; these will be resolved (i.e., evenly distributed) later 
		List<Harmony> harmonies = harmonyByOffset.get(offset);
		if (harmonies == null) {
			harmonies = new ArrayList<Harmony>();
			harmonyByOffset.put(offset, harmonies);
		} else { 
			if (harmonies.get(harmonies.size()-1).equals(harmony)) {
				return;
			}
		}
		harmonies.add(harmony);
	}

	private int calculateTotalDivisionsInMeasure(Time currTime, int currDivisionsPerQuarterNote) {
		// how many quarter notes?
		double quarterNoteCount = currTime.beats * 4.0 / currTime.beatType;
		int totalDivisionsInMeasure = (int) Math.round(quarterNoteCount * currDivisionsPerQuarterNote);
		return totalDivisionsInMeasure;
	}

	private void parseMeasureStyle(Node node) {
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

	private static final int REST = -2;
	private static final int UNPITCHED_RHYTHM = -3;
	private Note parseNote(Node node, int verse) {
		int pitch = -1;
		int duration = -1;
		int type = -1;
		NoteLyric lyric = null;
		int dots = 0;
		NoteTie tie = NoteTie.NONE;
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
				int currVerse = Integer.parseInt(child.getAttributes().getNamedItem("number").getTextContent());
				if (currVerse == verse)
					lyric = parseNoteLyric(child);
			} else if (childName.equals("dot")) {
				dots++;
			} else if (childName.equals("tie")) {
				tie = NoteTie.parse(child.getAttributes().getNamedItem("type").getTextContent());
			} else if (childName.equals("notations")) {
				// do nothing (this is for visualizing music)
			} else if (childName.equals("staff")) {
				// do nothing (this is for visualizing music)
			} else if (childName.equals("instrument")) {
				// do nothing (we only have one instrument)
				instruments.add(child.getAttributes().getNamedItem("id").getTextContent());
			} else if (childName.equals("rest")) {
				pitch = REST;
			} else if (childName.equals("time-modification")) {
				timeModification = parseNoteTimeModification(child);
			} else if (childName.equals("chord")) {
				isChordWithPreviousNote = true;
			} else if (childName.equals("grace")) {
				isGrace = true;
			} else if (childName.equals("cue")) {
				pitch = REST;
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
			MusicXMLAnalyzer.printNode(node, System.err);
			throw new RuntimeException("Note missing pitch or duration or type");
		}
		
		return new Note(pitch, duration, type, lyric, dots, tie, timeModification, isChordWithPreviousNote); 
	}
	
	private NoteTimeModification parseNoteTimeModification(Node node) {
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
				MusicXMLAnalyzer.printNode(node, System.err);
				throw new RuntimeException("Unknown child of note time modification:" + childName);
			}
		}
		
		if (actualNotes == -1 || normalNotes == -1) {
			MusicXMLAnalyzer.printNode(node, System.err);
			throw new RuntimeException("NoteTimeModification missing actual/normal notes");
		}
		
		return new NoteTimeModification(actualNotes, normalNotes, normalType, normalDot);
	}

	private NoteLyric parseNoteLyric(Node node) {
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
				MusicXMLAnalyzer.printNode(node, System.err);
				throw new RuntimeException("Unknown child of note node:" + childName);
			}
		}
		
		if (text == null && !extend) {
			MusicXMLAnalyzer.printNode(node, System.err);
			throw new RuntimeException("Note lyric missing text or syllabic");
		}
		
		return new NoteLyric(syllabic, text, extend, elision);
	}

	private int parseNotePitch(Node node) {
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
			MusicXMLAnalyzer.printNode(node, System.err);
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

	private Pair<Integer, Harmony> parseHarmony(Node node) {
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
				MusicXMLAnalyzer.printNode(node, System.err);
				throw new RuntimeException("Unknown child of harmony node:" + childName);
			}
		}
		
		if (root == null) {
			MusicXMLAnalyzer.printNode(node, System.err);
			throw new RuntimeException("Harmony missing root");
		}
		
		return new Pair<Integer, Harmony>(offset,new Harmony(root, quality, bass));
	}

	private Bass parseBass(Node node) {
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
			MusicXMLAnalyzer.printNode(node, System.err);
			throw new RuntimeException("Time node missing beats or beatType");
		}
		
		return new Bass(bassStep);
	}

	private Root parseRoot(Node node) {
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
			MusicXMLAnalyzer.printNode(node, System.err);
			throw new RuntimeException("Time node missing beats or beatType");
		}
		
		return new Root(rootStep);
	}

	static Map<String,Integer> songsWithMissingMode = new HashMap<String,Integer>();
	private Key parseKey(Node node) {
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


	private Time parseTime(Node node) {
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

	
	static PrintWriter tmpDataWriter = null;
	static PrintWriter tmp2DataWriter = null;
	public static void main(String[] args) throws ZipException, IOException, ParserConfigurationException, SAXException, TransformerException {
		int unrecoverableXML = 0;
		tmpDataWriter = new PrintWriter("/Users/norkish/Archive/2016_BYU/501R_Deep_Learning/final/pitch_data.txt");
		tmp2DataWriter = new PrintWriter("/Users/norkish/Archive/2016_BYU/501R_Deep_Learning/final/harmony_data.txt");
		File[] files = new File("/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/Wikifonia").listFiles();
		for (File file : files) {
//			 if (!file.getName().equals("Billy Joel - Just The Way You Are.mxl"))
//				 continue;
//			if (file.getName().charAt(0) < 'T') {
//				continue;
//			}
			 System.out.println(file.getName());
			 MusicXML musicXML = new MusicXML(MusicXMLAnalyzer.mxlToXML(file));
			 MusicXMLAnalyzer.printDocument(musicXML.xml, System.out);
			 String melogenString;
			 try {
				 melogenString = musicXML.toMelogenString();
			 } catch (AssertionError e) {
				 System.err.println(file.getName());
				 throw e;
			 } catch (Exception e) {
				 System.err.println(file.getName());
				 throw e;
			 }
			 if (melogenString == null)
				 unrecoverableXML++;
			 System.out.println(melogenString);
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
		tmpDataWriter.close();
		tmp2DataWriter.close();
		System.out.println("Chords removed in resolving overlapping chords:" + removedChordsCount);
		System.out.println("Songs nixed for unparseable harmony:" + unparseableHarmonies);
	}
}
