package mg.framework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebListener;
import mg.framework.utils.ClassScanner;
import mg.framework.utils.UrlMappingInfo;
import mg.framework.utils.Util;

@WebListener
public class ContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("=== ContextListener demarre ===");
        String packageName = sce.getServletContext().getInitParameter("controllersPackage");
        if (packageName == null || packageName.isBlank()) {
            System.out.println("Package name is null or blank");
            return;
        }

        System.out.println("Scanning package: " + packageName);
        ClassScanner scanner = new ClassScanner();
        Set<Class<?>> controllerClasses = scanner.scanPackageForControllers(packageName);
        List<Class<?>> controllerList = new ArrayList<>(controllerClasses);
        System.out.println("Found " + controllerList.size() + " controller classes");
        
        Map<String, UrlMappingInfo> map = new HashMap<>();
        try {
            Util.getMap(controllerList, map);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            throw new RuntimeException("Duplicate route detected: " + e.getMessage(), e);
        }
        
        sce.getServletContext().setAttribute("urlMap", map);
        
        System.out.println("=== URLs supportees au demarrage (ContextListener) ===");
        for (UrlMappingInfo info : map.values()) {
            System.out.println("URL: " + info.getUrl() + 
                             " -> Methode: " + info.getMethod().getName() + 
                             " -> Classe: " + info.getControllerClass().getName());
        }
    }
}
