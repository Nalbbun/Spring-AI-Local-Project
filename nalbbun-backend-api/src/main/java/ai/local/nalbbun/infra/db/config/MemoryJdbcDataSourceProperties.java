package ai.local.nalbbun.infra.db.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JDBC 메모리 저장소 전용 DB 연결 속성이다.
 * 미설정 시 app.api.datasource -> spring.datasource 순으로 fallback 한다.
 */
@ConfigurationProperties(prefix = "app.memory.jdbc.datasource")
public class MemoryJdbcDataSourceProperties {

    private String url;
    private String username;
    private String password;
    private String driverClassName;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
}
