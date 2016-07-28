package novels.annotators;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import novels.Book;
import novels.Dictionaries;
import novels.Token;
import novels.entities.Antecedent;
import novels.entities.PronounAntecedent;
import novels.util.Util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class CoreferenceAnnotator {

	// Log-linear model weights
	HashMap<String, Double> weights;

	// Number of tokens to look back to find antecendent.
	static final int antecedentWindow = 100;

	public void readWeights(String infile) {
		weights = Maps.newHashMap();
		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(infile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				String[] parts = str1.trim().split("\t");
				weights.put(parts[0], Double.valueOf(parts[1]));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Finished Reading Weights");
	}

	public int getSalience(Antecedent cand, Book book) {
		int window = 500;

		Token head = cand.getHead(book);
		if (head.pos.startsWith("PRP")) {
			return 1;
		}

		int start = head.tokenId - window;
		if (start < 0) {
			start = 0;
		}
		int count = 0;
		for (int i = start; i < head.tokenId; i++) {
			if (book.tokens.get(i).word.toLowerCase().equals(
					head.word.toLowerCase())) {
				count++;
			}
		}

		return count;
	}

	public HashMap<String, Object> getFeatures(Token pronoun,
			Antecedent candidate, Book book) {
		HashMap<String, Object> features = Maps.newHashMap();
		Token head = candidate.getHead(book);

		int sameQuote = 0;
		if (head.quotation == pronoun.quotation) {
			sameQuote = 1;
		}
		
		int isPerson = 0;
		if (head.ner.equals("PERSON"))
			isPerson = 1;

		String synpath = SyntaxAnnotator.getPath(pronoun.tokenId, head.tokenId,
				book.tokens);
		int syndist = synpath.split(">").length;

		// salience
		int gender = Dictionaries.getPronounGender(pronoun);
		
		int oppositeGender = 0;
		if ((gender == Dictionaries.MALE && candidate.getGender(book) == Dictionaries.FEMALE)
				|| gender == Dictionaries.FEMALE
				&& candidate.getGender(book) == Dictionaries.MALE) {
			oppositeGender = 1;
		}

		int linearDistance = 0;
		for (int i = head.tokenId; i < pronoun.tokenId; i++) {
			Token tok = book.tokens.get(i);
			if (tok.quotation == pronoun.quotation) {
				linearDistance++;
			}
		}

		features.put("isPerson", isPerson);
		features.put("oppositeGender", oppositeGender);
		features.put("linearDistance", linearDistance);
		features.put("sameQuote", sameQuote);
		features.put("salience", getSalience(candidate, book));
		features.put(head.pos, 1);
		features.put("synpath:" + synpath, 1);
		features.put("deprel:" + head.deprel, 1);
		features.put("syndist", syndist);

		return features;
	}

	public double score(Token pronoun, Antecedent candidate, Book book) {

		Token head = candidate.getHead(book);
		if (pronoun.quotation != head.quotation) {
			return 0;
		}
		int gender = Dictionaries.getPronounGender(pronoun);
		if (gender != candidate.getGender(book)) {
			return 0;
		}

		HashMap<String, Object> features = getFeatures(pronoun, candidate, book);

		double sum = 0;
		for (String feat : features.keySet()) {
			int val = (Integer) features.get(feat);
			if (weights.containsKey(feat)) {
				sum += weights.get(feat) * val;
			}
		}

		return Math.exp(sum);

	}

	/**
	 * Greedily assign coreference from beginning to end of book.
	 * 
	 * @param book
	 */
	public void resolvePronouns(Book book) {

		for (Token token : book.tokens) {
			//System.out.println("tokenid " + token.tokenId);			
			if (token.pos.startsWith("PRP")
					&& (token.lemma.equals("she") || token.lemma.equals("he"))) {

				HashMap<Antecedent, Double> scores = Maps.newHashMap();
				ArrayList<Antecedent> candidates = getCandidates(token.tokenId,
						book);
				for (Antecedent candidate : candidates) {
					double score = score(token, candidate, book);
					scores.put(candidate, score);
				}

				ArrayList<Object> sorted = Util.sortHashMapByValue(scores);
				if (sorted.size() > 0) {
					Antecedent winner = (Antecedent) sorted.get(0);
					token.coref = winner.getHead(book).tokenId;
					//System.out.println("COREF: " + token.coref);
				}
				// add pronoun as candidate antecendent for next token
				int gender = Dictionaries.getPronounGender(token);
				PronounAntecedent pronoun = new PronounAntecedent(
						token.tokenId, gender);
				book.animateEntities.put(token.tokenId, pronoun);

			}
		}

		// set maximal heads
		for (Token token : book.tokens) {
			//System.out.println("tokenid " + token.tokenId);			
			if (token.pos.startsWith("PRP")
					&& (token.lemma.equals("she") || token.lemma.equals("he") || token.lemma
							.equals("you"))) {
				int gender = Dictionaries.getPronounGender(token);

				if (token.coref != 0) {
					int corefHead = token.coref;
					int hops=0;
					while (corefHead != 0) {
						hops++;
						if (hops > 100) {
							break;
						}
						Token tokenHead = book.tokens.get(corefHead);
						if (book.tokenToCharacter
								.containsKey(tokenHead.tokenId)) {
							Antecedent c = book.tokenToCharacter
									.get(tokenHead.tokenId);

							PronounAntecedent pronoun = new PronounAntecedent(
									token.tokenId, gender);
							pronoun.characterID = c.getCharacterId();
							book.tokenToCharacter.put(token.tokenId, pronoun);

						}
						if (tokenHead.coref == corefHead) {
							break;
						}
						corefHead = tokenHead.coref;
					}
				}
			}
		}

	}

	/**
	 * Print pronouns to be resolved and candidates
	 * 
	 * @param book
	 */
	public HashMap<Integer, HashSet<Integer>> getResolvable(Book book) {
		HashMap<Integer, HashSet<Integer>> resolvable = Maps.newHashMap();
		for (Token token : book.tokens) {
			if (token.pos.startsWith("PRP")
					&& (token.lemma.equals("she") || token.lemma.equals("he") || token.lemma
							.equals("you"))) {

				ArrayList<Antecedent> candidates = getCandidates(token.tokenId,
						book);
				HashSet<Integer> cands = Sets.newHashSet();
				for (Antecedent candidate : candidates) {
					cands.add(candidate.getHead(book).tokenId);
				}

				resolvable.put(token.tokenId, cands);

				// add pronoun as candidate antecendent for next token
				int gender = Dictionaries.getPronounGender(token);
				PronounAntecedent pronoun = new PronounAntecedent(
						token.tokenId, gender);
				book.animateEntities.put(token.tokenId, pronoun);

			}
		}
		return resolvable;
	}

	/**
	 * Find all antecendent candidates for a given token.
	 * 
	 * @param tokenId
	 * @param book
	 * @return
	 */
	public ArrayList<Antecedent> getCandidates(int tokenId, Book book) {

		ArrayList<Antecedent> candidates = Lists.newArrayList();

		Token token = book.tokens.get(tokenId);
		HashSet<Integer> seen = Sets.newHashSet();
		int last = token.tokenId - 1;
		int l = last;
		int count = 0;

		// find starting point (antecedentWindow only pertains to tokens of the
		// same quotation level)
		while (count <= antecedentWindow && l > 0) {
			Token lastToken = book.tokens.get(l);
			if (lastToken.quotation == token.quotation) {
				count++;
			}
			l--;
		}

		last = l;

		for (int i = last; i < token.tokenId; i++) {
			if (seen.contains(i)) {
				continue;
			}
			Token lastToken = book.tokens.get(i);

			// skip all not in quotation?
			if (lastToken.quotation != token.quotation) {
				continue;
			}
			//if (lastToken.pos.startsWith("PRP")){// || lastToken.ner.equals("PERSON")) {
			if (lastToken.pos.startsWith("PRP") || lastToken.ner.equals("PERSON")) {
				int gender = Dictionaries.getPronounGender(lastToken);
				PronounAntecedent pro = new PronounAntecedent(i, gender);
				candidates.add(pro);
			}

			if (book.tokenToCharacter.containsKey(i)) {
				Antecedent charTok = book.tokenToCharacter.get(i);
				candidates.add(charTok);
				// skip over these tokens so we don't add the same character
				// twice
				for (int j = charTok.getStart(); j <= charTok.getEnd(); j++) {
					seen.add(j);
				}
			} else if (book.animateEntities.containsKey(i)) {
				candidates.add(book.animateEntities.get(i));
			}

		}

		return candidates;

	}

}
