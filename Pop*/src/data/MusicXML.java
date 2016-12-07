package data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

import pitch.Pitch;
import tabcomplete.utils.Utils;
import utils.Pair;
import utils.Triple;

public class MusicXML {

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
		private boolean elision;

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

	}

	public class Note {

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
		int bassStep;
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

	public class Harmony {
		Root root;
		Quality quality;
		Bass bass;
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
			result = prime * result + getOuterType().hashCode();
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
			if (!getOuterType().equals(other.getOuterType()))
				return false;
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
		private MusicXML getOuterType() {
			return MusicXML.this;
		}
	}

	public class Root {
		int rootStep;
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
			result = prime * result + getOuterType().hashCode();
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
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (rootStep != other.rootStep)
				return false;
			return true;
		}

		private MusicXML getOuterType() {
			return MusicXML.this;
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

	public class Key {
		int fifths;
		KeyMode mode;
		public Key(int fifths, KeyMode mode) {
			this.fifths = fifths;
			this.mode = mode;
		}
	}

	public class Time {
		public int beats;
		public int beatType;
		public Time(int beats, int beatType) {
			this.beats = beats;
			this.beatType = beatType;
		}
		
		public String toString() {
			return this.beats + "/" + this.beatType;
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
		
		instruments = new HashSet<String>();
		Map<Integer,Time> timeByMeasure = new TreeMap<Integer,Time>(); 
		Time currTime = null;
		Key currKey = null;
		Map<Integer,Integer> divsByMeasure = new TreeMap<Integer,Integer>(); 
		int currDivisionsPerQuarterNote = -1;
		int smallestNoteType = 0;
		for (int i = 0; i < measures.size(); i++) {
			int currMeasurePositionInDivisions = 0; // in divisions
			Node measure = measures.get(i);
			try {
				NodeList mChildren = measure.getChildNodes();
				for (int j = 0; j < mChildren.getLength(); j++) {
					Node mChild = mChildren.item(j);
					if (mChild instanceof Text) continue;
					String nodeName = mChild.getNodeName();
					NodeList mGrandchildren = mChild.getChildNodes();
					if (nodeName.equals("attributes")) {
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
						Note note = parseNote(mChild);
						if (note == null) continue;
						if (note.type > smallestNoteType) {
							smallestNoteType = note.type;
						}
						if (!note.isChordWithPrevious) {
							addNoteToMeasure(i, currMeasurePositionInDivisions, note, notesByMeasure, measureOffsetInfo);
							currMeasurePositionInDivisions += note.duration;
						}
					} else if (nodeName.equals("print")) {
						// TODO
					} else if (nodeName.equals("barline")) {
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
				} else if (i != 0 && i != measures.size()-1 && currMeasurePositionInDivisions != calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote)) {
//					MusicXMLAnalyzer.printNode(measure, System.err);
					System.err.println("events in measure fill " + currMeasurePositionInDivisions + " of " + calculateTotalDivisionsInMeasure(currTime, currDivisionsPerQuarterNote) + " divisions");
//					System.err.println("Current Time Signature = " + currTime);
					return null;
				}
			} catch (RuntimeException e) {
				MusicXMLAnalyzer.printNode(measure, System.out);
				System.err.println("In measure number " + i);
				throw e;
			}
		}
		
		Integer distribution = smallestNoteTypePerSong.get(smallestNoteType);
		if (distribution == null) {
			smallestNoteTypePerSong.put(smallestNoteType,1);
		} else {
			smallestNoteTypePerSong.put(smallestNoteType,distribution+1);
		}
		
		//Resolve co-occuring harmonies
		int measure = -1;
		for (Entry<Integer, Map<Integer, List<Harmony>>> first : harmonyByMeasure.entrySet()) {
			for (Entry<Integer, List<Harmony>> second : first.getValue().entrySet()) {
				if (second.getValue().size() > 1) {
					System.err.println("Co-occuring chords");
					measure = first.getKey();
				}
				for (Harmony harmony : second.getValue()) {
					System.out.println(first.getKey() + "\t" + second.getKey() + "\t" + harmony);
				}
			}
		}

		Map<Integer, Map<Integer, Harmony>> unoverlappingHarmonyByMeasure = resolveOverlappingHarmonies(harmonyByMeasure, measureOffsetInfo, timeByMeasure, divsByMeasure);
		
		for (Entry<Integer, Map<Integer, Harmony>> first : unoverlappingHarmonyByMeasure.entrySet()) {
			for (Entry<Integer, Harmony> second : first.getValue().entrySet()) {
				System.out.println(first.getKey() + "\t" + second.getKey() + "\t" + second.getValue());
			}
		}
		if (measure != -1) {
			Utils.promptEnterKey("Check it out at measure " + measure);
		}
		
		return events;
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

	private Map<Integer, Harmony> resolveOverlappingHarmonies(List<Harmony> harmonies, Integer startOffset, Integer offsetLimit, int divsPerBeat, Time currTime) {
		Map<Integer, Harmony> harmonyByOffset = new TreeMap<Integer, Harmony>();
		harmonyByOffset.put(startOffset, harmonies.get(0));

		List<Integer> validChordOffsets = null;
		
		validChordOffsets = getValidChordOffsets(startOffset, offsetLimit, divsPerBeat, currTime, harmonies.size());
		
		for (Integer integer : validChordOffsets) {
			System.out.print(integer + ", ");
		}
		System.out.println(startOffset + "," + offsetLimit  + "," + divsPerBeat  + "," +currTime);
		
		assert validChordOffsets.size() >= harmonies.size() : "insufficient measure divisions for overlapping chord resolution";
		
		//TODO: More evenly distribute chords rather than just do backfill
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
				if (!prevLyric.equals(note.lyric)) {
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
	private Note parseNote(Node node) {
		int pitch = -1;
		int duration = -1;
		int type = -1;
		NoteLyric lyric = null;
		int dots = 0;
		NoteTie tie = NoteTie.NONE;
		NoteTimeModification timeModification = null;
		boolean isChordWithPreviousNote = false;
		boolean isGrace = false;
		// triplet
				
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
				lyric = parseNoteLyric(child);
				// TODO: handle multiple lyrics (i.e., verses) per note
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

	public static void main(String[] args) throws ZipException, IOException, ParserConfigurationException, SAXException, TransformerException {
		int unrecoverableXML = 0;
		File[] files = new File("/Users/norkish/Archive/2015_BYU/ComputationalCreativity/data/Wikifonia").listFiles();
		for (File file : files) {
//			 if (!file.getName().equals("Billy Joel - Piano Man.mxl"))
//				 continue;
//			if (file.getName().charAt(0) < 'M') {
//				continue;
//			}
			 System.out.println(file.getName());
			 MusicXML musicXML = new MusicXML(MusicXMLAnalyzer.mxlToXML(file));
//			 MusicXMLAnalyzer.printDocument(musicXML.xml, System.out);
			 String melogenString = musicXML.toMelogenString();
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
		
		System.out.println("Songs nixed for unparseable harmony:" + unparseableHarmonies);
	}
}
