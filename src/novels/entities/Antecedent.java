package novels.entities;

import novels.Token;
import novels.Book;

public interface Antecedent {


	public int getGender(Book book);
	public Token getHead(Book book);
	public String getString(Book book);
	public int getCharacterId();
	public int getStart();
	public int getEnd();
}
