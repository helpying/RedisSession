package zh.redis.httpsession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

abstract interface RequestEventObserver{
  public abstract void completed(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse);
}