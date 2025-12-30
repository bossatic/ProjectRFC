package org.dataingest.rfc.server.monitoring.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.dataingest.rfc.server.config.IDocCaptureConfig;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Factory for creating H2 DataSource with connection pooling
 */
public class H2DataSourceFactory {

    public static DataSource create(IDocCaptureConfig config) throws Exception {
        String dbPath = config.getH2DatabasePath();
        String jdbcUrl = String.format(
            "jdbc:h2:file:%s;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MySQL",
            dbPath
        );

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(config.getH2PoolSize());
        hikariConfig.setConnectionTimeout(5000);
        hikariConfig.setIdleTimeout(300000);
        hikariConfig.setMaxLifetime(600000);

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);

        // Initialize schema
        initializeSchema(dataSource, config);

        return dataSource;
    }

    private static void initializeSchema(DataSource dataSource, IDocCaptureConfig config) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(
                     H2DataSourceFactory.class.getResourceAsStream("/schema.sql")
                 )
             )) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.trim().isEmpty() || line.trim().startsWith("--")) {
                    continue;
                }
                sql.append(line).append("\n");
                // Execute on semicolon
                if (line.trim().endsWith(";")) {
                    stmt.execute(sql.toString());
                    sql.setLength(0);
                }
            }

            config.log("[Monitoring] H2 database schema initialized");

        } catch (Exception e) {
            config.logError("Failed to initialize H2 schema", e);
            throw e;
        }
    }
}
