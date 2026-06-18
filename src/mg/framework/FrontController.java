
package mg.framework;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mg.framework.utils.ClassScanner;

public class FrontController extends HttpServlet {

    private final List<String> controllers = new ArrayList<>();

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
        for (Class<?> c : found) {
            controllers.add(c.getName());
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

        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        out.println("=== FRAMEWORK - Sprint 1 ===");
        out.println("Requete recue : " + uri);
        out.println("Methode       : " + req.getMethod());
        out.println("Le framework fonctionne !");
        out.println("");
        out.println("Controllers trouves (" + controllers.size() + ") :");
        for (String name : controllers) {
            out.println(" - " + name);
        }
        out.flush();
    }
}