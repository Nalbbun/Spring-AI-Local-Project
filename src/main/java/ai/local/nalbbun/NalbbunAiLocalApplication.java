package ai.local.nalbbun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class NalbbunAiLocalApplication {

    public static void main(String[] args) {
        SpringApplication.run(NalbbunAiLocalApplication.class, args);
    }
}
