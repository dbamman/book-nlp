package novels.entities;

import novels.Token;
import novels.Book;

public class CharacterToken implements Antecedent {
	public int start;
	public int end;
	public int characterID=-1;

	public CharacterToken(int id, int start, int end) {
		this.characterID = id;
		this.start = start;
		this.end = end;
	}

	public int getGender(Book book) {
		return book.characters[characterID].gender;
	}

	public Token getHead(Book book) {
		return book.tokens.get(end);
	}
	public String getString(Book book) {
		return book.characters[characterID].name;
	}
	public int getCharacterId() {
		return characterID;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
}