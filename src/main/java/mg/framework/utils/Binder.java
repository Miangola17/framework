package mg.framework.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Binder {

    public static Object[] bindParams(HttpServletRequest request, Method method) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            Class<?> paramType = parameters[i].getType();

            if (isSimple(paramType)) {
                String paramValue = request.getParameter(paramName);
                args[i] = convertValue(paramValue, paramType);
            } else {
                args[i] = bindObject(request, paramName, paramType);
            }
        }

        return args;
    }

    private static boolean isSimple(Class<?> type) {
        return type == String.class ||
               type == int.class || type == Integer.class ||
               type == long.class || type == Long.class ||
               type == double.class || type == Double.class ||
               type == float.class || type == Float.class ||
               type == boolean.class || type == Boolean.class;
    }

    private static Object bindObject(HttpServletRequest request, String paramName, Class<?> objectType) {
        try {
            Object obj = objectType.getDeclaredConstructor().newInstance();
            Map<String, String[]> parameterMap = request.getParameterMap();
            String prefix = paramName + ".";

            for (String key : parameterMap.keySet()) {
                if (key.startsWith(prefix)) {
                    String path = key.substring(prefix.length());
                    String[] values = parameterMap.get(key);
                    if (values != null && values.length > 0) {
                        setByPath(obj, path, values[0]);
                    }
                }
            }

            return obj;
        } catch (Exception e) {
            throw new RuntimeException("Error binding object: " + e.getMessage(), e);
        }
    }

    private static void setByPath(Object obj, String path, String value) {
        try {
            String[] steps = path.split("\\.");
            Object current = obj;

            for (int i = 0; i < steps.length; i++) {
                String step = steps[i];
                boolean isLast = (i == steps.length - 1);

                if (step.contains("[")) {
                    int bracketIndex = step.indexOf("[");
                    String fieldName = step.substring(0, bracketIndex);
                    int index = Integer.parseInt(step.substring(bracketIndex + 1, step.length() - 1));

                    Field field = getField(current.getClass(), fieldName);
                    field.setAccessible(true);

                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) field.get(current);

                    if (list == null) {
                        list = new ArrayList<>();
                        field.set(current, list);
                    }

                    while (list.size() <= index) {
                        list.add(null);
                    }

                    if (isLast) {
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            ParameterizedType pt = (ParameterizedType) genericType;
                            Type elementType = pt.getActualTypeArguments()[0];
                            Class<?> elementClass = getClass(elementType);
                            Object convertedValue = convertValue(value, elementClass);
                            if (convertedValue != null || !elementClass.isPrimitive()) {
                                list.set(index, convertedValue);
                            }
                        }
                    } else {
                        if (list.get(index) == null) {
                            Type genericType = field.getGenericType();
                            if (genericType instanceof ParameterizedType) {
                                ParameterizedType pt = (ParameterizedType) genericType;
                                Type elementType = pt.getActualTypeArguments()[0];
                                Class<?> elementClass = getClass(elementType);
                                list.set(index, elementClass.getDeclaredConstructor().newInstance());
                            }
                        }
                        current = list.get(index);
                    }
                } else {
                    Field field = getField(current.getClass(), step);
                    field.setAccessible(true);

                    if (isLast) {
                        Object convertedValue = convertValue(value, field.getType());
                        if (convertedValue != null || !field.getType().isPrimitive()) {
                            field.set(current, convertedValue);
                        }
                    } else {
                        Object child = field.get(current);
                        if (child == null) {
                            if (List.class.isAssignableFrom(field.getType())) {
                                child = new ArrayList<>();
                            } else {
                                child = field.getType().getDeclaredConstructor().newInstance();
                            }
                            field.set(current, child);
                        }
                        current = child;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error setting value by path: " + path + ", value: " + value, e);
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getField(superClass, fieldName);
            }
            throw e;
        }
    }

    private static Class<?> getClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return getClass(((ParameterizedType) type).getRawType());
        }
        return Object.class;
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
