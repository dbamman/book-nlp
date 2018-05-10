package novels.supersense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.google.common.collect.Sets;

/**
 * Features over the transitions between states (here, first-order features
 * since Viterbi decoding is also first-order)
 * 
 * @author dbamman
 * 
 */
public class TransitionFeatures extends FE {

	public HashSet<String> extractFeatures(ArrayList<HashMap<String,String>> input, Integer[] labels,
			int index) {

		HashSet<String> features = Sets.newHashSet();

		String priorLabel1 = "PRIOR_L:START";

		if (index > 0) {
			priorLabel1 = "PRIOR_L:"
					+ labels[index - 1];
		}

		features.add(priorLabel1);

		return features;
	}

}
