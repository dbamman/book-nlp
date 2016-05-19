package novels.annotators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import novels.Book;
import novels.BookCharacter;
import novels.Dictionaries;
import novels.Token;
import novels.entities.CharacterToken;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Find all characters within a book and annotate them as such.
 * 
 * @author dbamman
 * 
 */
public class CharacterAnnotator {

	// Map from character name to all Character indices it could be generated
	// from.
	HashMap<String, HashSet<Integer>> index;

	// the minimum number of times a name must show up to denote a character
	int minCharacterNameMentions = 2;

	// maximum length of a character name (in characters)
	int maxCharacterNameLength = 50;

	/*
	 * Find all characters in a book.
	 */
	public void findCharacters(Book book, Dictionaries dicts) {

		index = Maps.newHashMap();
		int i = 0;
		HashSet<String> characterHash = Sets.newHashSet();
		HashMap<String, Integer> counts = Maps.newHashMap();

		while (i < book.tokens.size()) {
			Token token = book.tokens.get(i);
			if (token.isPersonOrOrg()) {

				String mwe = "";
				mwe += token.word + " ";

				for (int j = i + 1; j < book.tokens.size(); j++) {
					Token next=book.tokens.get(j);
					if (next.p == token.p && next.isPersonOrOrg()) {
						mwe += book.tokens.get(j).word + " ";
						i = j;
					} else {
						break;
					}
				}

				String name = mwe.trim().toLowerCase();
				//System.out.println(name + " " + i);
				characterHash.add(name);
				int count = 0;
				if (counts.containsKey(name)) {
					count = counts.get(name);
				}
				count++;
				counts.put(name, count);
			}
			i++;

		}

		// Filter
		HashSet<String> filtNames = Sets.newHashSet();
		for (String name : characterHash) {
			if (counts.get(name) >= minCharacterNameMentions
					&& name.length() <= maxCharacterNameLength) {
				filtNames.add(name);
			}
		}
		characterHash = filtNames;

		HashSet<BookCharacter> finalNames = Sets.newHashSet();
		int id = 0;

		// Filter names that are subsets of others
		for (String name : characterHash) {

			boolean flag = false;
			// break out all components of the name (Mr. Joe Gargery)
			Set<String> nameSet = new HashSet<String>(Arrays.asList(name
					.split(" ")));
			for (String name2 : characterHash) {

				Set<String> name2Set = new HashSet<String>(Arrays.asList(name2
						.split(" ")));

				// if one character's name is a complete subset of another's,
				// don't add it (e.g., Joe > Mr. Joe Gargery)
				if (!nameSet.equals(name2Set) && name2Set.containsAll(nameSet)) {
					flag = true;
                    continue;
				}
				// if there are namesets that are equal (e.g. "Sakura Kinomoto"
				// and "Kinomoto Sakura") then only add the name first in
				// lexicographic order
				if (nameSet.equals(name2Set) && name.compareTo(name2) > 0) {
				    flag = true;
                    continue;
				}
			}

			if (!flag) {
				BookCharacter character = new BookCharacter(name, id);
				finalNames.add(character);
				id++;
			}
		}

		book.characters = new BookCharacter[id];

		// find all possible variants of a name (Mr. Joe Gargery -> Joe,
		// Gargery, Joe Gargery, Mr. Gargery, Mr. Joe), which restores subsets.
		// This means "Tom" will never show up as a character of its own if
		// another "Tom X" exists elsewhere.
		for (BookCharacter character : finalNames) {
			book.characters[character.id] = character;

			String name = character.name;
			HashSet<String> variants = getVariants(name, dicts);
			for (String v : variants) {

				HashSet<Integer> vi = null;
				if (index.containsKey(v)) {
					vi = index.get(v);
				} else {
					vi = Sets.newHashSet();
				}
				vi.add(character.id);
				index.put(v, vi);
			}

		}

	}

	/*
	 * Find all variants of a given name. E.g., Mr. Tom Sawyer -> Mr. Sawyer Tom
	 * Sawyer Sawyer Tom Mr. Tom. Should work nicknames in here.
	 */
	public HashSet<String> getVariants(String name, Dictionaries dicts) {
		HashSet<String> variants = Sets.newHashSet();
		String[] parts = name.split(" ");
		for (int i = 0; i < parts.length; i++) {
			if (!dicts.honorifics.contains(parts[i])) {
				variants.add(parts[i]);
			}
			for (int j = i + 1; j < parts.length; j++) {
				variants.add(parts[i] + " " + parts[j]);
				for (int k = j + 1; k < parts.length; k++) {
					variants.add(parts[i] + " " + parts[j] + " " + parts[k]);

					for (int l = k + 1; l < parts.length; l++) {
						variants.add(parts[i] + " " + parts[j] + " " + parts[k]
								+ " " + parts[l]);
						for (int m = l + 1; m < parts.length; m++) {
							variants.add(parts[i] + " " + parts[j] + " "
									+ parts[k] + " " + parts[l] + " "
									+ parts[m]);
						}
					}

				}
			}
		}
		variants.add(name);
		return variants;

	}

	/**
	 * If any tokens match names (but aren't labeled as ner PERSON), label as
	 * such. Not in use (not clear it helps and lots of noise).
	 * 
	 * @param book
	 */
	public void populateAllTokens(Book book) {
		ArrayList<HashSet<String>> characterGrams = Lists.newArrayList();
		for (int i = 0; i < 5; i++) {
			HashSet<String> characterGram = Sets.newHashSet();
			characterGrams.add(characterGram);
		}

		for (String name : index.keySet()) {
			int nameLength = name.split(" ").length - 1;
			HashSet<String> c = characterGrams.get(nameLength);
			c.add(name);
		}

		for (int i = 0; i < book.tokens.size(); i++) {
			Token word = book.tokens.get(i);
			if (characterGrams.get(0).contains(word.word)) {
				word.ner = "PERSON";
				System.out.println("updating" + word.word);
			}
			if (i < book.tokens.size() - 1) {
				Token word2 = book.tokens.get(i + 1);
				if (characterGrams.get(1)
						.contains(word.word + " " + word2.word)) {
					word.ner = "PERSON";
					word2.ner = "PERSON";
					System.out.println("updating" + word.word + " "
							+ word2.word);

				}
				if (i < book.tokens.size() - 2) {
					Token word3 = book.tokens.get(i + 2);
					if (characterGrams.get(2).contains(
							word.word + " " + word2.word + " " + word3.word)) {
						word.ner = "PERSON";
						word2.ner = "PERSON";
						word3.ner = "PERSON";
					}
					if (i < book.tokens.size() - 3) {
						Token word4 = book.tokens.get(i + 3);
						if (characterGrams.get(3).contains(
								word.word + " " + word2.word + " " + word3.word
										+ " " + word4.word)) {
							word.ner = "PERSON";
							word2.ner = "PERSON";
							word3.ner = "PERSON";
							word4.ner = "PERSON";
						}
						if (i < book.tokens.size() - 4) {
							Token word5 = book.tokens.get(i + 4);
							if (characterGrams.get(4).contains(
									word.word + " " + word2.word + " "
											+ word3.word + " " + word4.word
											+ " " + word4.word)) {
								word.ner = "PERSON";
								word2.ner = "PERSON";
								word3.ner = "PERSON";
								word4.ner = "PERSON";
								word5.ner = "PERSON";
							}
						}
					}
				}
			}
		}
	}


	/*
	 * Resolve ambiguous tokens (e.g., Tom) to the most recent seen character.
	 */
	public void resolveCharacters(Book book, Dictionaries dicts) {
		int i = 0;

		book.tokenToCharacter = Maps.newTreeMap();

		HashMap<BookCharacter, Integer> lastSeen = Maps.newHashMap();

		int start = 0;
		int end = 0;
		while (i < book.tokens.size()) {
			start = i;
			end = i;
			Token token = book.tokens.get(i);
			if (token.isPersonOrOrg()) {
				String mwe = "";
				mwe += token.word + " ";

				for (int j = i + 1; j < book.tokens.size(); j++) {
					if (book.tokens.get(j).isPersonOrOrg()) {
						mwe += book.tokens.get(j).word + " ";
						i = j;
						end = j;
					} else {
						break;
					}
				}

				String name = mwe.trim();

				if (index.containsKey(name.toLowerCase())) {
					HashSet<Integer> candidates = index.get(name.toLowerCase());
					int max = -1;
					BookCharacter maxChar = null;
					for (Integer ci : candidates) {
						BookCharacter c = book.characters[ci];
						// start by biasing toward high frequency characters
						int last = c.count;
						// + the location of the last assignment
						if (lastSeen.containsKey(c)) {
							last += lastSeen.get(c);
						}

						if (last > max) {
							max = last;
							maxChar = c;
						}

					}
					CharacterToken charToken = new CharacterToken(maxChar.id,
							start, end);

					// mark the whole span in the book as denoting that
					// character
					for (int k = start; k <= end; k++) {
						book.tokenToCharacter.put(k, charToken);
					}
					lastSeen.put(maxChar, max);
					maxChar.add(name);

				}
			}
			i++;

		}

		// After all the tokens have been assigned, calculate and save
		// properties of the characters (like most frequent name).
		for (int c = 0; c < book.characters.length; c++) {
			book.characters[c].setDominantName();
			int charGender = dicts.getGender(book.characters[c].nameCounts);
			book.characters[c].gender = charGender;
			//System.out.println(String.format("%s\tCHAR: %s\t%s\t%s",
				//	book.characters[c].count, c, book.characters[c].name,
					//charGender));

		}
	}

}
