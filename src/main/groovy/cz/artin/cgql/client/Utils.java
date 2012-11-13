package cz.artin.cgql.client;

import java.util.Map.Entry;

import com.google.common.base.Function;

/**
 * @author Pavel Cernocky
 *
 */
public abstract class Utils {

  private Utils() {
  }

  @SuppressWarnings("unchecked")
  public static <K, V> Function<Entry<K, V>, V> entryValueFunction() {
    return (Function<Entry<K, V>, V>) ENTRY_VALUE_FUNCTION;
  }

  private static final Function<?, ?> ENTRY_VALUE_FUNCTION = new Function<Entry<Object, Object>, Object>() {
    @Override
    public Object apply(Entry<Object, Object> input) {
      return input.getValue();
    }
  };

}
