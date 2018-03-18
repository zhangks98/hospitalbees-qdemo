package sg.edu.ntu.hospitalbeesqdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import java.time.Clock;

@SpringBootApplication
@PropertySource("queue.properties")
public class ClinicQDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicQDemoApplication.class, args);
    }

    @Bean
    public Clock getClock() {
        return Clock.systemUTC();
    }
}
