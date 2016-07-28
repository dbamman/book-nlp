package novels;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class Dictionaries {

	public HashSet<String> sentientLemmas;
	public HashSet<String> honorifics;
	public HashSet<String> maleHonorifics;
	public HashSet<String> femaleHonorifics;
	public HashSet<String> generalHonorifics;

	public HashSet<String> animateUnigrams;
	public HashMap<String, Integer> genderUnigrams;

	public static int FEMALE = 1;
	public static int MALE = 2;
	
	public Dictionaries() {

		maleHonorifics = Sets.newHashSet();
		femaleHonorifics = Sets.newHashSet();
		generalHonorifics = Sets.newHashSet();

		honorifics = Sets.newHashSet();
		maleHonorifics.add("mr.");
		maleHonorifics.add("mr");
		maleHonorifics.add("mister");
		maleHonorifics.add("lord");
		maleHonorifics.add("uncle");

		femaleHonorifics.add("ms.");
		femaleHonorifics.add("ms");
		femaleHonorifics.add("mrs.");
		femaleHonorifics.add("mrs");
		femaleHonorifics.add("miss");
		femaleHonorifics.add("madam");
		femaleHonorifics.add("lady");
		femaleHonorifics.add("aunt");

		generalHonorifics.add("dr.");
		generalHonorifics.add("dr");
		generalHonorifics.add("prof.");
		generalHonorifics.add("prof");
		generalHonorifics.add("professor");

		honorifics.addAll(maleHonorifics);
		honorifics.addAll(femaleHonorifics);
		honorifics.addAll(generalHonorifics);

		sentientLemmas = Sets.newHashSet();
		sentientLemmas.add("say");
		sentientLemmas.add("throw");
		sentientLemmas.add("ask");
		sentientLemmas.add("smile");
		sentientLemmas.add("decide");
		sentientLemmas.add("bid");
		sentientLemmas.add("whisper");
		sentientLemmas.add("speak");
		sentientLemmas.add("smile");
		sentientLemmas.add("think");
		sentientLemmas.add("cry");
		sentientLemmas.add("mutter");
	}
	
	public int getGender(HashMap<String, Integer> nameCounts) {
		int male = 0;
		int female = 0;
		int total = 0;

		for (String name : nameCounts.keySet()) {
			int count = nameCounts.get(name);
			total += count;

			String[] parts = name.toLowerCase().split(" ");
			for (String p : parts) {
				if (genderUnigrams.containsKey(p)) {
					int gender = genderUnigrams.get(p);
					if (gender == MALE) {
						male += count * (1. / parts.length);
					} else if (gender == FEMALE) {
						female += count * (1. / parts.length);
					}

				}
				if (maleHonorifics.contains(p)) {
					male += 1000 * count;
				}
				if (femaleHonorifics.contains(p)) {
					female += 1000 * count;
				}
			}

		}

		double mf = male + female;
		if (mf > 0) {
			double ratio = ((double) male) / (male + female);

			if (ratio > .6) {
				return MALE;
			} else if (ratio < .4) {
				return FEMALE;
			}
		}
		
		return 0;

	}
	
	public static int getPronounGender(Token pronoun) {
		int gender = 0;
		if (pronoun.lemma.equals("she")) {
			gender = FEMALE;
		} else if (pronoun.lemma.equals("he")) {
			gender = MALE;
		}
		return gender;
	}
	
	public void processHonorifics(ArrayList<Token> tokens) {

		for (int i = 0; i < tokens.size() - 1; i++) {
			Token token = tokens.get(i);
			Token next = tokens.get(i + 1);
			if (next.isPersonOrOrg()) {
				if (maleHonorifics.contains(token.word.toLowerCase())) {
					token.ner = "PERSON";
					token.gender = 2;
				} else if (femaleHonorifics.contains(token.word.toLowerCase())) {
					token.ner = "PERSON";
					token.gender = 1;
				} else if (generalHonorifics.contains(token.word.toLowerCase())) {
					token.ner = "PERSON";
				}
			}
		}
	}
	public void readAnimate(String infile, String genderFile, String maleFile, String femaleFile) {
		animateUnigrams = new HashSet<String>();
		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(infile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				animateUnigrams.add(str1.trim());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		genderUnigrams = new HashMap<String, Integer>();
		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(genderFile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				String[] parts = str1.trim().split("\t");
				if (parts[1].equals("MALE")) {
					genderUnigrams.put(parts[0], MALE);
				} else if (parts[1].equals("FEMALE")) {
					genderUnigrams.put(parts[0], FEMALE);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(maleFile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				genderUnigrams.put(str1.trim(), MALE);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(femaleFile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				genderUnigrams.put(str1.trim(), FEMALE);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	
	
}
