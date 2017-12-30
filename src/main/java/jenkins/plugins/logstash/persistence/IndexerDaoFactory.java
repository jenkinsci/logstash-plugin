/*
 * The MIT License
 *
 * Copyright 2014 Rusty Gerard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.logstash.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jenkins.plugins.logstash.persistence.LogstashIndexerDao.IndexerType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Factory for AbstractLogstashIndexerDao objects.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public final class IndexerDaoFactory {
  private static AbstractLogstashIndexerDao instance = null;

  private static final Map<IndexerType, Class<?>> INDEXER_MAP;
  static {
    Map<IndexerType, Class<?>> indexerMap = new HashMap<IndexerType, Class<?>>();

    indexerMap.put(IndexerType.REDIS, RedisDao.class);
    indexerMap.put(IndexerType.RABBIT_MQ, RabbitMqDao.class);
    indexerMap.put(IndexerType.ELASTICSEARCH, ElasticSearchDao.class);
    indexerMap.put(IndexerType.SYSLOG, SyslogDao.class);

    INDEXER_MAP = Collections.unmodifiableMap(indexerMap);
  }

  /**
   * Singleton instance accessor.
   *
   * @param type
   *          The type of indexer, not null
   * @param host
   *          The host name or IP address of the indexer, not null
   * @param port
   *          The port the indexer listens on
   * @param key
   *          The subcollection to write to in the indexer, not null
   * @param username
   *          The user name to authenticate with the indexer, nullable
   * @param password
   *          The password to authenticate with the indexer, nullable
   * @return The instance of the appropriate indexer DAO, never null
   * @throws InstantiationException
   */
  @SuppressFBWarnings(value="BX_UNBOXING_IMMEDIATELY_REBOXED", justification="Code becomes obsolete when changing to GlobalConfiguration")
  public static synchronized LogstashIndexerDao getInstance(IndexerType type, String host, Integer port, String key, String username, String password) throws InstantiationException {
    if (!INDEXER_MAP.containsKey(type)) {
      throw new InstantiationException("[logstash-plugin]: Unknown IndexerType '" + type + "'. Did you forget to configure the plugin?");
    }

    // Prevent NPE
    port = (port == null ? -1 : port.intValue());

    if (shouldRefreshInstance(type, host, port, key, username, password)) {
      try {
        Class<?> indexerClass = INDEXER_MAP.get(type);
        Constructor<?> constructor = indexerClass.getConstructor(String.class, int.class, String.class, String.class, String.class);
        instance = (AbstractLogstashIndexerDao) constructor.newInstance(host, port, key, username, password);
      } catch (NoSuchMethodException e) {
        throw new InstantiationException(ExceptionUtils.getRootCauseMessage(e));
      } catch (InvocationTargetException e) {
        throw new InstantiationException(ExceptionUtils.getRootCauseMessage(e));
      } catch (IllegalAccessException e) {
        throw new InstantiationException(ExceptionUtils.getRootCauseMessage(e));
      }
    }

    return instance;
  }

  private static boolean shouldRefreshInstance(IndexerType type, String host, int port, String key, String username, String password) {
    if (instance == null) {
      return true;
    }

    boolean matches = (instance.getIndexerType() == type) &&
      StringUtils.equals(instance.host, host) &&
      (instance.port == port) &&
      StringUtils.equals(instance.key, key) &&
      StringUtils.equals(instance.username, username) &&
      StringUtils.equals(instance.password, password);
    return !matches;
  }
}
