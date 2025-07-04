package com.zametech.personalhub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Configuration for repeatable todos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepeatConfig {
    private RepeatType repeatType;
    private Integer interval;           // e.g., every 2 weeks
    private List<Integer> daysOfWeek;   // for weekly: 1=Monday, 7=Sunday
    private Integer dayOfMonth;         // for monthly: 1-31
    private LocalDate endDate;          // null means no end date
    
    public static RepeatConfig daily(Integer interval, LocalDate endDate) {
        return new RepeatConfig(RepeatType.DAILY, interval, null, null, endDate);
    }
    
    public static RepeatConfig weekly(List<Integer> daysOfWeek, LocalDate endDate) {
        return new RepeatConfig(RepeatType.WEEKLY, 1, daysOfWeek, null, endDate);
    }
    
    public static RepeatConfig monthly(Integer dayOfMonth, LocalDate endDate) {
        return new RepeatConfig(RepeatType.MONTHLY, 1, null, dayOfMonth, endDate);
    }
    
    public static RepeatConfig yearly(LocalDate endDate) {
        return new RepeatConfig(RepeatType.YEARLY, 1, null, null, endDate);
    }
    
    public static RepeatConfig once() {
        return new RepeatConfig(RepeatType.ONCE, null, null, null, null);
    }
}