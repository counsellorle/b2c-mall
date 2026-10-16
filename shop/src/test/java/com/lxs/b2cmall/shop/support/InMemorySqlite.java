package com.lxs.b2cmall.shop.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 内存 SQLite 测试基座。
 *
 * 用途：给 Mapper 层与「事件 → 数据库终态」这类必须落库才能验证的任务项
 * （如 .tdd/task/login-tdd-tasks.md 的 3.2.1 缺陷 B1 复现）提供一个
 * 不污染生产 shop.db、无需 Spring 上下文的数据库。
 *
 * 注意：内存库的生命周期跟随 Connection，请勿提前 close。
 */
public final class InMemorySqlite {

    private static final String SCHEMA_RESOURCE = "/schema-test.sql";

    private InMemorySqlite() {
    }

    /** 打开一个已建好测试表结构的内存库。调用方负责 close。 */
    public static Connection open() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys=on");
            }
            for (String sql : schemaStatements()) {
                try (Statement st = conn.createStatement()) {
                    st.execute(sql);
                }
            }
            return conn;
        } catch (SQLException ex) {
            throw new IllegalStateException("初始化内存 SQLite 失败", ex);
        }
    }

    /** 插入一条员工记录，返回自增主键。密码需由调用方传入 BCrypt hash。 */
    public static int insertEmployee(Connection conn, int shopId, String username,
                                     String bcryptHash, int status) throws SQLException {
        String sql = "INSERT INTO tb_employee (shop_id, username, password, avatar_url, "
                + "login_count, status, created_at, updated_at) VALUES (?, ?, ?, ?, 0, ?, "
                + "'2026-10-13 15:00:00', '2026-10-13 15:00:00')";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, shopId);
            ps.setString(2, username);
            ps.setString(3, bcryptHash);
            ps.setString(4, "/avatars/default.png");
            ps.setInt(5, status);
            ps.executeUpdate();
            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public static int countRows(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    /** 把 schema-test.sql 切成一条条可执行语句（去掉注释行与空行）。 */
    private static List<String> schemaStatements() {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = InMemorySqlite.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("classpath 下找不到 " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("读取 " + SCHEMA_RESOURCE + " 失败", ex);
        }

        List<String> statements = new ArrayList<>();
        for (String raw : sb.toString().split(";")) {
            StringBuilder cleaned = new StringBuilder();
            for (String line : raw.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                cleaned.append(line).append('\n');
            }
            String sql = cleaned.toString().trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }
}
