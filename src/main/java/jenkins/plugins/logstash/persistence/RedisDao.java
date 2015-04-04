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

import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Redis Data Access Object.
 *
 * @author Rusty Gerard
 * @since 1.0.0
 */
public class RedisDao extends AbstractLogstashIndexerDao {
  final JedisPool pool;

  //primary constructor used by indexer factory
  public RedisDao(String host, int port, String key, String username, String password) {
    this(null, host, port, key, username, password);
  }

  // Factored for unit testing
  RedisDao(JedisPool factory, String host, int port, String key, String username, String password) {
    super(host, port, key, username, password);

    if (StringUtils.isBlank(key)) {
      throw new IllegalArgumentException("redis key is required");
    }

    // The JedisPool must be a singleton
    // We assume this is used as a singleton as well
    pool = factory == null ? new JedisPool(new JedisPoolConfig(), host, port) : factory;
  }

  @Override
  public long push(String data, PrintStream logger) {
    Jedis jedis = null;
    boolean connectionBroken = false;
    try {
      jedis = pool.getResource();
      if (!StringUtils.isBlank(password)) {
        jedis.auth(password);
      }

      jedis.connect();
      long result = jedis.rpush(key, data);
      jedis.disconnect();

      return result;
    } catch (JedisException e) {
      logger.println(ExceptionUtils.getStackTrace(e));

      connectionBroken = (e instanceof JedisConnectionException);
    } finally {
      if (jedis != null) {
        if (connectionBroken) {
          pool.returnBrokenResource(jedis);
        } else {
          pool.returnResource(jedis);
        }
      }
    }

    return -1;
  }

  @Override
  public IndexerType getIndexerType() {
    return IndexerType.REDIS;
  }
}
