package com.hyperq.analyzer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration //说明这是一个配置类
@EnableSwagger2// 该注解开启Swagger2的自动配置
public class SwaggerConfig {  //随便的一个类名

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.hyperq.controller"))
                .paths(PathSelectors.any())
                .build();

    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                //设置文档标题(API名称)
                .title("HyperQ work flow API document.")
                //文档描述
                .description("API document")
                //服务条款URL
                .termsOfServiceUrl("http://127.0.0.1:8000/")
                //联系信息
                .contact("dark")
                //版本号
                .version("1.0.0")
                .build();
    }
}