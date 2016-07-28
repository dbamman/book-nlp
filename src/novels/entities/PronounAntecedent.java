package novels.entities;

import novels.Token;
import novels.Book;


public class PronounAntecedent implements Antecedent {

	int gender;
	int tokenID;
	public int characterID=-1;

	public PronounAntecedent(int id, int gender) {
		this.gender = gender;
		this.tokenID = id;
	}

	public int getGender(Book book) {
		return gender;
	}

	public Token getHead(Book book) {
		return book.tokens.get(tokenID);
	}
	public String getString(Book book){ 
		if (characterID == -1)
			return book.tokens.get(tokenID).word;
		else
			return book.characters[characterID].name;
	}
	public int getCharacterId() {
		return characterID;
	}
	public int getStart() {
		return tokenID;
	}
	public int getEnd() {
		return tokenID;
	}

}
