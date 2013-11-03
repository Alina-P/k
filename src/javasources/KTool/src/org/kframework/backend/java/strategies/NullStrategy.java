package org.kframework.backend.java.strategies;

import org.kframework.backend.java.kil.Rule;

import java.util.Collection;
import java.util.Iterator;

public class NullStrategy extends Strategy {
  public NullStrategy() {
  }

  public void apply(Collection<Rule> rules) {
    this.rules = rules;
  }

  public Collection<Rule> next() {
    Collection<Rule> n = rules;
    rules = null;
    return n;
  }

  public boolean hasNext() {
    return rules != null;
  }

  private Collection<Rule> rules;
} 
