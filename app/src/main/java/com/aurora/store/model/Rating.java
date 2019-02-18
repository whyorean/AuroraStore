package com.aurora.store.model;

public class Rating {

    private float average;
    private int[] stars = new int[5];

    public float getAverage() {
        return average;
    }

    public void setAverage(float average) {
        this.average = average;
    }

    public int getStars(int starNum) {
        return stars[starNum - 1];
    }

    public void setStars(int starNum, int ratings) {
        stars[starNum - 1] = ratings;
    }
}
