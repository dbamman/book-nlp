package novels.supersense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Sets;

/**
 * Features on token identities
 * @author dbamman
 *
 */
public class TokenFeature extends FE {
	
	public HashSet<String> extractFeatures(ArrayList<HashMap<String,String>> input, Integer[] labels,
			int index) {
		
		HashSet<String> features=Sets.newHashSet();

		int start = index - SupersenseFeatures.WINDOW;
		if (start < 0) {
			start = 0;
		}

		int end = index + SupersenseFeatures.WINDOW;
		if (end > input.size() - 1) {
			end = input.size() - 1;
		}
		
		for (int i = start; i <= end; i++) {
			int offset = index - i;
			String featureName;

			featureName = String.format("position:%s:%s", offset, input.get(i).get("word").toLowerCase());
			features.add(featureName);

		}
		
		return features;
	}

}
