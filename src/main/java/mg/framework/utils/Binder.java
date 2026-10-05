package mg.framework.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Enumeration;

public class Binder {

    public static Object bind(HttpServletRequest request, Class<?> type) {
        try {
            // Instancier un objet vide du type donné
            Object instance = type.getDeclaredConstructor().newInstance();

            // Récupérer tous les paramètres du formulaire
            Enumeration<String> paramNames = request.getParameterNames();

            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);

                // Chercher le champ correspondant dans l'objet
                try {
                    Field field = type.getDeclaredField(paramName);
                    field.setAccessible(true);

                    // Convertir la valeur selon le type du champ
                    Object convertedValue = convertValue(paramValue, field.getType());
                    field.set(instance, convertedValue);
                } catch (NoSuchFieldException e) {
                    // Le champ n'existe pas dans l'objet, on ignore
                    System.out.println("Champ '" + paramName + "' non trouve dans la classe " + type.getSimpleName());
                }
            }

            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du binding : " + e.getMessage(), e);
        }
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
