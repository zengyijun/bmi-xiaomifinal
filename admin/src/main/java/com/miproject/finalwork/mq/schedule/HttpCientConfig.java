package com.miproject.finalwork.mq.schedule;

import com.baomidou.mybatisplus.extension.api.R;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author zengyijun
 */
@Configuration
public class HttpCientConfig {
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
