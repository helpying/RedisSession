package zh.redis.httpsession;

import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import zh.redis.RedisClient;
import zh.redis.RedisSimpleTempalte;
import org.apache.commons.lang.StringUtils;

public class RedisSessionManager {
	public static final String SESSION_ID_PREFIX = "RJSID_";
	public static final String SESSION_ID_COOKIE = "RSESSIONID";
	private RedisSimpleTempalte redisClient;
	private int expirationUpdateInterval;
	private int maxInactiveInterval;

	public RedisSessionManager() {
		this.expirationUpdateInterval = 300;
		this.maxInactiveInterval = 1800;
	}
	
	public RedisSessionManager(String host,int port) {
		this.expirationUpdateInterval = 300;
		this.maxInactiveInterval = 1800;
		RedisClient rc = new RedisClient(host, port);
		this.redisClient = new RedisSimpleTempalte(rc);
	}

	public void setredisClient(RedisSimpleTempalte redisClient) {
		this.redisClient = redisClient;
	}

	public void setExpirationUpdateInterval(int expirationUpdateInterval) {
		this.expirationUpdateInterval = expirationUpdateInterval;
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public RedisHttpSession createSession(
			SessionHttpServletRequestWrapper request,
			HttpServletResponse response,
			RequestEventSubject requestEventSubject, boolean create) {
		String sessionId = getRequestedSessionId(request);

		RedisHttpSession session = null;
		if ((StringUtils.isEmpty(sessionId)) && (!(create)))
			return null;
		if (StringUtils.isNotEmpty(sessionId))
			session = loadSession(sessionId);

		if ((session == null) && (create))
			session = createEmptySession(request, response);
		
		if (session != null)
            attachEvent(session, request, response, requestEventSubject);
		
		return session;
	}

    private String getRequestedSessionId(HttpServletRequestWrapper request){
        Cookie[] cookies = request.getCookies();
        if(cookies == null || cookies.length==0)return null;
        for(Cookie cookie: cookies){
            if(SESSION_ID_COOKIE.equals(cookie.getName()))return cookie.getValue();
        }
        return null;
    }

	private void saveSession(RedisHttpSession session) {
		String sessionid = generatorSessionKey(session.id);
		try {
			if (session.expired)
				this.redisClient.del(sessionid);
			else
				this.redisClient.set(sessionid, SeesionSerializer.serialize(session), this.maxInactiveInterval);
		} catch (Exception e) {
			throw new SessionException(e);
		}
	}
	
	private void attachEvent(final RedisHttpSession session, final HttpServletRequestWrapper request, final HttpServletResponse response, RequestEventSubject requestEventSubject) {
        session.setListener(new SessionListenerAdaptor(){
            public void onInvalidated(RedisHttpSession session) {
                saveCookie(session, request, response);
            }
        });
        requestEventSubject.attach(new RequestEventObserver() {
            public void completed(HttpServletRequest servletRequest, HttpServletResponse response) {
                int updateInterval = (int) ((System.currentTimeMillis() - session.lastAccessedTime) / 1000);
                if (session.isNew == false && session.isDirty == false && updateInterval < expirationUpdateInterval)
                    return;
                if (session.isNew && session.expired) return;
                session.lastAccessedTime = System.currentTimeMillis();
                saveSession(session);
            }
        });
    }

	private RedisHttpSession createEmptySession(
			SessionHttpServletRequestWrapper request,
			HttpServletResponse response) {
		RedisHttpSession session = new RedisHttpSession();
		session.id = createSessionId();
		session.creationTime = System.currentTimeMillis();
		session.maxInactiveInterval = this.maxInactiveInterval;
		session.isNew = true;
		saveCookie(session, request, response);
		return session;
	}

	private String createSessionId() {
		return UUID.randomUUID().toString().replace("-", "").toUpperCase();
	}

    private void saveCookie(RedisHttpSession session, HttpServletRequestWrapper request, HttpServletResponse response) {
        if (session.isNew == false && session.expired == false) return;

        Cookie cookie = new Cookie(SESSION_ID_COOKIE, null);
        cookie.setPath(request.getContextPath());
        if(session.expired){
            cookie.setMaxAge(0);
        }else if (session.isNew){
            cookie.setValue(session.getId());
		}
        response.addCookie(cookie);
    }

	private RedisHttpSession loadSession(String sessionId) {
		RedisHttpSession session;
		try {
			session = SeesionSerializer.deserialize(this.redisClient.getByte(generatorSessionKey(sessionId)));

			if (session != null) {
				session.isNew = false;
				session.isDirty = false;
			}
			return session;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected static String generatorSessionKey(String sessionId) {
		return SESSION_ID_PREFIX.concat(sessionId);
	}
}