package com.smartuniversity.scheduler.catalog.api;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;
public record CreateCourseRequest(@NotNull UUID semesterId, @NotBlank @Size(max =32) String code,
                                  @NotBlank @Size(max =200) String title,
                                  @NotNull @DecimalMin(value ="0", inclusive =false) @DecimalMax("60")
                                  @Digits(integer =2, fraction =1) BigDecimal credits) {}
