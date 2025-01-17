package liuyuyang.net;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFileStorage
public class Main {
    public static void main(String[] args) {
        System.out.println("服务已启动");
        SpringApplication.run(Main.class, args);
    }
}