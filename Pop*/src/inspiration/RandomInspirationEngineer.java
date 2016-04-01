package inspiration;

public class RandomInspirationEngineer extends InspirationEngineer {

	@Override
	public Inspiration generateInspiration() {
		Inspiration inspiration = new Inspiration(InspirationSource.RANDOM);
		inspiration.setExplaination("I was feeling " + inspiration.getMaxEmotion());
		return inspiration;
	}

}
