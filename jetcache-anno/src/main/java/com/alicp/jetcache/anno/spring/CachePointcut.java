/**
 * Created on  13-09-19 20:56
 */
package com.alicp.jetcache.anno.spring;

import com.alicp.jetcache.anno.impl.CacheConfigUtil;
import com.alicp.jetcache.anno.impl.CacheInvokeConfig;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:yeli.hl@taobao.com">huangli</a>
 */
public class CachePointcut extends StaticMethodMatcherPointcut implements ClassFilter {

    private ConcurrentHashMap<Method, CacheInvokeConfig> cacheConfigMap = new ConcurrentHashMap<Method, CacheInvokeConfig>();
    private String[] basePackages;

    public CachePointcut(String[] basePackages) {
        setClassFilter(this);
        this.basePackages = basePackages;
    }

    public boolean matches(Class clazz) {
        if (matchesThis(clazz)) {
            return true;
        }
        Class[] cs = clazz.getInterfaces();
        if (cs != null) {
            for (Class c : cs) {
                if (matches(c)) {
                    return true;
                }
            }
        }
        if (!clazz.isInterface()) {
            Class sp = clazz.getSuperclass();
            if(sp != null && matches(sp)){
                return true;
            }
        }
        return false;
    }

    public boolean matchesThis(Class clazz) {
        String name = clazz.getName();
        if (exclude(name)) {
            return false;
        }
        return include(name);
    }

    private boolean include(String name) {
        if (basePackages != null) {
            for (String p : basePackages) {
                if (name.startsWith(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean exclude(String name) {
        if (name.startsWith("java")) {
            return true;
        }
        //noinspection RedundantIfStatement
        if (name.startsWith("org.springframework")) {
            return true;
        }
        return false;
    }

    public boolean matches(Method method, Class targetClass) {
        CacheInvokeConfig cac = cacheConfigMap.get(method);
        if (cac == CacheInvokeConfig.getNoCacheInvokeConfigInstance()) {
            return false;
        } else if (cac != null) {
            return true;
        } else {
            cac = new CacheInvokeConfig();
            if (matchesThis(method.getClass())) {
                CacheConfigUtil.parse(cac, method);
            }

            String name = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();
            parseByTargetClass(cac, targetClass, name, paramTypes);

            if (!cac.isEnableCacheContext() && cac.getCacheAnnoConfig() == null) {
                cacheConfigMap.put(method, CacheInvokeConfig.getNoCacheInvokeConfigInstance());
                return false;
            } else {
                cacheConfigMap.put(method, cac);
                return true;
            }
        }
    }

    private void parseByTargetClass(CacheInvokeConfig cac, Class<?> clazz, String name, Class<?>[] paramTypes) {
        boolean matchThis = matchesThis(clazz);
        if (matchThis) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (methodMatch(name, method, paramTypes)) {
                    CacheConfigUtil.parse(cac, method);
                    break;
                }
            }
        }

        if (!clazz.isInterface() && clazz.getSuperclass() != null) {
            parseByTargetClass(cac, clazz.getSuperclass(), name, paramTypes);
        }
        Class<?>[] intfs = clazz.getInterfaces();
        for (Class<?> it : intfs) {
            parseByTargetClass(cac, it, name, paramTypes);
        }
    }

    private boolean methodMatch(String name, Method method, Class<?>[] paramTypes) {
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        if (!name.equals(method.getName())) {
            return false;
        }
        Class<?>[] ps = method.getParameterTypes();
        if (ps.length != paramTypes.length) {
            return false;
        }
        for (int i = 0; i < ps.length; i++) {
            if (!ps[i].equals(paramTypes[i])) {
                return false;
            }
        }
        return true;
    }


    public void setCacheConfigMap(ConcurrentHashMap cacheConfigMap) {
        this.cacheConfigMap = cacheConfigMap;
    }
}
