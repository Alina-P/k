
package generated;

import java.util.Map;

import org.w3c.dom.Element;

import ro.uaic.info.fmse.k2m.tag.Tag;

/**
 * @author andrei.arusoaie
 *
 */
public class ANNOS_annos_TAG extends Tag {

	public ANNOS_annos_TAG(Element element, Map<String, String> consMap) {
		super(element, consMap);
	}
	
	@Override
	public String toMaude() throws Exception {
		return processToMaudeAsSeparatedList(" ");
	}
}
