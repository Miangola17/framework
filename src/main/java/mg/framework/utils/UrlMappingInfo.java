package mg.framework.utils;

import java.lang.reflect.Method;

public class UrlMappingInfo {
    private final String url;
    private final Method method;
    private final Class<?> controllerClass;

    public UrlMappingInfo(String url, Method method, Class<?> controllerClass) {
        this.url = url;
        this.method = method;
        this.controllerClass = controllerClass;
    }

    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }
}
