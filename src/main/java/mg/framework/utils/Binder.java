package mg.framework.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class Binder {

    public static Object[] bindParams(HttpServletRequest request, Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            String paramValue = request.getParameter(paramName);
            Class<?> paramType = parameters[i].getType();

            args[i] = convertValue(paramValue, paramType);
        }

        return args;
    }

    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        }

        return value;
    }
}
