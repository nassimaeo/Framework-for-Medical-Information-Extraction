package reasoning;

public class Triplet {

	// The accuracy is averaged for SUBJECT, OBJECT and PREDIATE of the triplet
	// Accuracy ranges from 0.0 to 1.0 [0%-100%]

	double accuracy = 0.0;

	private String[] subject;
	private String[] predicate;
	private String[] object;

	public Triplet(String[] s, String[] p, String[] o, double accuracy) {
		this.subject = s;
		this.predicate = p;
		this.object = o;
		this.accuracy = accuracy;
	}

	public double getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(double newAccuracy) {
		this.accuracy = newAccuracy;
	}

	public String[] getSubject() {
		return subject;
	}

	public void setSubject(String[] subject) {
		this.subject = subject;
	}

	public String[] getPredicate() {
		return predicate;
	}

	public void setPredicate(String[] predicate) {
		this.predicate = predicate;
	}

	public String[] getObject() {
		return object;
	}

	public void setObject(String[] object) {
		this.object = object;
	}

	@Override
	public String toString() {
		String subjectString = (subject == null ? "null" : String.join(" ", subject));
		String predicateString = (predicate == null ? "null" : String.join(" ", predicate));
		String objectString = (object == null ? "null" : String.join(" ", object));
		String accuracy = String.format("%3.0f", this.getAccuracy() * 100) + "%";
		return predicateString + "(" + subjectString + "," + objectString + ") Accuracy: " + accuracy;
	}

	/**
	 * merge two triplets into one and return a new triplet
	 * 
	 * @param subTriplet
	 * @return
	 */
	public Triplet merge(Triplet subTriplet) {
		if ((this.getSubject() != null && subTriplet.getSubject() != null)
				|| (this.getObject() != null && subTriplet.getObject() != null)
				|| (this.getPredicate() != null && subTriplet.getPredicate() != null))
			throw new IllegalArgumentException(
					"merge conflict: " + this.toString() + " with: " + subTriplet.toString());

		double avragedAccuracy = (this.getAccuracy() + subTriplet.getAccuracy()) / 2.0;
		Triplet newTriplet = new Triplet(this.getSubject(), this.getPredicate(), this.getObject(), avragedAccuracy);
		if (subTriplet.getSubject() != null)
			newTriplet.setSubject(subTriplet.getSubject());
		if (subTriplet.getObject() != null)
			newTriplet.setObject(subTriplet.getObject());
		if (subTriplet.getPredicate() != null)
			newTriplet.setPredicate(subTriplet.getPredicate());

		return newTriplet;
	}
}
