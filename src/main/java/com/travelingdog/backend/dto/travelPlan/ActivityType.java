package com.travelingdog.backend.dto.travelPlan;

public enum ActivityType {
    MOVE(0),
    LOCATION(1);

    private final int value;

    ActivityType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ActivityType fromValue(int value) {
        return ActivityType.values()[value];
    }
}
