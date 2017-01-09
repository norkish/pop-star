package orchestrate;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import segmentstructure.TimeSignature;
import utils.Triple;

public class MIDIOrchestration {

	public class MeasureTextEvent extends MeasureEvent {

		private String lyric;

		public MeasureTextEvent(double msrOffset, String text) {
			offset = msrOffset;
			lyric = text;
		}

		@Override
		int compareTo(MeasureEvent event) {
			return 0;
		}

		@Override
		String subToString() {
			return " Lyric:" + lyric;
		}
	}

	public abstract class MeasureEvent {
		public double offset;
		
		public int baseCompareTo(MeasureEvent eventToAdd) {
			if (eventToAdd.offset < this.offset)
				return 1;
			if (this.offset < eventToAdd.offset)
				return -1;
			if (this.getClass() != eventToAdd.getClass())
				return this.getClass().toString().compareTo(eventToAdd.getClass().toString());
			
			return this.compareTo(eventToAdd);
		}
		
		abstract int compareTo(MeasureEvent event);
		
		abstract String subToString();

		public String toString() {
			return "Offset:" + offset + subToString();
		}
	}

	public class MeasureNoteEvent extends MeasureEvent {

		public int pitch;
		public double duration;

		public MeasureNoteEvent(double msrOffset, int midiPitch, double dur) {
			offset = msrOffset;
			pitch = midiPitch;
			duration = dur;
		}
		
		@Override
		int compareTo(MeasureEvent e) {
			MeasureNoteEvent eventToAdd = (MeasureNoteEvent) e;
			if (eventToAdd.pitch < this.pitch)
				return 1;
			if (this.pitch < eventToAdd.pitch)
				return -1;
			
			return 0;
		}
		
		@Override
		String subToString() {
			return " Pitch:" + pitch + " Duration:" + duration;
		}
	}

	public class Measure implements Iterable<MeasureEvent> {
		public TimeSignature ts;

		List<MeasureEvent> events = new ArrayList<MeasureEvent>();

		public Measure(TimeSignature timeSignature) {
			ts = timeSignature;
		}

		public void addEvent(MeasureEvent eventToAdd) {
			int i = 0;
			int comparisonResult = -1;
			for (MeasureEvent event : events) {
				if ((comparisonResult = event.baseCompareTo(eventToAdd)) >= 0) {
					break;
				} else {
					i++;
				}
			}

			if (comparisonResult == 0 && this.getClass().equals(MeasureNoteEvent.class) && eventToAdd.getClass().equals(MeasureNoteEvent.class)) { // if same note at same time, take longer duration
				MeasureNoteEvent existingEvent = (MeasureNoteEvent) events.get(i);
				existingEvent.duration = Math.max(existingEvent.duration, ((MeasureNoteEvent) eventToAdd).duration);
			} else {
				events.add(i, eventToAdd);
			}
		}

		public String toString() {
			StringBuilder str = new StringBuilder();

			for (int i = 0; i < events.size(); i++) {
				MeasureEvent event = events.get(i);
				str.append("\n\t\tEvent ");
				str.append(i);
				str.append(" - ");
				str.append(event);
			}

			return str.toString();
		}

		@Override
		public Iterator<MeasureEvent> iterator() {
			return events.iterator();
		}
	}

	public class Track implements Iterable<Measure> {
		List<Measure> measures = new ArrayList<Measure>();

		public Track(int measureCount, TimeSignature timeSignature) {
			for (int i = 0; i < measureCount; i++) {
				measures.add(new Measure(timeSignature));
			}
		}

		public Track(Track otherTrack) {
			for (Measure otherMeasure : otherTrack.measures) {
				measures.add(new Measure(otherMeasure.ts));
			}
		}

		public void addMeasures(int measureCount, TimeSignature timeSignature) {
			for (int i = 0; i < measureCount; i++) {
				measures.add(new Measure(timeSignature));
			}
		}

		public void addEvent(int measure, MeasureEvent measureEvent) {
			measures.get(measure).addEvent(measureEvent);
		}

		public String toString() {
			StringBuilder str = new StringBuilder();

			for (int i = 0; i < measures.size(); i++) {
				Measure measure = measures.get(i);
				str.append("\n\tMeasure ");
				str.append(i);
				str.append(":");
				str.append(measure);
			}

			return str.toString();
		}

		int iterPtrPos = 0;
		
		@Override
		public Iterator<Measure> iterator() {
			return measures.iterator();
		}
	}


	// We index events in the orchestration by 1) voice, 2) time-sig block, 3) measure number
	// An event is an offset (as a function of the denominator) in the measure, a pitch, and a duration
	List<Triple<String,Integer,Track>> allTracks = new ArrayList<Triple<String,Integer,Track>>();

	public void addMeasures(int measureCount, TimeSignature timeSignature) {
		if (allTracks.size() == 0) {
			throw new RuntimeException("Track must be created before measures can be added");
		}
		for (Triple<String, Integer, Track> track : allTracks) {
			track.getThird().addMeasures(measureCount, timeSignature);
		}
	}

	public void addTrack(String name, int programNumber) {
		if (allTracks.size() == 0) {
			allTracks.add(new Triple<String,Integer,Track>(name, programNumber, new Track(0, null)));
		} else {
			allTracks.add(new Triple<String,Integer,Track>(name, programNumber, new Track(allTracks.get(0).getThird())));
		}
	}

	public void setProgramNumber(int track, int programNumber) {
		allTracks.get(track).setSecond(programNumber);
	}

	public void addNoteEvent(int track, int measure, double msrOffset, int midiPitch, double duration) {
		allTracks.get(track).getThird().addEvent(measure, new MeasureNoteEvent(msrOffset, midiPitch, duration));
	}
	
	public void addTextEvent(int track, int measure, double msrOffset, String text) {
		allTracks.get(track).getThird().addEvent(measure, new MeasureTextEvent(msrOffset, text));
	}

	public static void main(String[] args) {
		MIDIOrchestration orch = new MIDIOrchestration();
		orch.addTrack("Melody", 0);
		orch.addMeasures(4, new TimeSignature(4, 4));
		orch.addNoteEvent(0, 0, 0, 64, 1);
		orch.addNoteEvent(0, 0, 0, 60, 1);
		orch.addTextEvent(0, 0, 0, "I ");
		orch.addNoteEvent(0, 0, 1, 64, 1);
		orch.addTextEvent(0, 0, 1, "love ");
		orch.addNoteEvent(0, 0, 2, 65, 1);
		orch.addTextEvent(0, 0, 2, "you ");
		orch.addNoteEvent(0, 0, 3, 67, 1);
		orch.addTextEvent(0, 0, 3, "and ");
		orch.addNoteEvent(0, 1, 0, 67, 1);
		orch.addTextEvent(0, 1, 0, "you ");
		orch.addNoteEvent(0, 1, 1, 65, 1);
		orch.addTextEvent(0, 1, 1, "love ");
		orch.addNoteEvent(0, 1, 2, 64, 1);
		orch.addTextEvent(0, 1, 2, "me ");
		orch.addNoteEvent(0, 1, 3, 62, 1);
		orch.addTextEvent(0, 1, 3, "we ");
		orch.addNoteEvent(0, 2, 0, 60, 1);
		orch.addTextEvent(0, 2, 0, "are ");
		orch.addNoteEvent(0, 2, 1, 60, 1);
		orch.addTextEvent(0, 2, 1, "a ");
		orch.addNoteEvent(0, 2, 2, 62, 1);
		orch.addTextEvent(0, 2, 2, "hap");
		orch.addNoteEvent(0, 2, 3, 64, 1);
		orch.addTextEvent(0, 2, 3, "py ");
		orch.addNoteEvent(0, 3, 0, 62, 1.5);
		orch.addTextEvent(0, 3, 0, "fa");
		orch.addNoteEvent(0, 3, 1.5, 60, .5);
		orch.addTextEvent(0, 3, 1.5, "mi");
		orch.addNoteEvent(0, 3, 2, 60, 1);
		orch.addTextEvent(0, 3, 2, "ly");
		orch.addNoteEvent(0, 3, 2, 60, 2);
		
		orch.addTrack("Bass", 32);
		orch.addNoteEvent(1, 0, 0, 48, 4);
		orch.addNoteEvent(1, 1, 0, 50, 4);
		orch.addNoteEvent(1, 2, 0, 52, 4);
		orch.addNoteEvent(1, 3, 0, 50, 2);
		orch.addNoteEvent(1, 3, 2, 48, 2);
		
		
		System.out.println(orch);

		orch.saveToFile(new File("midifile.kar"));
	}

	public String toString() {
		StringBuilder str = new StringBuilder();

		for (int i = 0; i < allTracks.size(); i++) {
			Triple<String, Integer, Track> track = allTracks.get(i);
			str.append("Track ");
			str.append(i);
			str.append(" (Name: ");
			str.append(track.getFirst());
			str.append(" - ProgramChange: ");
			str.append(track.getSecond());
			str.append("):");
			str.append(track.getThird());
			str.append("\n");
		}

		return str.toString();
	}

	public void saveToFile(File f) {
		try {
			// **** Create a new MIDI sequence with 24 ticks per beat ****
			Sequence s = new Sequence(javax.sound.midi.Sequence.PPQ, 24);

			for (int i = 0; i < allTracks.size(); i++) {
				Triple<String, Integer, Track> trip = allTracks.get(i);
				
				String trackName = trip.getFirst();
				int tProgram = trip.getSecond();
				Track track = trip.getThird();
				// **** Obtain a MIDI track from the sequence ****
				javax.sound.midi.Track t = s.createTrack();
	
				// **** General MIDI sysex -- turn on General MIDI sound set ****
				byte[] b = { (byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7 };
				SysexMessage sm = new SysexMessage();
				sm.setMessage(b, 6);
				MidiEvent me = new MidiEvent(sm, (long) 0);
				t.add(me);
	
				// **** set tempo (meta event) ****
				MetaMessage mt = new MetaMessage();
				byte[] bt = { 0x06, (byte) 0x00, 0x00 };
				mt.setMessage(0x51, bt, 3);
				me = new MidiEvent(mt, (long) 0);
				t.add(me);
	
				// **** set track name (meta event) ****
				mt = new MetaMessage();
				mt.setMessage(0x03, trackName.getBytes(), trackName.length());
				me = new MidiEvent(mt, (long) 0);
				t.add(me);
	
				// **** set omni on ****
				ShortMessage mm = new ShortMessage();
				mm.setMessage(0xB0, 0x7D, 0x00);
				me = new MidiEvent(mm, (long) 0);
				t.add(me);
	
				// **** set poly on ****
				mm = new ShortMessage();
				mm.setMessage(0xB0, 0x7F, 0x00);
				me = new MidiEvent(mm, (long) 0);
				t.add(me);
	
				// **** set instrument to Piano ****
				mm = new ShortMessage();
				mm.setMessage(Integer.parseInt("C" + (char)('0' + i),16), tProgram, 0x00);
				me = new MidiEvent(mm, (long) 0);
				t.add(me);
	
				long absTick = 0;
				long tickOffset;
				for(Measure measure: track) {
					for(MeasureEvent e: measure) {
						if (e.getClass().equals(MeasureNoteEvent.class)) {
							MeasureNoteEvent event = (MeasureNoteEvent) e;
							tickOffset = (long) (event.offset / (measure.ts.denominator/4.0) * s.getResolution());
							t.add(new MidiEvent(new ShortMessage(Integer.parseInt("9" + (char)('0' + i),16), event.pitch, 0x60), absTick + tickOffset));
							tickOffset += (long) (event.duration / (measure.ts.denominator/4.0) * s.getResolution());
							t.add(new MidiEvent(new ShortMessage(Integer.parseInt("8" + (char)('0' + i),16), event.pitch, 0x40), absTick + tickOffset));
						} else if (e.getClass().equals(MeasureTextEvent.class)) {
							MeasureTextEvent event = (MeasureTextEvent) e;
							tickOffset = (long) (event.offset / (measure.ts.denominator/4.0) * s.getResolution());
							t.add(new MidiEvent(new MetaMessage(0x01, event.lyric.getBytes(), event.lyric.length()), absTick + tickOffset));
						}
					}
					absTick += measure.ts.numerator / (measure.ts.denominator/4.0) * s.getResolution(); 
				}
				
				// **** note on - middle C ****
//				mm = new ShortMessage();
//				mm.setMessage(0x90, 0x3C, 0x60);
//				me = new MidiEvent(mm, (long) 1);
//				t.add(me);
	
				// **** note off - middle C - 120 ticks later ****
//				mm = new ShortMessage();
//				mm.setMessage(0x80, 0x3C, 0x40);
//				me = new MidiEvent(mm, (long) 121);
//				t.add(me);
	
				// **** set end of track (meta event) 19 ticks later ****
				mt = new MetaMessage();
				byte[] bet = {}; // empty array
				mt.setMessage(0x2F, bet, 0);
				me = new MidiEvent(mt, (long) 140);
				t.add(me);
			}

			// **** write the MIDI sequence to a MIDI file ****
			MidiSystem.write(s, 1, f);
		} // try
		catch (Exception e) {
			System.out.println("Exception caught " + e.toString());
		} // catch
	} // main
}
