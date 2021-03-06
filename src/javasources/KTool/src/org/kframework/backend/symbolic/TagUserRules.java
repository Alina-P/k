package org.kframework.backend.symbolic;

import org.kframework.kil.ASTNode;
import org.kframework.kil.Attribute;
import org.kframework.kil.Attributes;
import org.kframework.kil.Rule;
import org.kframework.kil.loader.Constants;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.CopyOnWriteTransformer;
import org.kframework.kil.visitors.exceptions.TransformerException;
import org.kframework.utils.file.KPaths;
import org.kframework.utils.general.GlobalSettings;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;


/**
 * Tag all the rules which are not part of K "dist/include" files with
 * 'symbolic' attribute. All the rules tagged with symbolic will suffer the
 * symbolic execution transformation steps.
 * 
 * @author andreiarusoaie
 */
public class TagUserRules extends CopyOnWriteTransformer {

	public static final Set<String> notSymbolicTags;
    static {
        if (GlobalSettings.javaBackend) {
            notSymbolicTags = Collections.emptySet();
        } else {
            notSymbolicTags = ImmutableSet.of(
                    Constants.MACRO,
                    Constants.FUNCTION,
                    Constants.STRUCTURAL,
                    Constants.ANYWHERE,
                    SymbolicBackend.NOTSYMBOLIC);
            if (!GlobalSettings.nonSymbolicTags.isEmpty()) {
                notSymbolicTags.addAll(GlobalSettings.nonSymbolicTags);
            }
        }
    }

	public TagUserRules(Context context) {
		super("Tag rules which are not builtin with 'symbolic' tag", context);
	}

	@Override
	public ASTNode transform(Rule node) throws TransformerException {

		for (String nst : notSymbolicTags)
			if (node.containsAttribute(nst)) {
				return super.transform(node);
			}

		if ((!node.getFilename().startsWith(
				KPaths.getKBase(false) + File.separator + "include")
				&& !node.getFilename().startsWith(
						org.kframework.kil.loader.Constants.GENERATED_FILENAME))
                || (GlobalSettings.javaBackend && node.getFilename().equals(
                        KPaths.getKBase(false) + File.separator + "include" +
                                File.separator + "io" +
                                File.separator + "io.k"))
                ) {

            // this handles the case when the user wants to
            // specify exactly what rules should be transformed
            // symAllowed is true when the rule is tagged in this purpose
            boolean symAllowed = false;
            for (String st : GlobalSettings.symbolicTags) {
                if (node.containsAttribute(st)) {
                    symAllowed = true;
                }
            }
            // the first condition might not be needed, but we keep it
            // to ensure that, by default, if no rules (identified by tags)
            // are specified, then all rules are transformed by symbolic steps.
            if (!GlobalSettings.symbolicTags.isEmpty() && !symAllowed) {
                return super.transform(node);
            }

            List<Attribute> attrs = node.getAttributes().getContents();
			attrs.add(new Attribute(SymbolicBackend.SYMBOLIC, ""));

			Attributes atts = node.getAttributes().shallowCopy();
			atts.setContents(attrs);

			node = node.shallowCopy();
			node.setAttributes(atts);
			return node;
		}

		return super.transform(node);
	}
}
