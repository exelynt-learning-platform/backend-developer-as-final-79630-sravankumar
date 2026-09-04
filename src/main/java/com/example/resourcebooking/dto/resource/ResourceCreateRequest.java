package com.example.resourcebooking.dto.resource;

import com.example.resourcebooking.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ResourceCreateRequest {

    @NotBlank(message = "Resource name is required")
    @Size(max = 100, message = "Resource name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private ResourceType resourceType = ResourceType.ROOM;

    @NotBlank(message = "Location is required")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    private Integer capacity;

    @NotNull(message = "Price per hour is required")
    @Positive(message = "Price per hour must be greater than zero")
    private BigDecimal pricePerHour;

    private Boolean available = true;

    public ResourceCreateRequest() {
    }

    public ResourceCreateRequest(String name, String description, ResourceType resourceType, String location, Integer capacity, BigDecimal pricePerHour, Boolean available) {
        this.name = name;
        this.description = description;
        this.resourceType = resourceType != null ? resourceType : ResourceType.ROOM;
        this.location = location;
        this.capacity = capacity;
        this.pricePerHour = pricePerHour;
        this.available = available != null ? available : true;
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
        this.resourceType = resourceType != null ? resourceType : ResourceType.ROOM;
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

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available != null ? available : true;
    }
}
