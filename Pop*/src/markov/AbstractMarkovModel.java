package markov;
import java.util.List;

public abstract class AbstractMarkovModel<T> {

	abstract public double probabilityOfSequence(T[] seq);
	
	abstract public List<T> generate(int length);
	
}
