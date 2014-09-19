package matcher.pong
import redis.clients.jedis.*
import groovy.json.*

class RedisDriver {
  private redis = new Jedis("localhost")
  private key
  private slurper = new JsonSlurper()

  RedisDriver(s) {
    key = s
  }

  def leftShift(element) {
    def builder = new JsonBuilder()
    builder(element)
    redis.rpush(key, builder.toString())
  }

  def clear() {
    redis.del(key)
  }

  def size() {
    redis.llen(key)
  }

  Iterator iterator() {
    elements().iterator()
  }

  private

  def elements() {
    redis.lrange(key, 0, size() - 1).collect {
      slurper.parseText(it)
    }
  }
}
