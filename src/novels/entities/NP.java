package novels.entities;

import novels.Token;
import novels.Book;


public class NP implements Antecedent {
	public int start;
	public int end;
	public int head;
	public int gender = 0;
	public int characterID=-1;
	
	public boolean animate;
	public boolean male;

	public String phrase;
	public String headPhrase;

	public String toShortString(Book book) {
		return phrase + "\t" + book.tokens.get(head).word + "\t" + gender;
	}

	public String toString(Book book) {
		return start + "\t" + end + "\t" + phrase + "\t"
				+ book.tokens.get(head).word + "\t" + animate;
	}

	public NP() {
		phrase = "";
		headPhrase = "";
	}

	public int getGender(Book book) {
		return gender;
	}

	public Token getHead(Book book) {
		return book.tokens.get(head);
	}
	public String getString(Book book) {
	if (characterID == -1)
		return headPhrase;
	else
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
