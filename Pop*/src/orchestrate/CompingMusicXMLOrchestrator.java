package orchestrate;

import java.util.Random;

import composition.Measure;

public class CompingMusicXMLOrchestrator extends Orchestrator {

	Random rand = new Random();
	@Override
	int orchestrate(Measure measure, boolean lastMeasure, int prevPatternChoice) {
		measure.initOrchestration();
		
		boolean timeIsThree = measure.time.beats == 3;
		
		double currPos = 0.0;
		
		int patternChoice = prevPatternChoice == -1 ? rand.nextInt(7) : prevPatternChoice;
		
		if (!timeIsThree && (patternChoice == 0 || lastMeasure)) {
			boolean compOnFour = !lastMeasure && rand.nextBoolean();
			measure.addChordForHarmonyAt(0.0, 1.0);
			measure.addBassNoteForHarmony(0.0, 1.5);
			measure.addChordForHarmonyAt(1.0, 1.0);
			measure.addBassNoteForHarmony(1.5, 0.5);
			measure.addChordForHarmonyAt(2.0, compOnFour?1.0:2.0);
			measure.addBassNoteForHarmony(2.0, 2.0);
			if (compOnFour){
				measure.addChordForHarmonyAt(3.0, 1.0);
			}
		} else if (timeIsThree || patternChoice == 1) {
			while (currPos < measure.time.beats) {
				double formPosition = currPos % (timeIsThree?3.0:2.0);
				if (formPosition == 0.0 || formPosition == 2.0) {
					// add chord
					measure.addChordForHarmonyAt(currPos, 1.0);
					currPos += 1.0;
					
					// if not the last beat in 3/? measure, add bass note
					if (!timeIsThree || formPosition == 0.0) {
						measure.addBassNoteForHarmony(currPos, 1.5);
					}
					
				} else if (formPosition == 1.0) {
					// add chord
					measure.addChordForHarmonyAt(currPos, 1.0);
					currPos += 0.5;
					
				} else {
					// add bass
					assert formPosition == 1.5;
					measure.addBassNoteForHarmony(currPos, 0.5);
					currPos += 0.5;
				}
			}
		} else if (patternChoice == 2) {
			measure.addBassNoteForHarmony(0.0, 0.5);
			measure.addChordForHarmonyAt(0.0, 1.0);
			measure.addBassNoteForHarmony(0.5, 1.0);
			measure.addChordForHarmonyAt(1.0, 1.0);
			measure.addBassNoteForHarmony(1.5, 1.0);
			measure.addChordForHarmonyAt(2.0, 1.0);
			measure.addBassNoteForHarmony(2.5, 1.0);
			measure.addChordForHarmonyAt(3.0, 1.0);
			measure.addBassNoteForHarmony(3.5, 0.5);
		} else if (patternChoice == 3) {
			measure.addChordForHarmonyAt(0.0, 1.0);
			measure.addChordForHarmonyAt(1.0, 1.0);
			measure.addChordForHarmonyAt(2.0, 1.0);
			measure.addChordForHarmonyAt(3.0, 1.0);
			measure.addBassNoteForHarmony(0.0, 1.0);
			measure.addBassNoteForHarmony(1.0, 2.0);
			measure.addBassNoteForHarmony(3.0, 1.0);
		} else if (patternChoice == 4) {
			measure.addChordForHarmonyAt(0.0, 1.0);
			measure.addChordForHarmonyAt(1.0, 1.0);
			measure.addChordForHarmonyAt(2.0, 1.0);
			measure.addChordForHarmonyAt(3.0, 1.0);
			measure.addBassNoteForHarmony(0.0, 2.5);
			measure.addBassNoteForHarmony(2.5, 1.0);
			measure.addBassNoteForHarmony(3.5, 0.5);
		} else if (patternChoice == 5) {
			measure.addChordForHarmonyAt(0.0, 1.0);
			measure.addBassNoteForHarmony(0.0, 4.0);
			measure.addChordForHarmonyAt(1.0, 0.5);
			measure.addChordForHarmonyAt(1.5, 1.0);
			measure.addChordForHarmonyAt(2.5, 0.5);
			measure.addChordForHarmonyAt(3.0, 1.0);
		} else if (patternChoice == 6) {
			measure.addChordForHarmonyAt(0.0, 2.0);
			measure.addBassNoteForHarmony(0.0, 2.0);
			measure.addChordForHarmonyAt(2.0, 2.0);
			measure.addBassNoteForHarmony(2.0, 2.0);
		} 
		
		return patternChoice;
	}

}
