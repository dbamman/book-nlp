package novels.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stoplist {

	HashSet<String> stoplist;
	
	public boolean isStopword(String word) {
		if (stoplist.contains(word)) {
			return true;
		}
		if (word.matches("[A-Za-z]+")) {
			return false;
		}
		return true;
	}
	public Stoplist(String infile) {
		
		stoplist=new HashSet<String>();
		try {
			BufferedReader in1 = new BufferedReader(new InputStreamReader(new FileInputStream(infile), "UTF-8"));
			String str1;
			
			while ((str1 = in1.readLine()) != null) {
				stoplist.add(str1.trim());
	
			}			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
