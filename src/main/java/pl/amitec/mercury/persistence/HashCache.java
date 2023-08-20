package pl.amitec.mercury.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import pl.amitec.mercury.util.Utils;

import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class HashCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(HashCache.class);

    private JdbcTemplate jdbcTemplate;
    private String file;

    public HashCache(String path) {
        this.file = path;
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + file);
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        if (!tableExists()) {
            createTable();
        }
    }

    @Override
    public boolean hit(String tenant, String source, String resource, String key, String data, Consumer<String> dataConsumer) {
        String hash = Utils.sha1HexDigest(data != null ? data : "");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        List<Map<String, Object>> result = jdbcTemplate.queryForList(
                "SELECT * FROM items WHERE tenant = ? AND source = ? AND resource = ? AND key = ?",
                tenant, source, resource, key);

        if (result != null && !result.isEmpty()) {
            var rec = result.get(0);
            if (hash.equals(rec.get("data_hash"))) {
                logger.debug("Hit {}", rec);
                jdbcTemplate.update(
                        "UPDATE items SET last_hit_at = ? WHERE tenant = ? AND source = ? AND resource = ? AND key = ?",
                        now, tenant, source, resource, key);
                return true;
            } else {
                dataConsumer.accept(data);
                jdbcTemplate.update(
                        "UPDATE items SET data_hash = ?, data_size = ?, updated_at = ? WHERE tenant = ? AND source = ? AND resource = ? AND key = ?",
                        hash, data.length(), now, tenant, source, resource, key);
                return false;
            }
        } else {
            dataConsumer.accept(data);
            jdbcTemplate.update(
                    "INSERT INTO items (tenant, source, resource, key, created_at, updated_at, data_hash, data_size)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    tenant, source, resource, key, now, now, hash, data.length());
            return false;
        }
    }

    private boolean tableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='items'",
                Integer.class);
        return count != null && count > 0;
    }

    private void createTable() {
        jdbcTemplate.execute(
                "CREATE TABLE items (" +
                        "id INTEGER PRIMARY KEY, " +
                        "tenant TEXT, " +
                        "source TEXT, " +
                        "resource TEXT, " +
                        "key TEXT, " +
                        "data_hash TEXT, " +
                        "data_size TEXT, " +
                        "created_at TIMESTAMP, " +
                        "updated_at TIMESTAMP, " +
                        "last_hit_at TIMESTAMP, " +
                        "UNIQUE(tenant, source, resource, key))");
    }

    @Override
    public void close() {
        try {
            jdbcTemplate.getDataSource().getConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void drop() {
        close();
        logger.warn("Deleting {}", file);
        Paths.get(file).toFile().delete();
    }
}