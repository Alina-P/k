package K3Syntax.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

/**
 * Helper class for {@link java_strategy_0_0}.
 */
public class InteropRegisterer extends JavaInteropRegisterer {

  public InteropRegisterer() {
    super(new Strategy[] { java_strategy_0_0.instance, xml_string_escape_from_string_0_0.instance, myorigin_0_0.instance });
  }
}
