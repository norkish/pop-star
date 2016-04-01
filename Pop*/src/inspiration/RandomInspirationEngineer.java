package inspiration;

public class RandomInspirationEngineer extends InspirationEngineer {

	@Override
	public Inspiration generateInspiration() {
		Inspiration inspiration = new Inspiration(InspirationSource.RANDOM);
		return inspiration;
	}

}
