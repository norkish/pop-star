package tabcomplete.rhyme;

public class StressedPhone {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + phone;
		result = prime * result + stress;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StressedPhone other = (StressedPhone) obj;
		if (phone != other.phone)
			return false;
		if (stress != other.stress)
			return false;
		return true;
	}

	public int phone;
	public int stress;

	public StressedPhone(int phone, int stress) {
		this.phone = phone;
		this.stress = stress;
	}
	
	public String toString() {
		return "" + Phonetecizer.intToString(phone) + ":" + stress;
	}
}
