package novels.supersense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Sets;

/**
 * Word shape features
 * @author dbamman
 *
 */
public class ShapeFeature extends FE {
	
	public static String getShape(String word) {

		String shape = "-";
		if (word.matches("[a-z]+")) {
			shape = "o";
		} else if (word.matches("[A-Z][a-z]+")) {
			shape = "Xo";
		} else if (word.matches("[A-Z]+")) {
			shape = "X";
		}

		return shape;
	}
	
	public HashSet<String> extractFeatures(ArrayList<HashMap<String,String>> words, Integer[] labels,
			int index) {
		
		HashSet<String> features=Sets.newHashSet();
		
		int start = index - SupersenseFeatures.WINDOW;
		if (start < 0) {
			start = 0;
		}

		int end = index + SupersenseFeatures.WINDOW;
		if (end > words.size() - 1) {
			end = words.size() - 1;
		}
		
		for (int i = start; i <= end; i++) {
			int offset = index - i;

			String featureName = String.format("shape:%s:%s", offset,
					getShape(words.get(i).get("word")));
			features.add(featureName);
			
		}
		
		String shape = getShape(words.get(index).get("word"));
		if (index > 0 && SupersenseFeatures.borderTokens.contains(words.get(index - 1).get("word"))
				&& shape.equals("Xo")) {
			String featureName = String.format("sentenceInitialXo");
			features.add(featureName);
		}
		
		
		return features;
	}

}
