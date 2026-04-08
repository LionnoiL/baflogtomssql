package ua.haponov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.Security;

@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) {

        String disabledAlgorithms = Security.getProperty("jdk.tls.disabledAlgorithms");
        if (disabledAlgorithms != null) {
            String updatedAlgorithms = disabledAlgorithms
                    .replace("TLSv1, ", "")
                    .replace("TLSv1.1, ", "")
                    .replace(", TLSv1", "")
                    .replace(", TLSv1.1", "");
            Security.setProperty("jdk.tls.disabledAlgorithms", updatedAlgorithms);
        }

        SpringApplication.run(Main.class, args);
    }
}