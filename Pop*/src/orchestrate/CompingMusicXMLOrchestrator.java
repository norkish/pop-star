package orchestrate;

import composition.Measure;

public class CompingMusicXMLOrchestrator extends Orchestrator {

	@Override
	void orchestrate(Measure measure) {
		measure.initOrchestration();
		
		boolean timeIsThree = measure.time.beats == 3;
		
		double currPos = 0.0;
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
		
	}

}
