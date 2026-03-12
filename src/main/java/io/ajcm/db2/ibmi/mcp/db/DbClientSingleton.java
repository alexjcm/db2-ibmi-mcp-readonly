package io.ajcm.db2.ibmi.mcp.db;

import io.ajcm.db2.ibmi.mcp.util.KeySet;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process-wide singleton provider for DbClient instances.
 */
public final class DbClientSingleton {
    private DbClientSingleton() {}
    private static final Logger log = LoggerFactory.getLogger(DbClientSingleton.class);

    private static class Holder {
        static final Multi INSTANCE = new Multi();
    }

    /**
     * Gets the default database client.
     * 
     * @return DbClient instance
     */
    public static DbClient get() {
        return Holder.INSTANCE.getDefault();
    }

    /**
     * Gets a database client by connection ID.
     * 
     * @param connectionId the connection identifier
     * @return DbClient instance
     */
    public static DbClient get(String connectionId) {
        return Holder.INSTANCE.get(connectionId);
    }

    /**
     * Resolves the actual connection ID to use.
     * 
     * @param requestedId the requested connection identifier
     * @return resolved connection ID
     */
    public static String resolveConnectionId(String requestedId) {
        return Holder.INSTANCE.resolve(requestedId);
    }

    /**
     * Checks if a connection ID exists.
     * 
     * @param connectionId the connection identifier
     * @return true if it exists, false otherwise
     */
    public static boolean has(String connectionId) {
        return Holder.INSTANCE.has(connectionId);
    }

    private static class Multi {
        private final java.util.Map<String, DbClient> byId;
        private final String defaultId;

        Multi() {
            String idsRaw = System.getenv(KeySet.DB2_CONN_IDS);
            if (idsRaw == null || idsRaw.isBlank()) {
                throw new IllegalStateException("DB2_CONN_IDS is required. Define one or more IDs (e.g., ECUADOR,PANAMA) and corresponding DB2_CONN_<ID>_* variables.");
            }
            java.util.Map<String, DbClient> tmp = new java.util.LinkedHashMap<>();
            String[] ids = idsRaw.split(",");
            List<String> invalid = new ArrayList<>();
            for (String idRaw : ids) {
                String id = idRaw.trim();
                if (id.isEmpty()) continue;
                String prefix = "DB2_CONN_" + id + "_";
                DbConfig cfg = DbConfig.fromEnvWithPrefix(prefix, false);
                if (isValid(cfg)) {
                    tmp.put(id, new DbClient(cfg));
                } else {
                    invalid.add(id);
                }
            }
            if (tmp.isEmpty()) {
                throw new IllegalStateException("No valid DB2 connection profiles found. Invalid IDs: " + String.join(",", invalid));
            }
            String envDef = System.getenv(KeySet.DB2_CONN_DEFAULT_ID);
            if (envDef == null || envDef.isBlank()) {
                envDef = System.getenv(KeySet.DB2_DEFAULT_CONN_ID);
            }
            String def;
            if (envDef != null && !envDef.isBlank() && tmp.containsKey(envDef.trim())) {
                def = envDef.trim();
            } else {
                def = tmp.keySet().iterator().next();
            }
            if (!invalid.isEmpty()) {
                log.warn("Some DB profiles were invalid and ignored: {}", String.join(",", invalid));
            }
            log.info("Loaded DB profiles: {} , default={}", tmp.keySet(), def);
            this.byId = java.util.Collections.unmodifiableMap(tmp);
            this.defaultId = def;
        }

        DbClient getDefault() {
            return byId.get(defaultId);
        }

        DbClient get(String id) {
            if (id == null || id.isBlank()) return getDefault();
            DbClient c = byId.get(id);
            return c != null ? c : getDefault();
        }

        String resolve(String id) {
            if (id == null || id.isBlank()) return defaultId;
            return byId.containsKey(id) ? id : defaultId;
        }

        boolean has(String id) {
            return id != null && byId.containsKey(id);
        }

        private static boolean isValid(DbConfig cfg) {
            return cfg != null
                    && cfg.host() != null && !cfg.host().isBlank()
                    && cfg.user() != null && !cfg.user().isBlank()
                    && cfg.password() != null && !cfg.password().isBlank()
                    && cfg.schema() != null && !cfg.schema().isBlank();
        }
    }
}
