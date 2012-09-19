package k3.loader;

import org.kframework.kil.Attributes;

public class SDFHelper {
	public static String getSDFAttributes(Attributes attrs) {
		String str = " {";
		if (attrs.getContents().size() == 0)
			return "";

		if (attrs.containsKey("prefer"))
			str += "prefer, ";
		if (attrs.containsKey("avoid"))
			str += "avoid, ";
		if (attrs.containsKey("left"))
			str += "left, ";
		if (attrs.containsKey("right"))
			str += "right, ";
		if (attrs.containsKey("non-assoc"))
			str += "non-assoc, ";
		if (attrs.containsKey("bracket"))
			str += "bracket, ";
		if (attrs.containsKey("cons"))
			str += "cons(" + attrs.get("cons") + "), ";

		if (str.endsWith(", "))
			return str.substring(0, str.length() - 2) + "}";
		else
			return str + "}";
	}
}
