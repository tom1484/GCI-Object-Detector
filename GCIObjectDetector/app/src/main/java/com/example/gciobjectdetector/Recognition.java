package com.example.gciobjectdetector;

import android.graphics.RectF;

class Recognition {

    private int id;
    private String title;
    private float confidence;
    private RectF location;

    public Recognition(int id, String title, Float confidence, RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
    }

    public int getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public float getConfidence() {
        return this.confidence;
    }

    public RectF getLocation() {
        return this.location;
    }

}
