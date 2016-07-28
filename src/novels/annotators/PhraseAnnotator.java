package novels.annotators;

import java.util.TreeSet;

import novels.Book;
import novels.Dictionaries;
import novels.Token;
import novels.entities.NP;

/**
 * Find and annotate all animate NPs for a book
 * @author dbamman
 *
 */
public class PhraseAnnotator {

	public NP getPhrase(int index, Book book, Dictionaries dicts) {

		TreeSet<Integer> deps = null;
		if (book.dependents.containsKey(index)) {
			deps = new TreeSet<Integer>(SyntaxAnnotator.getRecursiveDeps(index, book));
		} else {
			deps = new TreeSet<Integer>();
		}

		deps.add(index);
		NP np = new NP();
		np.head = index;
		np.start = deps.first();
		np.end = deps.last();

		for (int i = np.start; i <= np.end; i++) {
			Token tok = book.tokens.get(i);
			np.phrase += tok.word + " ";
		}
		
		Token word = book.tokens.get(index);
		if (dicts.animateUnigrams.contains(word.word)) {
			np.animate = true;
		}
		
		
		if (dicts.genderUnigrams.containsKey(word.word.toLowerCase())) {
			np.gender = dicts.genderUnigrams.get(word.word.toLowerCase());
		}

		// immediate children
		TreeSet<Integer> nextDeps = book.dependents.get(index);
		TreeSet<Integer> full = new TreeSet<Integer>();
		if (nextDeps != null) {
			full.addAll(nextDeps);
		}

		if (full != null) {
			full.add(index);
			for (int next : full) {
				Token tok = book.tokens.get(next);
				if (tok.pos.equals("DT") || tok.pos.startsWith("NN")) {
					np.headPhrase += tok.word + " ";
				}
				if (dicts.maleHonorifics.contains(tok.word.toLowerCase())) {
					np.gender = Dictionaries.MALE;
				} else if (dicts.femaleHonorifics.contains(tok.word.toLowerCase())) {
					np.gender = Dictionaries.FEMALE;
				}
			}
		}
		
	/*	if (np.animate){
			System.out.println("\n-----ANIMATE \n Phrase - " + np.phrase);
			System.out.println("Head Phrase - " + np.headPhrase + "\n-----");
		}else{
			System.out.println("\n Phrase - " + np.phrase);
			System.out.println(" Head Phrase - " + np.headPhrase);
		}
	*/
		return np;

	}
	
	/**
	 * Find all the animate NPs in the book and annotate them.
	 * @param book
	 * @param dicts
	 */
	public void getPhrases(Book book, Dictionaries dicts) {

		for (Token tok : book.tokens) {

			if (tok.pos.startsWith("NN")) {
				NP phrase = getPhrase(tok.tokenId, book, dicts);

				if (phrase.animate) {
					book.animateEntities.put(tok.tokenId, phrase);
				}
			}
		}
		
		book.animateEntities.putAll(book.tokenToCharacter);
		
	}
}
