package cz.artin.cgql.client

import java.util.Map.Entry

import com.google.common.collect.Collections2
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.tangosol.net.CacheFactory
import com.tangosol.net.NamedCache
import com.tangosol.util.Filter
import com.tangosol.util.InvocableMap.EntryProcessor
import com.tangosol.util.extractor.KeyExtractor
import com.tangosol.util.filter.AlwaysFilter
import com.tangosol.util.filter.InFilter
import com.tangosol.util.processor.ConditionalRemove

import cz.artin.cgql.server.GroovyEntryProcessor;
import cz.artin.cgql.server.GroovyFilter;

/**
 * @author Pavel Cernocky
 *
 */
abstract class CgqlScript extends Script {

  static final String IMPORTS = '''\
import com.tangosol.util.filter.*
import com.tangosol.util.extractor.*
import com.tangosol.util.processor.*
import com.tangosol.util.aggregator.*
import com.google.common.collect.*
'''

  NamedCache cache(String cacheName) {
    this.cache = cacheName
    return getCoherenceCache()
  }

  private NamedCache getCoherenceCache() {
    if (!cache) {
      throw new MessageException("Cache is not set, use command: cache 'TestCache'")
    }
    return CacheFactory.getCache(cache)
  }

  void key(Object key) {
    this.filter = key
  }

  void keys(Object... keys) {
    this.keys(Arrays.asList(keys))
  }

  void keys(Iterable keys) {
    this.keys(Lists.newArrayList(keys))
  }

  void keys(Collection keys) {
    this.filter = keys
  }

  void filter(String filterString) {
    filter(new GroovyFilter(filterString))
  }

  void filter(Filter filter) {
    this.filter = filter
  }

  int count() {
    return keys().size()
  }

  Collection keys() {
    def cache = getCoherenceCache()

    if (filter == null) {
      Set<Object> keys = cache.keySet()
      return keys
    }
    else if (filter instanceof Filter) {
      Set<Object> keys = cache.keySet(filter)
      return keys
    }
    else if (filter instanceof Collection) {
      Set<Object> keys = cache.keySet(new InFilter(new KeyExtractor(), Sets.newHashSet(filter)))
      return keys
    }
    else {
      return cache.containsKey(filter) ? Collections.singletonList(filter) : Collections.emptyList()
    }
  }

  Collection values() {
    if (filter == null) {
      Set<Entry> entries = getCoherenceCache().entrySet()
      return Collections2.transform(entries, Utils.entryValueFunction())
    }
    else if (filter instanceof Filter) {
      Set<Entry> entries = getCoherenceCache().entrySet(filter)
      return Collections2.transform(entries, Utils.entryValueFunction())
    }
    else if (filter instanceof Collection) {
      Map<Object, Object> entries = getCoherenceCache().getAll(filter)
      return entries.values()
    }
    else {
      return Collections.singletonList(getCoherenceCache().get(filter))
    }
  }

  Map entries() {
    if (filter == null) {
      Set<Entry> entries = getCoherenceCache().entrySet()
      return createMapFromEntries(entries)
    }
    else if (filter instanceof Filter) {
      Set<Entry> entries = getCoherenceCache().entrySet(filter)
      return createMapFromEntries(entries)
    }
    else if (filter instanceof Collection) {
      Map<Object, Object> entries = getCoherenceCache().getAll(filter)
      return entries
    }
    else {
      return Collections.singletonMap(filter, getCoherenceCache().get(filter))
    }
  }

  Map process(EntryProcessor entryProcessor) {
    if (filter == null) {
      throwExplicitFilterException()
    }
    else if (filter instanceof Filter) {
      return getCoherenceCache().invokeAll(filter, entryProcessor)
    }
    else if (filter instanceof Collection) {
      return getCoherenceCache().invokeAll(filter, entryProcessor)
    }
    else {
      return Collections.singletonMap(filter, getCoherenceCache().invoke(filter, entryProcessor))
    }
  }

  Map process(String entryProcessorString) {
    process(new GroovyEntryProcessor(entryProcessorString))
  }

  void put(Object key, Object value) {
    getCoherenceCache().put(key, value)
  }

  Map delete() {
    return process(new ConditionalRemove(AlwaysFilter.INSTANCE))
  }

  private static Map createMapFromEntries(Set<Entry> entries) {
    Map map = Maps.newHashMapWithExpectedSize(entries.size())
    for (Entry entry : entries) {
      map.put(entry.key, entry.value)
    }
    return map
  }

  private void throwExplicitFilterException() {
    throw new MessageException('Write operations must have explicit filter or keys defined, use AlwaysFilter if you want to process all entries')
  }

}
