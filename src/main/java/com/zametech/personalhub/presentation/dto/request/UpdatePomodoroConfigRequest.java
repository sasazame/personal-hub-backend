package com.zametech.personalhub.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating Pomodoro configuration.
 */
public class UpdatePomodoroConfigRequest {
    @Min(value = 1, message = "Work duration must be at least 1 minute")
    @Max(value = 120, message = "Work duration cannot exceed 120 minutes")
    private Integer workDuration;

    @Min(value = 1, message = "Short break duration must be at least 1 minute")
    @Max(value = 60, message = "Short break duration cannot exceed 60 minutes")
    private Integer shortBreakDuration;

    @Min(value = 1, message = "Long break duration must be at least 1 minute")
    @Max(value = 60, message = "Long break duration cannot exceed 60 minutes")
    private Integer longBreakDuration;

    @Min(value = 1, message = "Cycles before long break must be at least 1")
    @Max(value = 10, message = "Cycles before long break cannot exceed 10")
    private Integer cyclesBeforeLongBreak;

    private String alarmSound;

    @Min(value = 0, message = "Alarm volume must be at least 0")
    @Max(value = 100, message = "Alarm volume cannot exceed 100")
    private Integer alarmVolume;

    private Boolean autoStartBreaks;
    private Boolean autoStartWork;

    // Getters and Setters
    public Integer getWorkDuration() {
        return workDuration;
    }

    public void setWorkDuration(Integer workDuration) {
        this.workDuration = workDuration;
    }

    public Integer getShortBreakDuration() {
        return shortBreakDuration;
    }

    public void setShortBreakDuration(Integer shortBreakDuration) {
        this.shortBreakDuration = shortBreakDuration;
    }

    public Integer getLongBreakDuration() {
        return longBreakDuration;
    }

    public void setLongBreakDuration(Integer longBreakDuration) {
        this.longBreakDuration = longBreakDuration;
    }

    public Integer getCyclesBeforeLongBreak() {
        return cyclesBeforeLongBreak;
    }

    public void setCyclesBeforeLongBreak(Integer cyclesBeforeLongBreak) {
        this.cyclesBeforeLongBreak = cyclesBeforeLongBreak;
    }

    public String getAlarmSound() {
        return alarmSound;
    }

    public void setAlarmSound(String alarmSound) {
        this.alarmSound = alarmSound;
    }

    public Integer getAlarmVolume() {
        return alarmVolume;
    }

    public void setAlarmVolume(Integer alarmVolume) {
        this.alarmVolume = alarmVolume;
    }

    public Boolean getAutoStartBreaks() {
        return autoStartBreaks;
    }

    public void setAutoStartBreaks(Boolean autoStartBreaks) {
        this.autoStartBreaks = autoStartBreaks;
    }

    public Boolean getAutoStartWork() {
        return autoStartWork;
    }

    public void setAutoStartWork(Boolean autoStartWork) {
        this.autoStartWork = autoStartWork;
    }
}