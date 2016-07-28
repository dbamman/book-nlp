package novels;

public class Quotation {
	public int start;
	public int end;
	public int attributionId;
	public int sentenceId;
	public int p;

	public Quotation(int start, int end, int sentenceId) {
		this.start = start;
		this.end = end;
		this.sentenceId = sentenceId;
	}

}
