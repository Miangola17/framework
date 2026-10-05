
package mg.framework;

import com.google.gson.Gson;
import jakarta.servlet.RequestDispatcher;
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
import mg.framework.annotation.RestAPI;
import mg.framework.annotation.UrlMapping;
import mg.framework.utils.Binder;
import mg.framework.utils.ClassScanner;
import mg.framework.utils.UrlMappingInfo;

public class FrontController extends HttpServlet {

    private Map<String, UrlMappingInfo> urlMap;
    private String prefixe;
    private String suffixe;

    @Override
    public void init() throws ServletException {
        super.init();

        this.prefixe = getInitParameter("prefixe");
        this.suffixe = getInitParameter("suffixe");

        if (this.prefixe == null) {
            this.prefixe = "/WEB-INF/views/";
        }
        if (this.suffixe == null) {
            this.suffixe = ".jsp";
        }

        @SuppressWarnings("unchecked")
        Map<String, UrlMappingInfo> loadedMap = (Map<String, UrlMappingInfo>) this.getServletContext().getAttribute("urlMap");

        if (loadedMap == null) {
            String pkg = getInitParameter("controllersPackage");
            if (pkg == null || pkg.trim().isEmpty()) {
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

        UrlMappingInfo mapping = urlMap.get(currentKey);

        if (mapping == null) {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("FRAMEWORK");
            out.println("Requete recue : " + uri);
            out.println("Methode       : " + httpMethod);
            out.println("Route         : " + path);
            out.println("Cle recherche : " + currentKey);
            out.println("");
            out.println("Aucune methode ne correspond a l'URL : " + path + ", methode " + httpMethod);
            out.println("");
            out.println("URLs supportees :");
            for (Map.Entry<String, UrlMappingInfo> entry : urlMap.entrySet()) {
                String key = entry.getKey();
                UrlMappingInfo info = entry.getValue();
                out.println(" - " + key + " -> " + info.getControllerClass().getSimpleName() + "." + info.getMethod().getName() + "()");
            }
            out.flush();
        } else {
            try {
                Class<?> clazz = mapping.getControllerClass();
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                Method methodToInvoke = mapping.getMethod();
                
                // Data Binding : remplir les paramètres de la méthode
                Class<?>[] paramTypes = methodToInvoke.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                
                for (int i = 0; i < paramTypes.length; i++) {
                    args[i] = Binder.bind(req, paramTypes[i]);
                }
                
                RestAPI restApiAnnotation = methodToInvoke.getAnnotation(RestAPI.class);
                
                if (restApiAnnotation != null) {
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    PrintWriter out = resp.getWriter();
                    
                    Object result = methodToInvoke.invoke(controllerInstance, args);
                    
                    if (result instanceof String) {
                        out.print((String) result);
                    } else {
                        Gson gson = new Gson();
                        String json = gson.toJson(result);
                        out.print(json);
                    }
                    out.flush();
                } else {
                    Object result = methodToInvoke.invoke(controllerInstance, args);

                    if (result instanceof ModelAndView) {
                        ModelAndView mv = (ModelAndView) result;
                        String viewUrl = mv.getUrl();
                        String fullPath = prefixe + viewUrl + suffixe;

                        for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                            req.setAttribute(entry.getKey(), entry.getValue());
                        }

                        RequestDispatcher dispatcher = req.getRequestDispatcher(fullPath);
                        dispatcher.forward(req, resp);
                    } else {
                        resp.setContentType("text/plain");
                        resp.setCharacterEncoding("UTF-8");
                        PrintWriter out = resp.getWriter();
                        out.println("FRAMEWORK");
                        out.println("Requete recue : " + uri);
                        out.println("Methode       : " + httpMethod);
                        out.println("Route         : " + path);
                        out.println("Cle recherche : " + currentKey);
                        out.println("");
                        out.println("URL connue : " + mapping.getUrl());
                        out.println("Classe      : " + mapping.getControllerClass().getName());
                        out.println("Methode     : " + mapping.getMethod().getName());
                        out.println("");
                        out.println("=== Invocation de la methode ===");
                        out.println("Methode executee avec succes !");
                        out.flush();
                    }
                }
            } catch (NoSuchMethodException e) {
                resp.setContentType("text/plain");
                resp.setCharacterEncoding("UTF-8");
                PrintWriter out = resp.getWriter();
                out.println("Erreur : Methode introuvable - " + e.getMessage());
                e.printStackTrace(out);
            } catch (Exception e) {
                resp.setContentType("text/plain");
                resp.setCharacterEncoding("UTF-8");
                PrintWriter out = resp.getWriter();
                out.println("Erreur lors de l'invocation : " + e.getMessage());
                e.printStackTrace(out);
            }
        }
    }
}