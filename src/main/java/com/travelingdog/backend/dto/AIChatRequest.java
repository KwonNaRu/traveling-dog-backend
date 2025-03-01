package com.travelingdog.backend.dto;

import java.util.List;

public class AIChatRequest {
    private String model;
    private List<AIChatMessage> messages;
    private double temperature;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<AIChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AIChatMessage> messages) {
        this.messages = messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
}