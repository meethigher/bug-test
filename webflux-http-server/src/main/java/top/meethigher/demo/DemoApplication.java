package top.meethigher.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RestController
public class DemoApplication {

    private static final Logger log = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    private final Scheduler myScheduler = Schedulers.newBoundedElastic(
            1,         // 最大线程数
            100_000,     // 队列容量（用于背压等待）
            "custom-elastic"
    );


    @GetMapping("/bug-test")
    public Mono<String> bugTest() throws Exception {
        return Mono.fromCallable(() -> {
            log.info("business logic start");
            TimeUnit.SECONDS.sleep(20);
            log.info("business logic end");
            return String.valueOf(System.currentTimeMillis());
        }).subscribeOn(myScheduler);
    }
}
