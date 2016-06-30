package tabcomplete.utils;

/**
 * @author Paul Bodily
 */

public class StopWatch {

	private long start = -1;
	private long elapsed = 0;

	/**
	 * Initializes a new stopwatch.
	 */
	public StopWatch() {
	}

	/**
	 * Returns the elapsed CPU time (in seconds) since the stopwatch was created.
	 *
	 * @return elapsed CPU time (in seconds) since the stopwatch was created
	 */
	public double elapsedTime() {
		if (start == -1) {
			return elapsed/1000.0;
		} else
			return (elapsed + (System.currentTimeMillis() - start)) / 1000.0;
	}

	public void reset() {
		elapsed = 0;
	}

	public void start() {
		if (start == -1) {
			start = System.currentTimeMillis();
		} else {
			throw new RuntimeException("Stopwatch must be stopped before starting");
		}
	}

	public void stop() {
		if (start == -1) {
			throw new RuntimeException("Stopwatch must be started before stopping");
		} else {
			elapsed += (System.currentTimeMillis() - start);
			start = -1;
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		StopWatch watch = new StopWatch();
		
		for (int i = 0; i < 10; i++) {
			watch.start();
			Thread.sleep(1000);
			watch.stop();
			System.out.println("Time: "+ watch.elapsedTime() + " seconds");
		}
	}
}
