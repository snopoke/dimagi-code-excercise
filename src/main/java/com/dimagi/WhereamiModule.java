package com.dimagi;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.twitter.util.ExecutorServiceFuturePool;
import com.twitter.util.FuturePool;
import fi.evident.dalesbred.Database;
import jodd.mail.Pop3SslServer;
import jodd.mail.ReceiveMailSession;
import jodd.mail.ReceiveMailSessionProvider;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.*;

public class WhereamiModule extends AbstractModule {

    private static final String THREADPOOL_KEEP_ALIVE_TIME = "threadpool.KeepAliveTime";
    private static final String THREADPOOL_MAX_SIZE = "threadpool.maxSize";
    private static final String THREADPOOL_CORE_SIZE = "threadpool.coreSize";

    private static final String MAIL_HOST = "mail.host";
    private static final String MAIL_USERNAME = "mail.username";
    private static final String MAIL_PASSWORD = "mail.password";

    private static final String JDBC_DRIVER = "jdbc.driver";
    private static final String JDBC_URL = "jdbc.url";
    private static final String JDBC_USERNAME = "jdbc.username";
    private static final String JDBC_PASSWORD = "jdbc.password";

    private Properties properties;
    private final File props;


    public WhereamiModule(File props) throws Exception {
        this.props = props;
        this.properties = loadProperties(props);
    }

    @Override
    protected void configure() {
        Names.bindProperties(binder(), properties);

        bind(File.class).annotatedWith(Names.named("config")).toInstance(props);

        bind(WhereamiByMail.class);
    }

    @Provides
    @Singleton
    public ReceiveMailSessionProvider mailSessionProvider(@Named(MAIL_HOST) final String host,
            @Named(MAIL_USERNAME) final String username,
            @Named(MAIL_PASSWORD) final String password) {
        Pop3SslServer popServer = new Pop3SslServer(host, username, password);
        return popServer;
    }


    @Provides
    @Singleton
    public ExecutorService provideExecutor(@Named(THREADPOOL_CORE_SIZE) final int coreSize) {
        return new ScheduledThreadPoolExecutor(coreSize);
    }

    @Provides
    @Singleton
    public DataSource provideDataSource(@Named("jdbc.driver") final String driver,
                                        @Named("jdbc.url") final String url,
                                        @Named("jdbc.username") final String username,
                                        @Named("jdbc.password") final String password) {
        final org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Provides
    @Singleton
    public Database provideDataSource(final DataSource dataSource) {
        return Database.forDataSource(dataSource);
    }

    private Properties loadProperties(File props) throws Exception {
        Properties properties = new Properties();
        FileInputStream fis = new FileInputStream(props);
        properties.load(fis);
        return properties;
    }
}
