package harmony;

import composition.Score;
import data.MusicXMLParser.Harmony;
import data.MusicXMLParser.Quality;
import data.MusicXMLParser.Root;
import inspiration.Inspiration;
import pitch.Pitch;

public class MonochordHarmonyEngineer extends HarmonyEngineer {

	@Override
	public void addHarmony(Inspiration inspiration, Score score) {
		Quality majorQuality = new Quality();
		for (int i = 0; i < score.length(); i++) {
			score.addHarmony(i, 0.0, new Harmony(new Root(Pitch.getPitchValue("C")), majorQuality, null));
		}
	}

	

}
