package novels.supersense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public abstract class FE {

	public abstract HashSet<String> extractFeatures(ArrayList<HashMap<String,String>> input,
			Integer[] labels, int index);
	
}
