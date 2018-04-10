package sg.edu.ntu.hospitalbeesqdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.client.RestTemplate;

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

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
