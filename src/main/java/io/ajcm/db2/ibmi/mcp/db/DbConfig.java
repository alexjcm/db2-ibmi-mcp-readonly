package io.ajcm.db2.ibmi.mcp.db;

import java.util.Locale;

import com.ibm.as400.access.AS400JDBCDataSource;
import io.ajcm.db2.ibmi.mcp.util.Env;
import io.ajcm.db2.ibmi.mcp.util.KeySet;

/**
 * Centralized DB configuration.
 */
public record DbConfig(
        String host,
        String user,
        String password,
        String schema,
        boolean ssl,
        int port
) {
    /**
     * Creates a DbConfig instance from global environment variables.
     * 
     * @return new DbConfig instance
     */
    public static DbConfig fromEnv() {
        return fromEnvWithPrefix("DB2_CONN_A_", false);
    }

    /**
     * Converts the configuration to an AS400JDBCDataSource.
     * 
     * @return configured AS400JDBCDataSource
     */
    public AS400JDBCDataSource toDataSource() {
        AS400JDBCDataSource ds = new AS400JDBCDataSource();
        ds.setServerName(host);
        ds.setUser(user);
        ds.setPassword(password.toCharArray());
        ds.setLibraries(schema.toUpperCase(Locale.ROOT));
        ds.setSecure(ssl);
        if ((!ssl && port != 446) || (ssl && port != 448)) {
            if (port > 0) {
                ds.setPortNumber(port);
            }
        }
        return ds;
    }

    /**
     * Generates standard JDBC connection URL.
     * 
     * @return jdbc URL string
     */
    public String jdbcUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:as400://").append(host);
        if ((!ssl && port != 446) || (ssl && port != 448)) {
            if (port > 0) {
                sb.append(";portNumber=").append(port);
            }
        }
        if (ssl) {
            sb.append(";secure=true");
        }
        sb.append(";libraries=").append(schema.toUpperCase(Locale.ROOT));
        return sb.toString();
    }

    /**
     * Creates a DbConfig instance using a specific environment variable prefix.
     * 
     * @param prefix environment variable prefix
     * @param fallbackToGlobal whether to fallback to global variables if prefix is not found
     * @return new DbConfig instance
     */
    public static DbConfig fromEnvWithPrefix(String prefix, boolean fallbackToGlobal) {
        String host = getEnv(prefix, "IBM_I_HOST", fallbackToGlobal);
        String user = getEnv(prefix, "IBM_I_USER", fallbackToGlobal);
        String password = getEnv(prefix, "IBM_I_PASSWORD", fallbackToGlobal);
        String schema = getEnv(prefix, "IBM_I_SCHEMA", fallbackToGlobal);
        boolean ssl = Env.getBool(KeySet.DB2_SSL, false);
        int defaultPort = ssl ? 448 : 446;
        int port = Env.getInt(KeySet.DB2_PORT, defaultPort);
        return new DbConfig(host, user, password, schema, ssl, port);
    }

    private static String getEnv(String prefix, String key, boolean fallbackToGlobal) {
        String v = System.getenv(prefix + key);
        if ((v == null || v.isBlank()) && fallbackToGlobal) {
            v = System.getenv(key);
        }
        return v == null ? "" : v.trim();
    }
}
