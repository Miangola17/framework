package mg.framework.utils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import mg.framework.annotation.UrlMapping;

public class Util {
    
    public static Map<String, UrlMappingInfo> getMap(List<Class<?>> classes, Map<String, UrlMappingInfo> map) {
        for (Class<?> cls : classes) {
            Method[] methods = cls.getDeclaredMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(UrlMapping.class)) {
                    continue;
                }
                
                UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                String url = annotation.value();
                String httpMethod = annotation.method().toUpperCase();
                String key = httpMethod + "|" + url;
                
                if (map.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate route detected: " + key);
                }
                
                map.put(key, new UrlMappingInfo(url, method, cls));
            }
        }
        
        return map;
    }
}
