
package mg.framework;

import com.google.gson.Gson;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import mg.framework.annotation.Authenticated;
import mg.framework.annotation.RestAPI;
import mg.framework.annotation.UrlMapping;
import mg.framework.utils.Binder;
import mg.framework.utils.ClassScanner;
import mg.framework.utils.UrlMappingInfo;

@MultipartConfig
public class FrontController extends HttpServlet {

    private Map<String, UrlMappingInfo> urlMap;
    private String prefixe;
    private String suffixe;
    private String authSessionKey;
    private String roleSessionKey;

    @Override
    public void init() throws ServletException {
        super.init();

        this.prefixe = getInitParameter("prefixe");
        this.suffixe = getInitParameter("suffixe");
        this.authSessionKey = getInitParameter("auth-session-key");
        this.roleSessionKey = getInitParameter("role-session-key");

        if (this.prefixe == null) {
            this.prefixe = "/WEB-INF/views/";
        }
        if (this.suffixe == null) {
            this.suffixe = ".jsp";
        }
        if (this.authSessionKey == null) {
            this.authSessionKey = "user";
        }
        if (this.roleSessionKey == null) {
            this.roleSessionKey = "role";
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
                Method methodToInvoke = mapping.getMethod();
                
                // Vérifier l'authentification si la méthode porte l'annotation @Authenticated
                Authenticated authenticatedAnnotation = methodToInvoke.getAnnotation(Authenticated.class);
                if (authenticatedAnnotation != null) {
                    HttpSession session = req.getSession(false);
                    if (session == null || session.getAttribute(authSessionKey) == null) {
                        resp.sendError(401, "Authentification requise");
                        return;
                    }
                    
                    // Vérifier les rôles si l'annotation en a
                    String[] roles = authenticatedAnnotation.roles();
                    if (roles != null && roles.length > 0) {
                        Object roleObj = session.getAttribute(roleSessionKey);
                        if (roleObj == null) {
                            resp.sendError(403, "Role requis");
                            return;
                        }
                        
                        String userRole = roleObj.toString();
                        boolean roleAuthorized = false;
                        for (String role : roles) {
                            if (role.equals(userRole)) {
                                roleAuthorized = true;
                                break;
                            }
                        }
                        
                        if (!roleAuthorized) {
                            resp.sendError(403, "Role non autorise");
                            return;
                        }
                    }
                }
                
                @SuppressWarnings("unchecked")
                ConcurrentMap<Class<?>, Object> controllerInstances = 
                    (ConcurrentMap<Class<?>, Object>) getServletContext().getAttribute("controllerInstances");
                
                Object controllerInstance;
                if (controllerInstances != null) {
                    controllerInstance = controllerInstances.computeIfAbsent(clazz, c -> {
                        try {
                            return c.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException("Error creating controller instance", e);
                        }
                    });
                } else {
                    controllerInstance = clazz.getDeclaredConstructor().newInstance();
                }
                
                // Data Binding : remplir les paramètres de la méthode
                Object[] args = Binder.bindParams(req, methodToInvoke);
                
                RestAPI restApiAnnotation = methodToInvoke.getAnnotation(RestAPI.class);
                
                Object result;
                MySession mySession = null;
                
                // Trouver MySession dans les arguments
                for (Object arg : args) {
                    if (arg instanceof MySession) {
                        mySession = (MySession) arg;
                        break;
                    }
                }
                
                if (restApiAnnotation != null) {
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    PrintWriter out = resp.getWriter();
                    
                    result = methodToInvoke.invoke(controllerInstance, args);
                    
                    // Mettre à jour la session HTTP après l'invocation
                    if (mySession != null) {
                        updateHttpSession(req, mySession);
                    }
                    
                    if (result instanceof String) {
                        out.print((String) result);
                    } else {
                        Gson gson = new Gson();
                        String json = gson.toJson(result);
                        out.print(json);
                    }
                    out.flush();
                } else {
                    result = methodToInvoke.invoke(controllerInstance, args);
                    
                    // Mettre à jour la session HTTP après l'invocation
                    if (mySession != null) {
                        updateHttpSession(req, mySession);
                    }

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
    
    private void updateHttpSession(HttpServletRequest request, MySession mySession) {
        HttpSession httpSession = request.getSession(true);
        Set<String> initialKeys = mySession.getInitialKeys();
        Set<String> currentKeys = mySession.getNames();
        
        // Ajouter ou mettre à jour les variables
        for (String key : currentKeys) {
            Object value = mySession.get(key);
            httpSession.setAttribute(key, value);
        }
        
        // Supprimer les variables qui ont été retirées de MySession
        if (initialKeys != null) {
            for (String key : initialKeys) {
                if (!currentKeys.contains(key)) {
                    httpSession.removeAttribute(key);
                }
            }
        }
    }
}