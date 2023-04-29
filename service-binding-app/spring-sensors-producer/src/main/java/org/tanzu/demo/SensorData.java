package org.tanzu.demo;

import java.util.UUID;

public class SensorData {

    private final UUID id;
    private final double temperature;
    private final double pressure;

    public SensorData(UUID id, double temperature, double pressure) {
        this.id = id;
        this.temperature = temperature;
        this.pressure = pressure;
    }

    public static SensorData generate(UUID id) {
        return new SensorData(id, generateTemperature(), generatePressure());
    }

    private static double generateTemperature() {
        return Math.random() * 100;
    }

    private static double generatePressure() {
        return Math.random() * 100;
    }

    // The getters are required for the JSON serialization
    public UUID getId() {
        return id;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getPressure() {
        return pressure;
    }
}
