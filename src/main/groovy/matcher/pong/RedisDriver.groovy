package matcher.pong
import redis.clients.jedis.*
import groovy.json.*

class RedisDriver {
  private Jedis redis
  private key
  private slurper = new JsonSlurper()

  static def fromEnv(key) {
    def conf = config()

    // workaround until https://github.com/xetorthio/jedis/pull/658 is released
    def jedis = conf.containsKey("password")
    ? new Jedis(uri())
    : new Jedis(conf.hostname, new Integer(conf.port))

    new RedisDriver(jedis, key)
  }

  static def uri() {
    def conf = config()
    "redis://:${conf.password}@${conf.hostname}:${conf.port}/0"
  }

  static def config() {
    def vcap_services = new JsonSlurper().parseText(
      System.getenv("VCAP_SERVICES") ?: defaultVcapServices()
    )
    vcap_services.rediscloud[0].credentials
  }

  static def defaultVcapServices() {
    def builder = new JsonBuilder()
    builder(rediscloud: [ [ credentials: [ hostname: "localhost", port: "6379" ] ] ])
    builder.toString()
  }

  RedisDriver(jedis, a_key) {
    key = a_key
    redis = jedis
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
