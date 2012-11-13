package cz.artin.cgql.server;

import static com.google.common.base.Preconditions.checkNotNull;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian.GuardContext;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;

/**
 * @author Pavel Cernocky
 *
 */
@Portable
public class GroovyEntryProcessor implements EntryProcessor {

  private static final long serialVersionUID = 1L;

  @PortableProperty(1)
  private String expr;

  public GroovyEntryProcessor() {
  }

  public GroovyEntryProcessor(String expr) {
    this.expr = checkNotNull(expr);
  }

  @Override
  public Object process(Entry entry) {
    GroovyShell shell = new GroovyShell();
    Script script = shell.parse(expr);
    script.getBinding().setVariable("entry", entry);
    return script.run();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public Map processAll(Set entries) {
    GuardContext ctxGuard = GuardSupport.getThreadContext();
    long cMillis = (ctxGuard == null) ? 0L : ctxGuard.getTimeoutMillis();

    GroovyShell shell = new GroovyShell();
    Script script = shell.parse(expr);
    Map<Object, Object> results = Maps.newHashMapWithExpectedSize(entries.size());

    for (Entry entry : (Set<Entry>) entries) {
      script.getBinding().setVariable("entry", entry);
      Object res = script.run();
      results.put(entry.getKey(), res);

      if (ctxGuard != null) {
        ctxGuard.heartbeat(cMillis);
      }
    }

    return results;
  }

}
