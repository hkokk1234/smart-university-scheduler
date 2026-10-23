package com.smartuniversity.scheduler.catalog.infrastructure;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.DayOfWeek;

@Converter
public class WeekdayConverter implements AttributeConverter<DayOfWeek, Integer> {
    @Override
    public Integer convertToDatabaseColumn(DayOfWeek weekday) {
        return weekday ==null ? null : weekday.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer weekday) {
        return weekday ==null ? null : DayOfWeek.of(weekday);
    }
}
