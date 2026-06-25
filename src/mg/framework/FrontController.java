
package mg.framework;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import mg.framework.annotation.UrlMapping;
import mg.framework.utils.ClassScanner;

public class FrontController extends HttpServlet {

    private final Map<String, UrlMappingInfo> urlMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        ServletConfig config = getServletConfig();
        String pkg = config.getInitParameter("controllersPackage");
        ClassScanner scanner = new ClassScanner();
        Set<Class<?>> found;
        if (pkg != null && !pkg.isEmpty()) {
            found = scanner.scanPackageForControllers(pkg);
        } else {
            found = scanner.scanAllForControllers();
        }
        for (Class<?> controllerClass : found) {
            for (Method method : controllerClass.getDeclaredMethods()) {
                UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                if (mapping != null) {
                    String url = mapping.value();
                    urlMappings.put(url, new UrlMappingInfo(url, method, controllerClass));
                }
            }
        }
        
        System.out.println("=== URLs supportees au demarrage ===");
        for (UrlMappingInfo info : urlMappings.values()) {
            System.out.println("URL: " + info.getUrl() + 
                             " -> Methode: " + info.getMethod().getName() + 
                             " -> Classe: " + info.getControllerClass().getName());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("== FRAMEWORK - Sprint 2 ==");
        out.println("Requete recue : " + uri);
        out.println("Methode       : " + req.getMethod());
        out.println("");

        UrlMappingInfo mapping = urlMappings.get(path);
        
        if (mapping == null) {
            out.println("ERREUR: URL inconnue !");
            out.println("");
            out.println("URLs supportees :");
            for (UrlMappingInfo info : urlMappings.values()) {
                out.println(" - " + info.getUrl() + " -> " + info.getControllerClass().getSimpleName() + "." + info.getMethod().getName() + "()");
            }
        } else {
            out.println("URL connue : " + mapping.getUrl());
            out.println("Classe      : " + mapping.getControllerClass().getName());
            out.println("Methode     : " + mapping.getMethod().getName());
        }
        
        out.flush();
    }
}