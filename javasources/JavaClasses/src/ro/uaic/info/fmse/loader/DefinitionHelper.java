package ro.uaic.info.fmse.loader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ro.uaic.info.fmse.k.Production;

public class DefinitionHelper {
	public static String generatedTags[] = {
		"cons",
		"kgeneratedlabel",
		"prefixlabel",
	};
	
	public static String specialTerminals[] = {
		"(",
		")",
		",",
		"[",
		"]",
		"{",
		"}",
	};
	
	public static java.util.Map<String, Production> conses = new HashMap<String, Production>();
	public static java.util.Map<String, Production> listConses = new HashMap<String, Production>();
	// contains a mapping from listSort to list separator
	public static java.util.Set<Subsort> subsorts = Subsort.getDefaultSubsorts();

	public static boolean isListSort(String sort) {
		return DefinitionHelper.listConses.containsKey(sort);
	}

	public static void addSubsort(String bigSort, String smallSort) {
		// add the new subsorting
		subsorts.add(new Subsort(bigSort, smallSort));

		// closure for sorts
		boolean finished = false;
		while (!finished) {
			finished = true;
			Set<Subsort> ssTemp = new HashSet<Subsort>();
			for (Subsort s1 : subsorts) {
				for (Subsort s2 : subsorts) {
					if (s1.getBigSort().equals(s2.getSmallSort())) {
						Subsort sTemp = new Subsort(s2.getBigSort(), s1.getSmallSort());
						if (!subsorts.contains(sTemp)) {
							ssTemp.add(sTemp);
							finished = false;
						}
					}
				}
			}
			subsorts.addAll(ssTemp);
		}
	}

	public static boolean isSubsorted(String bigSort, String smallSort) {
		return subsorts.contains(new Subsort(bigSort, smallSort));
	}

	public static boolean isTagGenerated(String key) {
		return (Arrays.binarySearch(generatedTags, key) >= 0);
	}

	public static boolean isSpecialTerminal(String terminal) {
		return (Arrays.binarySearch(specialTerminals, terminal) >= 0);
	}
	
	public static String latexify(String name) {
		return name.replace("_","\\_").replace("{","\\{").replace("}", "\\}").replace("#", "\\#").replace("%", "\\%").replace(
				"$", "\\$").replace("&", "\\&").replace("~", "\\mbox{\\~{}}").replace("^", "\\mbox{\\^{}}");
	}	
}
