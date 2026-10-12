package com.smartuniversity.scheduler.platform.api;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods =false)
@OpenAPIDefinition(info =@Info(title ="Smart University Scheduler API", version ="v1",
        description ="University catalog, complete weekly sections, and private saved schedules"))
@SecurityScheme(name ="bearerAuth", type =SecuritySchemeType.HTTP, scheme ="bearer", bearerFormat ="JWT")
public class OpenApiConfiguration {}
