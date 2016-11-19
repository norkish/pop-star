package utils;

public class Triple<S1, S2, S3> {

	private S1 first;
	private S2 second;
	private S3 third;

	public Triple(S1 first, S2 second, S3 third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public S1 getFirst() {
		return first;
	}

	public S2 getSecond() {
		return second;
	}

	public S3 getThird() {
		return third;
	}

	public void setSecond(S2 second) {
		this.second = second;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Triple [first=").append(first).append(", second=").append(second).append(", third=")
				.append(third).append("]");
		return builder.toString();
	}

	public void setFirst(S1 first) {
		this.first = first;
	}
}
