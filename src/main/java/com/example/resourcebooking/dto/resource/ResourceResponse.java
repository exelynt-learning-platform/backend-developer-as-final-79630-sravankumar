package com.example.resourcebooking.dto.resource;

import com.example.resourcebooking.enums.ResourceType;

import java.math.BigDecimal;
import java.time.Instant;

public class ResourceResponse {

    private Long id;
    private String name;
    private String description;
    private ResourceType resourceType;
    private String location;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private boolean available;
    private Instant createdAt;

    public ResourceResponse() {
    }

    public ResourceResponse(Long id, String name, String description, ResourceType resourceType, String location, Integer capacity, BigDecimal pricePerHour, boolean available, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.resourceType = resourceType;
        this.location = location;
        this.capacity = capacity;
        this.pricePerHour = pricePerHour;
        this.available = available;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(BigDecimal pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
