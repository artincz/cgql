package cz.artin.cgql.server;

import static com.google.common.base.Preconditions.checkNotNull;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.Map.Entry;

import org.codehaus.groovy.control.CompilerConfiguration;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;
import com.tangosol.util.filter.EntryFilter;

/**
 * @author Pavel Cernocky
 *
 */
@Portable
public class GroovyFilter implements EntryFilter {

  @PortableProperty(1)
  private String expression;

  private Script script;

  public GroovyFilter() {
  }

  public GroovyFilter(String expression) {
    this.expression = checkNotNull(expression);
  }

  public String getExpression() {
    return expression;
  }

  @Override
  public boolean evaluate(Object value) {
    Binding binding = new Binding();
    binding.setVariable("value", value);
    return eval(binding);
  }

  @Override
  public boolean evaluateEntry(@SuppressWarnings("rawtypes") Entry entry) {
    Binding binding = new Binding();
    binding.setVariable("entry", entry);
    return eval(binding);
  }

  private synchronized boolean eval(Binding binding) {
    Script script = getScript();
    script.setBinding(binding);
    return script.run() == Boolean.TRUE;
  }

  private Script getScript() {
    if (script == null) {
      CompilerConfiguration compilerConfig = new CompilerConfiguration();
      GroovyShell shell = new GroovyShell(compilerConfig);
      script = shell.parse(expression);
    }
    return script;
  }

  @Override
  public String toString() {
    return GroovyFilter.class.getSimpleName() + "[" + expression + "]";
  }

}
