package one.pkg.kreno_fpatcher;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static one.pkg.kreno_fpatcher.PKShared.TARGET_JARS;

public class JarHijacker {
    public static URL[] hijackClasspathUrls(URL[] originalUrls) {
        if (originalUrls == null || originalUrls.length == 0) {
            return originalUrls;
        }

        boolean modified = false;
        URL[] newUrls = new URL[originalUrls.length];

        for (int i = 0; i < originalUrls.length; i++) {
            URL url = originalUrls[i];
            String urlPath = url.getPath();
            boolean hijacked = false;

            for (String targetJar : TARGET_JARS) {
                if (urlPath.endsWith(targetJar)) {
                    URL hijackedUrl = getBuiltinJarUrl();
                    if (hijackedUrl != null) {
                        newUrls[i] = hijackedUrl;
                        System.out.println("[KRENO FPATCHER] Hijacked: " + urlPath + " -> " + hijackedUrl);
                        modified = true;
                        hijacked = true;
                        break;
                    }
                }
            }

            if (!hijacked) {
                newUrls[i] = url;
            }
        }

        return modified ? newUrls : originalUrls;
    }

    private static volatile URL cachedBuiltinJarUrl = null;

    public static Path getBuiltinJarPath() {
        for (String jarName : TARGET_JARS) {
            try {
                URL resource = JarHijacker.class.getResource("/META-INF/jars/" + jarName);
                if (resource != null) {
                    System.out.println("[KRENO FPATCHER] Found builtin jar resource: " + resource);

                    Path tempFile = extractToTemp(resource, jarName);
                    if (tempFile != null) {
                        return tempFile;
                    }
                }
            } catch (Exception e) {
                System.err.println("[KRENO FPATCHER] Failed to load builtin jar " + jarName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.err.println("[KRENO FPATCHER] Builtin jar not found in resources.");
        return null;
    }

    public static URL getBuiltinJarUrl() {
        if (cachedBuiltinJarUrl == null) {
            synchronized (JarHijacker.class) {
                if (cachedBuiltinJarUrl == null) {
                    try {
                        Path jarPath = getBuiltinJarPath();
                        if (jarPath != null) {
                            cachedBuiltinJarUrl = jarPath.toUri().toURL();
                        }
                    } catch (Exception e) {
                        System.err.println("[KRENO FPATCHER] Failed to load builtin jar: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        return cachedBuiltinJarUrl;
    }

    private static Path extractToTemp(URL resource, String jarName) {
        try {
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "KRENO FPATCHER");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
            }

            Path tempFile = tempDir.resolve(jarName);

            if (Files.exists(tempFile)) {
                System.out.println("[KRENO FPATCHER] Using cached jar: " + tempFile);
                return tempFile;
            }

            System.out.println("[KRENO FPATCHER] Extracting jar to: " + tempFile);
            try (var in = resource.openStream();
                 var out = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            tempFile.toFile().deleteOnExit();

            System.out.println("[KRENO FPATCHER] Successfully extracted builtin jar to: " + tempFile);
            return tempFile;

        } catch (Exception e) {
            System.err.println("[KRENO FPATCHER] Failed to extract jar to temp: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}