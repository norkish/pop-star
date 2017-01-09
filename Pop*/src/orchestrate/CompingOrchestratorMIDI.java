package orchestrate;

import composition.Composition;
import composition.Measure;

public class CompingOrchestratorMIDI extends Orchestrator {

	public void orchestrate(Composition newSong) {
		throw new UnsupportedOperationException();
//		MIDIOrchestration orchestration = new MIDIOrchestration();
//		orchestration.addTrack("Melody", 0);
//		orchestration.addTrack("Piano", 0);
//		orchestration.addTrack("Bass", 132);
//		orchestration.addTrack("Drums", 132);
//		
////		for (Iterator<Triple<SegmentType, Integer, SegmentStructure>> segmentIter = newSong.getStructure().new SegmentIterator<Triple<SegmentType, Integer, SegmentStructure>>(); segmentIter.hasNext();) {
////			Triple<SegmentType, Integer, SegmentStructure> segment = (Triple<SegmentType, Integer, SegmentStructure>) segmentIter.next();
////			SegmentStructure segmentSubstructure = segment.getThird();
////			orchestration.addMeasures(segmentSubstructure.getMeasureCount(), segmentSubstructure.timeSignature);
////			
////			
////			
////		}
//		
//		return null;
	}

	@Override
	void orchestrate(Measure measure) {
		throw new UnsupportedOperationException();
	}

}
