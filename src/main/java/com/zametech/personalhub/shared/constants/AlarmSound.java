package com.zametech.personalhub.shared.constants;

public enum AlarmSound {
    DEFAULT("default"),
    BELL("bell"),
    CHIME("chime"),
    DING("ding"),
    GENTLE("gentle");

    private final String value;

    AlarmSound(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AlarmSound fromValue(String value) {
        for (AlarmSound sound : AlarmSound.values()) {
            if (sound.value.equalsIgnoreCase(value)) {
                return sound;
            }
        }
        return DEFAULT;
    }
}