package mg.framework.utils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassScanner {

    private static final String CONTROLLER_ANNOTATION = "mg.framework.annotation.Controller";

    public Set<Class<?>> scanPackageForControllers(String pkg) {
        Set<Class<?>> result = new HashSet<>();
        String path = pkg.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();
                try {
                    if ("file".equals(protocol)) {
                        File dir = new File(url.toURI());
                        scanDirectory(pkg, dir, result);
                    } else if ("jar".equals(protocol)) {
                        JarURLConnection jconn = (JarURLConnection) url.openConnection();
                        try (JarFile jar = jconn.getJarFile()) {
                            scanJarForPackage(jar, path, result);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (IOException e) {
           
        }
        return result;
    }

    public Set<Class<?>> scanAllForControllers() {
        Set<Class<?>> result = new HashSet<>();
        String cp = System.getProperty("java.class.path");
        if (cp == null) cp = "";
        String[] entries = cp.split(File.pathSeparator);
        for (String entry : entries) {
            try {
                File f = new File(entry);
                if (f.isDirectory()) {
                    scanDirForAll(f, "", result);
                } else if (f.isFile() && entry.endsWith(".jar")) {
                    try (JarFile jar = new JarFile(f)) {
                        scanJarForAll(jar, result);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return result;
    }

    private void scanDirectory(String pkg, File dir, Set<Class<?>> out) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                scanDirectory(pkg + "." + f.getName(), f, out);
            } else if (f.getName().endsWith(".class")) {
                String className = pkg + "." + f.getName().substring(0, f.getName().length() - 6);
                tryAddClass(className, out);
            }
        }
    }

    private void scanJarForPackage(JarFile jar, String packagePath, Set<Class<?>> out) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = entries.nextElement();
            String name = je.getName();
            if (name.startsWith(packagePath) && name.endsWith(".class")) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                tryAddClass(className, out);
            }
        }
    }

    private void scanDirForAll(File dir, String pkgPrefix, Set<Class<?>> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String next = pkgPrefix.isEmpty() ? f.getName() : pkgPrefix + "." + f.getName();
                scanDirForAll(f, next, out);
            } else if (f.getName().endsWith(".class")) {
                String clazz = (pkgPrefix.isEmpty() ? "" : pkgPrefix + ".") + f.getName().substring(0, f.getName().length() - 6);
                tryAddClass(clazz, out);
            }
        }
    }

    private void scanJarForAll(JarFile jar, Set<Class<?>> out) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry je = entries.nextElement();
            String name = je.getName();
            if (name.endsWith(".class")) {
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                tryAddClass(className, out);
            }
        }
    }

    private void tryAddClass(String className, Set<Class<?>> out) {
        try {
            Class<?> c = Class.forName(className);
            if (hasControllerAnnotation(c)) out.add(c);
        } catch (Throwable ignored) {
        }
    }

    private boolean hasControllerAnnotation(Class<?> c) {
        try {
            for (java.lang.annotation.Annotation a : c.getAnnotations()) {
                if (a.annotationType().getName().equals(CONTROLLER_ANNOTATION)) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
