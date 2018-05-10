package novels.supersense;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 * @author dbamman
 * 
 */
public class WordNet extends FE {

	static HashMap<String, String> noun_exceptions;
	static HashMap<String, String> verb_exceptions;
	static HashMap<String, String> mfs;
	static HashMap<String, String[]> valid;

	public String WN_lemmatize(String word, String pos) {
		String lemma = null;
		if (pos.equals("n")) {
			if (noun_exceptions.containsKey(word)) {
				return noun_exceptions.get(word);
			}
			if (word.endsWith("men")) {
				lemma = word.replaceAll("men$", "");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("ies")) {
				lemma = word.replaceAll("ies$", "y");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("shes")) {
				lemma = word.replaceAll("shes$", "sh");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}

			}
			if (word.endsWith("ches")) {
				lemma = word.replaceAll("ches$", "ch");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("zes")) {
				lemma = word.replaceAll("zes$", "z");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("xes")) {
				lemma = word.replaceAll("xes$", "x");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("ses")) {
				lemma = word.replaceAll("ses$", "s");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("s")) {
				lemma = word.replaceAll("s$", "");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}

		}
		if (pos.equals("v")) {
			if (verb_exceptions.containsKey(word)) {
				return verb_exceptions.get(word);
			}
			if (word.endsWith("s")) {
				lemma = word.replaceAll("s$", "");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("ies")) {
				lemma = word.replaceAll("ies$", "y");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("es")) {
				lemma = word.replaceAll("es$", "e");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("es")) {
				lemma = word.replaceAll("es$", "");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("ed")) {
				lemma = word.replaceAll("ed$", "e");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("ed")) {
				lemma = word.replaceAll("ed$", "");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("ing")) {
				lemma = word.replaceAll("ing$", "e");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}
			if (word.endsWith("ing")) {
				lemma = word.replaceAll("ing$", "");
				if (valid.containsKey(lemma + "_" + pos)) {
					return lemma;
				}
			}

		}

		return word;
	}

	static {

		mfs = Maps.newHashMap();
		valid = Maps.newHashMap();
		noun_exceptions = Maps.newHashMap();
		verb_exceptions = Maps.newHashMap();

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream("files/supersense/wordnet.first.sense"), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				try {
					String[] parts = str1.trim().split("\t");
					if (parts.length >= 2) {

						String[] senses = parts[2].split(" ");
						mfs.put(parts[0] + "_" + parts[1], senses[0]);

						valid.put(parts[0] + "_" + parts[1], senses);

						// System.out.println(parts[0] + "\t" + parts[1]);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			in1.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream("files/supersense/verb.exc"), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				try {
					String[] parts = str1.trim().split(" ");
					String form = parts[0];
					String lemma = parts[1];
					verb_exceptions.put(form, lemma);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			in1.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(
					new FileInputStream("files/supersense/noun.exc"), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				try {
					String[] parts = str1.trim().split(" ");
					String form = parts[0];
					String lemma = parts[1];
					noun_exceptions.put(form, lemma);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			in1.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public HashSet<String> extractFeatures(
			ArrayList<HashMap<String, String>> words, Integer[] labels,
			int index) {

		HashSet<String> features = Sets.newHashSet();

		boolean wordnetMatch = false;

		int window = 0; // SupersenseFeatures.WINDOW;
		int start = index - window;
		if (start < 0) {
			start = 0;
		}

		int end = index + window;
		if (end > words.size() - 1) {
			end = words.size() - 1;
		}

		int triStart = index - 2;
		if (triStart < 0) {
			triStart = 0;
		}

		int triEnd = index;
		if (triEnd > words.size() - 1) {
			triEnd = words.size() - 1;
		}
		for (int i = triStart; i <= triEnd; i++) {

			if (i + 2 < words.size() - 1) {
				String token = words.get(i).get("word").toLowerCase();
				String pos = String.valueOf(words.get(i).get("pos")
						.toLowerCase().charAt(0));

				String lemma = WN_lemmatize(token, pos);

				String nextToken = words.get(i + 1).get("word").toLowerCase();
				String nextPos = String.valueOf(words.get(i + 1).get("pos")
						.toLowerCase().charAt(0));

				String nextLemma = WN_lemmatize(nextToken, nextPos);

				String nextnextToken = words.get(i + 2).get("word")
						.toLowerCase();
				String nextnextPos = String.valueOf(words.get(i + 2).get("pos")
						.toLowerCase().charAt(0));

				String nextnextLemma = WN_lemmatize(nextnextToken, nextnextPos);

				String mwe = String.format("%s_%s_%s_%s", lemma, nextLemma,
						nextnextLemma, pos);

				String biokey = "b";
				if (i != index) {
					biokey = "i";
				}

				if (mfs.containsKey(mwe)) {

					wordnetMatch = true;
					String featureName = String.format("%s_mfs:%s:%s", biokey,
							0, mfs.get(mwe));

					features.add(featureName);

				}

				if (valid.containsKey(mwe)) {

					wordnetMatch = true;
					String[] senses = valid.get(mwe);
					for (String sense : senses) {
						String featureName = String.format(
								"%s_mwe_valid_sense:%s:%s", biokey, 0, sense);

						features.add(featureName);
					}

				}

			}

		}

		// Bigrams

		int biStart = index - 1;
		if (biStart < 0) {
			biStart = 0;
		}

		int biEnd = index;
		if (biEnd > words.size() - 1) {
			biEnd = words.size() - 1;
		}
		for (int i = biStart; i <= biEnd; i++) {

			if (i + 2 < words.size() - 1) {
				String token = words.get(i).get("word").toLowerCase();
				String pos = String.valueOf(words.get(i).get("pos")
						.toLowerCase().charAt(0));

				String lemma = WN_lemmatize(token, pos);

				String nextToken = words.get(i + 1).get("word").toLowerCase();
				String nextPos = String.valueOf(words.get(i + 1).get("pos")
						.toLowerCase().charAt(0));

				String nextLemma = WN_lemmatize(nextToken, nextPos);

				String mwe = String.format("%s_%s_%s", lemma, nextLemma, pos);

				String biokey = "b";
				if (i != index) {
					biokey = "i";
				}

				if (mfs.containsKey(mwe)) {

					wordnetMatch = true;
					String featureName = String.format("%s_mfs:%s:%s", biokey,
							0, mfs.get(mwe));

					features.add(featureName);

				}

				if (valid.containsKey(mwe)) {

					wordnetMatch = true;
					String[] senses = valid.get(mwe);
					for (String sense : senses) {
						String featureName = String.format(
								"%s_mwe_valid_sense:%s:%s", biokey, 0, sense);

						features.add(featureName);
					}

				}

			}

		}

		for (int i = start; i <= end; i++) {
			int offset = index - i;

			String token = words.get(i).get("word").toLowerCase();
			String pos = String.valueOf(words.get(i).get("pos").toLowerCase()
					.charAt(0));

			String lemma = WN_lemmatize(token, pos);
			String key = lemma + "_" + pos;

			if (mfs.containsKey(key)) {

				if (i == index) {
					wordnetMatch = true;
				}
				String featureName = String.format("mfs:%s:%s", offset,
						mfs.get(key));

				features.add(featureName);

				// featureName = String.format("mfs_word:%s:%s:%s", lemma,
				// offset,
				// mfs.get(key));

				// features.add(featureName);

			}

			if (valid.containsKey(key)) {

				if (i == index) {
					wordnetMatch = true;
				}
				String[] senses = valid.get(key);
				for (String sense : senses) {
					String featureName = String.format("valid_sense:%s:%s",
							offset, sense);

					features.add(featureName);
				}

			}
		}

		if (wordnetMatch == false) {
			String featureName = String.format("no_wordnet_match");

			features.add(featureName);
		}

		return features;
	}
}
