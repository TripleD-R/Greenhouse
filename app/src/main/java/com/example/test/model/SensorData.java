package com.example.test.model;

public class SensorData {
    private final float temperature;
    private final float humidity;
    private final float light;

    public SensorData(float temperature, float humidity, float light) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.light = light;
    }

    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public float getLight() { return light; }
}
