
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
import mg.framework.utils.UrlMappingInfo;

public class FrontController extends HttpServlet {

    private Map<String, UrlMappingInfo> urlMap;

    @Override
    public void init() throws ServletException {
        super.init();

        @SuppressWarnings("unchecked")
        Map<String, UrlMappingInfo> loadedMap = (Map<String, UrlMappingInfo>) this.getServletContext().getAttribute("urlMap");

        if (loadedMap == null) {
            String pkg = getInitParameter("controllersPackage");
            if (pkg == null || pkg.isBlank()) {
                throw new ServletException("urlMap is not initialized and no controllersPackage init-param is configured");
            }

            ClassScanner scanner = new ClassScanner();
            Set<Class<?>> found = scanner.scanPackageForControllers(pkg);
            
            this.urlMap = new HashMap<>();
            for (Class<?> controllerClass : found) {
                for (Method method : controllerClass.getDeclaredMethods()) {
                    UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                    if (mapping != null) {
                        String url = mapping.value();
                        String httpMethod = mapping.method().toUpperCase();
                        String key = httpMethod + "|" + url;
                        
                        if (this.urlMap.containsKey(key)) {
                            throw new ServletException("Duplicate route detected: " + key);
                        }
                        
                        this.urlMap.put(key, new UrlMappingInfo(url, method, controllerClass));
                    }
                }
            }
            
            this.getServletContext().setAttribute("urlMap", this.urlMap);
            
            System.out.println("=== URLs supportees au demarrage ===");
            for (UrlMappingInfo info : this.urlMap.values()) {
                System.out.println("URL: " + info.getUrl() + 
                                 " -> Methode: " + info.getMethod().getName() + 
                                 " -> Classe: " + info.getControllerClass().getName());
            }
            return;
        }

        this.urlMap = loadedMap;
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
        String httpMethod = req.getMethod().toUpperCase();
        String currentKey = httpMethod + "|" + path;

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("=== FRAMEWORK - Sprint 3 ===");
        out.println("Requete recue : " + uri);
        out.println("Methode       : " + httpMethod);
        out.println("Route         : " + path);
        out.println("Cle recherche : " + currentKey);
        out.println("");

        UrlMappingInfo mapping = urlMap.get(currentKey);
        
        if (mapping == null) {
            out.println("Aucune methode ne correspond a l'URL : " + path + ", methode " + httpMethod);
            out.println("");
            out.println("URLs supportees :");
            for (Map.Entry<String, UrlMappingInfo> entry : urlMap.entrySet()) {
                String key = entry.getKey();
                UrlMappingInfo info = entry.getValue();
                out.println(" - " + key + " -> " + info.getControllerClass().getSimpleName() + "." + info.getMethod().getName() + "()");
            }
        } else {
            out.println("URL connue : " + mapping.getUrl());
            out.println("Classe      : " + mapping.getControllerClass().getName());
            out.println("Methode     : " + mapping.getMethod().getName());
            out.println("");
            out.println("=== Invocation de la methode ===");
            
            try {
                Class<?> clazz = mapping.getControllerClass();
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                Method methodToInvoke = mapping.getMethod();
                methodToInvoke.invoke(controllerInstance);
                out.println("Methode executee avec succes !");
            } catch (NoSuchMethodException e) {
                out.println("Erreur : Methode introuvable - " + e.getMessage());
                e.printStackTrace(out);
            } catch (Exception e) {
                out.println("Erreur lors de l'invocation : " + e.getMessage());
                e.printStackTrace(out);
            }
        }
        
        out.flush();
    }
}