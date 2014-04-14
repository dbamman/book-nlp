package novels.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;

public class Util {

	public static HashMap<String, String> readDocPaths(String infile) {
		BufferedReader in1;
		StringBuffer buffer = new StringBuffer();
		HashMap<String, String> paths = Maps.newHashMap();
		try {
			in1 = new BufferedReader(new InputStreamReader(new FileInputStream(
					infile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				try {
					String[] parts = str1.trim().split("\t");
					paths.put(parts[0], parts[1]);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {

		}
		return paths;
	}

	public static String filterGutenberg(String doc) {
		String headerRegex="^.*START OF THIS PROJECT GUTENBERG .*? \\*\\*\\*";
		Pattern pattern=Pattern.compile(headerRegex, Pattern.DOTALL);
		Matcher matcher=pattern.matcher(doc);
		doc=matcher.replaceAll("");

		String tailRegex="\\*\\*\\* END OF THIS PROJECT GUTENBERG .*$";
		pattern=Pattern.compile(tailRegex, Pattern.DOTALL);
		matcher=pattern.matcher(doc);
		doc=matcher.replaceAll("");

		return doc;
	}

	public static String readText(String infile) {
		BufferedReader in1;
		StringBuffer buffer = new StringBuffer();

		try {
			in1 = new BufferedReader(new InputStreamReader(new FileInputStream(
					infile), "UTF-8"));
			String str1;

			while ((str1 = in1.readLine()) != null) {
				try {
					buffer.append(str1.trim() + "\n");

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {

		}
		return buffer.toString();
	}

	public static double add_log(double logA, double logB) {
		if (logA < logB) {
			return logB + Math.log(1 + Math.exp(logA - logB));
		} else {
			return logA + Math.log(1 + Math.exp(logB - logA));
		}
	}

	public static ArrayList<Object> sortHashMapByValue(HashMap<?, ?> hm) {

		ArrayList<Object> sortedCollocates = new ArrayList<Object>();

		Set entries2 = hm.entrySet();

		Map.Entry[] entries = new Map.Entry[entries2.size()];

		Iterator<Map.Entry> it = entries2.iterator();
		int n = 0;
		while (it.hasNext()) {
			entries[n] = it.next();
			n++;
		}

		Arrays.sort(entries, new Comparator() {
			public int compare(Object lhs, Object rhs) {
				Map.Entry le = (Map.Entry) lhs;
				Map.Entry re = (Map.Entry) rhs;
				return ((Comparable) re.getValue()).compareTo((Comparable) le
						.getValue());
			}
		});

		for (int i = 0; i < entries.length; i++) {
			Map.Entry<Object, Integer> entry = entries[i];
			sortedCollocates.add(entry.getKey());
		}

		return sortedCollocates;

	}

}
