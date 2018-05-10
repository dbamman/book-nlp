package novels.supersense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Sets;

/**
 * Features on token identities
 * 
 * @author dbamman
 * 
 */
public class POSFeature extends FE {

	public HashSet<String> extractFeatures(
			ArrayList<HashMap<String, String>> input, Integer[] labels,
			int index) {

		HashSet<String> features = Sets.newHashSet();

		int window=SupersenseFeatures.WINDOW;
		int start = index - window;
		if (start < 0) {
			start = 0;
		}

		int end = index + window;
		if (end > input.size() - 1) {
			end = input.size() - 1;
		}

		for (int i = start; i <= end; i++) {
			int offset = index - i;
			String featureName;

			String pos = input.get(i).get("pos").toLowerCase();
			
			// Semcor POS is weird (irregular NNS usage), so only use coarse information.
//			featureName = String.format("pos:position:%s:%s", offset, pos);
//			features.add(featureName);

			String cpos = String.valueOf(pos.charAt(0));
			featureName = String.format("cpos:position:%s:%s", offset, cpos);
			features.add(featureName);

			if (pos.equals("nn") || pos.equals("nns")) {
				featureName = String.format("common_noun:%s", offset);
				features.add(featureName);
			}
			if (pos.equals("nnp") || pos.equals("nnps")) {
				featureName = String.format("proper_noun:%s", offset);
				features.add(featureName);
			}

		}

		return features;
	}

}
