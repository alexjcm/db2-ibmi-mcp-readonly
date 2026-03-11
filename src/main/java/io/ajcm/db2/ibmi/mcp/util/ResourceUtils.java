package io.ajcm.db2.ibmi.mcp.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility for reading classpath resources.
 */
public final class ResourceUtils {
    private ResourceUtils() {}

    /**
     * Reads a classpath resource as a UTF-8 string.
     * 
     * @param path the resource path
     * @return the resource content as string
     */
    public static String readClasspathResourceAsString(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ResourceUtils.class.getClassLoader();
        }
        try (InputStream is = cl.getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }
}
