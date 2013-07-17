package org.kframework.kil;

import java.io.Serializable;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


/**
 * A data structure sort. Each data structure sort is hooked to one of the following builtin
 * data structures: Bag, List, Map or Set (an element of {@code TYPES}).
 * Each data structure sort must provide the following primitive operations:
 * <p>
 * (1) constructor: takes two data structures and constructs the union (bag, map, set) or the
 *     concatenation (list);
 * (2) element: takes one K term (bag, list, set) or two K terms (map) and constructs an element
 *     (bag, list, set) or an entry (map);
 * (3) unit: constructs the empty data structure.
 * </p>
 * Additionally, a data structure sort may provide other hooked operations. Each backend must
 * implement these builtin data structures types.
 *
 * @author AndreiS
 */
public class DataStructureSort implements Serializable {

    public enum Label { CONSTRUCTOR, ELEMENT, UNIT }

    /** {@code Set} of builtin data structure types */
    public static final java.util.Set<String> TYPES = ImmutableSet.of(
            KSorts.BAG,
            KSorts.LIST,
            KSorts.MAP,
            KSorts.SET);

    /**
     * {@code Map} of builtin data structure types (Bag, List, Set, Map) to fundamental hooks
     * (builtin data structure constructor, element constructor, empty data structure constructor).
     * The full name of a hook is obtained by using the builtin module name as a prefix (e.g.
     * Map:__).
     */
    public static final Map<String, ImmutableMap<Label, String>> LABELS = ImmutableMap.of(
            KSorts.BAG, ImmutableMap.of(
                    Label.CONSTRUCTOR, "__",
                    Label.ELEMENT, "BagItem",
                    Label.UNIT, ".Bag"),
            KSorts.LIST, ImmutableMap.of(
                    Label.CONSTRUCTOR, "__",
                    Label.ELEMENT, "ListItem",
                    Label.UNIT, ".List"),
            KSorts.MAP, ImmutableMap.of(
                    Label.CONSTRUCTOR, "__",
                    Label.ELEMENT, "_|->_",
                    Label.UNIT, ".Map"),
            KSorts.SET, ImmutableMap.of(
                    Label.CONSTRUCTOR, "__",
                    Label.ELEMENT, "SetItem",
                    Label.UNIT, ".Set"));

    /** Name of this data structure sort. */
    private final String name;
    /** Type of the builtin data structure this sort is hooked to (an element of {@code TYPES}). */
    private final String type;
    /** {@code String} representation of the data structure constructor KLabel. */
    private final String constructorLabel;
    /** {@code String} representation of the data structure element KLabel*/
    private final String elementLabel;
    /** {@code String} representation of the empty data structure KLabel. */
    private final String unitLabel;
    /** {@code Map} of the remaining KLabels hooked to to builtin operations */
    private final ImmutableMap<String, String> operatorLabels;

    public DataStructureSort(
            String name,
            String type,
            String constructorLabel,
            String elementLabel,
            String unitLabel,
            Map<String, String> operatorLabels) {
        assert TYPES.contains(type): "unknown builtin collection type " + type;

        this.name = name;
        this.type = type;
        this.constructorLabel = constructorLabel;
        this.elementLabel = elementLabel;
        this.unitLabel = unitLabel;
        this.operatorLabels = ImmutableMap.copyOf(operatorLabels);
    }

    public String constructorLabel() {
        return constructorLabel;
    }

    public String elementLabel() {
        return elementLabel;
    }

    public String name() {
        return name;
    }

    public Map<String, String> operatorLabels() {
        return operatorLabels;
    }

    public String type() {
        return type;
    }

    public String unitLabel() {
        return unitLabel;
    }

}
