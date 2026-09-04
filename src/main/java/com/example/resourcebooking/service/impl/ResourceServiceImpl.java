package com.example.resourcebooking.service.impl;

import com.example.resourcebooking.dto.common.PageResponse;
import com.example.resourcebooking.dto.resource.ResourceCreateRequest;
import com.example.resourcebooking.dto.resource.ResourceResponse;
import com.example.resourcebooking.dto.resource.ResourceUpdateRequest;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationConflictException;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.mapper.ResourceMapper;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ResourceServiceImpl implements ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "capacity", "pricePerHour", "available", "createdAt"
    );

    private final ResourceRepository resourceRepository;
    private final ResourceMapper resourceMapper;

    public ResourceServiceImpl(ResourceRepository resourceRepository, ResourceMapper resourceMapper) {
        this.resourceRepository = resourceRepository;
        this.resourceMapper = resourceMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getAllResources(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createValidatedPageable(page, size, sortBy, sortDir);
        Page<Resource> resourcePage = resourceRepository.findAll(pageable);
        Page<ResourceResponse> responsePage = resourcePage.map(resourceMapper::toResponse);
        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
        return resourceMapper.toResponse(resource);
    }

    @Override
    @Transactional
    public ResourceResponse createResource(ResourceCreateRequest request) {
        log.info("Creating new resource with name: {}", request.getName());

        if (resourceRepository.existsByName(request.getName())) {
            log.warn("Resource creation failed: Resource with name '{}' already exists", request.getName());
            throw new ReservationConflictException("Resource with name '" + request.getName() + "' already exists");
        }

        Resource resource = resourceMapper.toEntity(request);
        Resource saved = resourceRepository.save(resource);
        log.info("Resource successfully created with ID: {}", saved.getId());
        return resourceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(Long id, ResourceUpdateRequest request) {
        log.info("Updating resource with ID: {}", id);

        Resource existingResource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        resourceMapper.updateEntity(existingResource, request);
        Resource updated = resourceRepository.save(existingResource);
        log.info("Resource with ID: {} successfully updated", updated.getId());
        return resourceMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        log.info("Deleting resource with ID: {}", id);

        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with ID: " + id);
        }

        resourceRepository.deleteById(id);
        log.info("Resource with ID: {} successfully deleted", id);
    }

    private Pageable createValidatedPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new BadRequestException("Page index cannot be less than zero");
        }
        if (size <= 0) {
            throw new BadRequestException("Page size must be greater than zero");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page size cannot exceed maximum limit of " + MAX_PAGE_SIZE);
        }

        String field = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : "id";
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new BadRequestException("Invalid sort field: '" + field + "'. Allowed fields: " + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, field));
    }
}
