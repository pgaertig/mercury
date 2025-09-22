package pl.amitec.mercury.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;
import pl.amitec.mercury.MercuryException;
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
        SQLiteDataSource dataSource = new SQLiteDataSource();
        //ds.setLogWriter();
        dataSource.setUrl("jdbc:sqlite:" + file);
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        if (!tableExists()) {
            createTable();
        }
    }

    @Override
    public <E> boolean hit(String tenant, String source, String resource, String key, E data, Consumer<E> dataConsumer) {
        String strData = data.toString();
        String hash = Utils.sha1HexDigest(strData != null ? strData : "");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        List<Map<String, Object>> result = jdbcTemplate.queryForList(
                "SELECT * FROM items WHERE tenant = ? AND source = ? AND resource = ? AND key = ?",
                tenant, source, resource, key);

        if (result != null && !result.isEmpty()) {
            var rec = result.getFirst();
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
                        hash, strData.length(), now, tenant, source, resource, key);
                return false;
            }
        } else {
            dataConsumer.accept(data);
            jdbcTemplate.update(
                    "INSERT INTO items (tenant, source, resource, key, created_at, updated_at, data_hash, data_size)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    tenant, source, resource, key, now, now, hash, strData.length());
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
            throw new MercuryException("DB closing problem", e);
        }
    }

    @Override
    public void drop() {
        close();
        logger.warn("Deleting {}", file);
        Paths.get(file).toFile().delete();
    }
}