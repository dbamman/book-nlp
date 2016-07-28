package novels.annotators;

import novels.Book;
import novels.BookCharacter;
import novels.Quotation;
import novels.Token;
import novels.entities.Antecedent;
import novels.util.PrintUtil;

public class CharacterFeatureAnnotator {

	boolean verbose = false;

	public void annotatePaths(Book book) {
		for (Quotation quote : book.quotations) {
			int speakerTokenId = quote.attributionId;
			if (speakerTokenId > 0) {
				if (book.tokenToCharacter.containsKey(speakerTokenId)) {
					Antecedent ant = book.tokenToCharacter.get(speakerTokenId);
					BookCharacter character = book.characters[ant
							.getCharacterId()];
					character
							.addFeature(BookCharacter.SPEAKING, PrintUtil
									.getSpan(quote.start, quote.end + 1, book),
									quote.start);
				}
			}
		}
		
		for (int i = 0; i < book.tokens.size(); i++) {
			if (book.tokenToCharacter.containsKey(i)) {
				Antecedent ant = book.tokenToCharacter.get(i);
				Token head = ant.getHead(book);
				BookCharacter character = book.characters[ant.getCharacterId()];
				if (verbose)
					System.out.println(head.word + " " + head.tokenId + " "
							+ head.pos + " " + head.deprel + " "
							+ ant.getCharacterId());
				String span = PrintUtil.getSpan(head.tokenId - 10,
						head.tokenId + 10, book);
				String agent = SyntaxAnnotator.getAgent(book, head.tokenId);
				if (agent != null) {
					character.addFeature(BookCharacter.AGENT, agent,
							head.tokenId);
					if (verbose)
						System.out.println("\tAGE:" + agent + " "
								+ ant.getCharacterId() + "\t" + span);
				}

				String patient = SyntaxAnnotator.getPatient(book, head.tokenId);
				if (patient != null) {
					character.addFeature(BookCharacter.PATIENT, patient,
							head.tokenId);
					if (verbose)
					System.out.println("\tPAT: " + patient + " " + head.tokenId
							+ " " + ant.getCharacterId() + "\t" + span);
				}

				String mod = SyntaxAnnotator.getMods(book, head.tokenId);
				if (mod != null) {
					character.addFeature(BookCharacter.MOD, mod, head.tokenId);
					if (verbose)
						System.out.println("\tMOD: " + mod + " "
								+ ant.getCharacterId() + "\t" + span);
				}

				String poss = SyntaxAnnotator.getPoss(book, head.tokenId);
				if (poss != null) {
					character
							.addFeature(BookCharacter.POSS, poss, head.tokenId);
					if (verbose)
						System.out.println("\tPOS: " + poss + " "
								+ ant.getCharacterId() + "\t" + span);
				}

				if (verbose) {

					if (agent == null && patient == null && mod == null
							&& poss == null && !head.deprel.equals("null")) {
						if (!head.deprel.equals("pobj")) {
							//System.out.println(head.word + " " + head.deprel);
						}

					}
				}

				i = ant.getEnd() + 1;
			}
		}
	}
}
