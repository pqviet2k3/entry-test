package com.wiinvent.entrytest.enumeration;

public enum CheckinPoints {
    DAY_1(1),
    DAY_2(2),
    DAY_3(3),
    DAY_4(5),
    DAY_5(8),
    DAY_6(13),
    DAY_7(21);

    private final int points;

    CheckinPoints(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }

    public static int getPointsForDay(int day) {
        if (day < 1 || day > 7) {
            return 0;
        }
        return values()[day - 1].getPoints();
    }
} 