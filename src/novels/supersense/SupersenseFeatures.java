package novels.supersense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Manage all features used in training/decoding (in initalizeFeatures)
 * @author dbamman
 *
 */
public class SupersenseFeatures {

	public static String BIAS = "BIASER";
	public static int WINDOW=1;


	public static HashSet<String> borderTokens;

	static {

		borderTokens = Sets.newHashSet();
		borderTokens.add(".");
		borderTokens.add("!");
		borderTokens.add("``");
		borderTokens.add("''");
		borderTokens.add(";");
		borderTokens.add("?");
		
		initializeFeatures();

	}
	
	static List<FE> featureExtractors;
	public static void initializeFeatures() {
		featureExtractors=Lists.newArrayList();
		featureExtractors.add(new TokenFeature());
		featureExtractors.add(new POSFeature());
		featureExtractors.add(new WordNet());
		featureExtractors.add(new TransitionFeatures());
		featureExtractors.add(new ShapeFeature());
	}

	public static HashSet<String> extractFeatures(ArrayList<HashMap<String,String>> input,
			Integer[] labels, int index) {

		
		HashSet<String> features = Sets.newHashSet();
		
		features.add(BIAS);

		for (FE fe : featureExtractors) {
			features.addAll(fe.extractFeatures(input, labels, index));
		}


		return features;
	}
}
