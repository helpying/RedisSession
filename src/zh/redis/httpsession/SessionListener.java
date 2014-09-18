package zh.redis.httpsession;

public abstract interface SessionListener
{
  public abstract void onAttributeChanged(RedisHttpSession session);

  public abstract void onInvalidated(RedisHttpSession session);
}
