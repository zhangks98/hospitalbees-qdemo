package sg.edu.ntu.hospitalbeesqdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.time.Clock;

@SpringBootApplication
@PropertySources({@PropertySource("classpath:queue.properties"),
        @PropertySource(value = "file:./queue.properties", ignoreResourceNotFound = true)})
public class ClinicQDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicQDemoApplication.class, args);
    }

    @Bean
    public Clock getClock() {
        return Clock.systemUTC();
    }
}
