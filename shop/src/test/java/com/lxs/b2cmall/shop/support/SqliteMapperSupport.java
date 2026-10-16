package com.lxs.b2cmall.shop.support;

import com.lxs.b2cmall.shop.mapper.EmployeeMapper;
import com.lxs.b2cmall.shop.mapper.LoginLogMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.logging.Logger;

/**
 * 把 MyBatis 的注解式 Mapper 挂到一个内存 SQLite 上，供「真库断言」类任务项使用。
 *
 * 对应 tdd-test.md 中的：
 * 1.4.x（Mapper 映射与约束）、1.5.x / 2.2.x / 8.1.x（事件 → 数据库终态）。
 *
 * 为什么不启动 Spring：这些测试只需要 Mapper + 事件监听器，
 * 启动完整上下文会连带拉起 Nacos / Redis，慢且脆。
 *
 * 关键实现点：内存 SQLite 的生命周期跟随 Connection，
 * 所以这里用一个「close() 是空操作」的连接代理包住它，
 * 否则 MyBatis 每次关闭 SqlSession 都会把库连根拔掉。
 */
public final class SqliteMapperSupport {

    private SqliteMapperSupport() {
    }

    /**
     * 基于给定连接构建 SqlSessionFactory。
     * 调用方请用 factory.openSession(true)（自动提交）获取会话，
     * 否则 MyBatis 会把连接切到手动提交模式，插入结果读不到。
     */
    public static SqlSessionFactory factory(Connection keepAliveConnection) {
        DataSource dataSource = new SingleConnectionDataSource(keepAliveConnection);
        Environment environment = new Environment("sqlite-test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(EmployeeMapper.class);
        configuration.addMapper(LoginLogMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    /** 只返回同一个连接、且 close() 不真正关闭的数据源。 */
    private static final class SingleConnectionDataSource implements DataSource {

        private final Connection connection;

        SingleConnectionDataSource(Connection real) {
            this.connection = keepAlive(real);
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            // 测试环境无需日志 writer
        }

        @Override
        public void setLoginTimeout(int seconds) {
            // 测试环境无需超时设置
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger("sqlite-test");
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }

    /** 包一层动态代理，拦截 close() 使内存库存活到测试结束。 */
    private static Connection keepAlive(final Connection real) {
        return (Connection) Proxy.newProxyInstance(
                SqliteMapperSupport.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException ex) {
                        throw ex.getTargetException();
                    }
                });
    }
}
