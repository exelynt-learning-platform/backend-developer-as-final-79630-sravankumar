package com.example.resourcebooking.mapper;

import com.example.resourcebooking.dto.resource.ResourceCreateRequest;
import com.example.resourcebooking.dto.resource.ResourceResponse;
import com.example.resourcebooking.dto.resource.ResourceUpdateRequest;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.enums.ResourceType;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public Resource toEntity(ResourceCreateRequest request) {
        if (request == null) {
            return null;
        }
        Resource resource = new Resource();
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setResourceType(request.getResourceType() != null ? request.getResourceType() : ResourceType.ROOM);
        resource.setLocation(request.getLocation());
        resource.setCapacity(request.getCapacity());
        resource.setPricePerHour(request.getPricePerHour());
        resource.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        return resource;
    }

    public void updateEntity(Resource resource, ResourceUpdateRequest request) {
        if (resource == null || request == null) {
            return;
        }
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        if (request.getResourceType() != null) {
            resource.setResourceType(request.getResourceType());
        }
        resource.setLocation(request.getLocation());
        resource.setCapacity(request.getCapacity());
        resource.setPricePerHour(request.getPricePerHour());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
    }

    public ResourceResponse toResponse(Resource resource) {
        if (resource == null) {
            return null;
        }
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getResourceType(),
                resource.getLocation(),
                resource.getCapacity(),
                resource.getPricePerHour(),
                resource.isAvailable(),
                resource.getCreatedAt()
        );
    }
}
