package novels;

public class Token {
	public int sentenceID;
	public int tokenId;
	public String quotation;
	public int p;
	public String word;
	public String original;
	public String lemma;
	public String ner;
	public int gender;
	public String pos;
	public int head;
	public String deprel;
	public int beginOffset;
	public int endOffset;
	public String whitespaceAfter;
	public int characterId=-1;
	public int coref;
	public String supersense;

	public boolean isPersonOrOrg() {
		return ner.equals("PERSON") || ner.equals("ORGANIZATION");
	}

	public Token() {

	}

	public Token(String line, int parOffset, int sentenceOffset, int tokenOffset) {
		String[] parts = line.trim().split("\t");
		this.p = Integer.valueOf(parts[0]) + parOffset;
		this.sentenceID = Integer.valueOf(parts[1]) + sentenceOffset;
		this.tokenId = Integer.valueOf(parts[2]) + tokenOffset;
		this.beginOffset = Integer.valueOf(parts[3]);
		this.endOffset = Integer.valueOf(parts[4]);
		this.whitespaceAfter = parts[5];
		this.head = Integer.valueOf(parts[6]);
		if (this.head != -1) {
			this.head += tokenOffset;
		}
		this.original = parts[7];
		this.word = parts[8];
		this.lemma = parts[9];
		this.pos = parts[10];
		this.ner = parts[11];
		this.deprel = parts[12];
		this.quotation = parts[13];
		if (parts.length > 14) {
			this.characterId = Integer.valueOf(parts[14]);
		}
		if (parts.length > 15) {
			this.supersense = parts[15];
		}
	}

	public Token(String line) {
		String[] parts = line.trim().split("\t");
		this.p = Integer.valueOf(parts[0]);
		this.sentenceID = Integer.valueOf(parts[1]);
		this.tokenId = Integer.valueOf(parts[2]);
		this.beginOffset = Integer.valueOf(parts[3]);
		this.endOffset = Integer.valueOf(parts[4]);
		this.whitespaceAfter = parts[5];
		this.head = Integer.valueOf(parts[6]);
		this.original = parts[7];
		this.word = parts[8];
		this.lemma = parts[9];
		this.pos = parts[10];
		this.ner = parts[11];
		this.deprel = parts[12];
		this.quotation = parts[13];
		if (parts.length > 14) {
			this.characterId = Integer.valueOf(parts[14]);
		}
		if (parts.length > 15) {
			this.supersense = parts[15];
		}

	}

	public void setWhitespaceAfter(String ws) {
		whitespaceAfter = ws.replaceAll("[^ \\n\\r\\t]", "");
		whitespaceAfter = whitespaceAfter.replaceAll("\\n", "N");
		whitespaceAfter = whitespaceAfter.replaceAll(" ", "S");
		whitespaceAfter = whitespaceAfter.replaceAll("\\t", "T");
	}

	public String getWhiteSpaceAfter() {
		return whitespaceAfter.replaceAll("T", "\t").replaceAll("S", " ")
				.replaceAll("N", "\n");
	}

	public static String ORDER = String.format(
			"%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
			"paragraphId", "sentenceID", "tokenId", "beginOffset", "endOffset",
			"whitespaceAfter", "headTokenId", "originalWord", "normalizedWord",
			"lemma", "pos", "ner", "deprel", "inQuotation", "characterId", "supersense");

	public String toString() {
		return String.format(
				"%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
				p, sentenceID, tokenId, beginOffset, endOffset,
				whitespaceAfter, head, original, word, lemma, pos, ner, deprel,
				quotation, characterId, supersense);

	}

}
