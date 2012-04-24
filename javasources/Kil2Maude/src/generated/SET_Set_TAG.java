
package generated;

import java.util.Map;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.k2m.tag.Tag;

/**
 * @author andrei.arusoaie
 *
 */
public class SET_Set_TAG extends Tag {

	public SET_Set_TAG(Element element, Map<String, String> consMap) {
		super(element, consMap);
	}
	
	@Override
	public String toMaude() throws Exception {
		 if (getChildren().size() == 1)
			return processToMaudeAsSeparatedList("");
		
		return "__(" + processToMaudeAsSeparatedList(",") + ")";
	}
}
