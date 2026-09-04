package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.common.PageResponse;
import com.example.resourcebooking.dto.resource.ResourceCreateRequest;
import com.example.resourcebooking.dto.resource.ResourceResponse;
import com.example.resourcebooking.dto.resource.ResourceUpdateRequest;

public interface ResourceService {

    PageResponse<ResourceResponse> getAllResources(int page, int size, String sortBy, String sortDir);

    ResourceResponse getResourceById(Long id);

    ResourceResponse createResource(ResourceCreateRequest request);

    ResourceResponse updateResource(Long id, ResourceUpdateRequest request);

    void deleteResource(Long id);
}
