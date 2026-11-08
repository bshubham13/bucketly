package com.bucketly.api.dto;

import org.springframework.core.io.Resource;

public record PhotoDownload(Resource resource, String contentType) {
}

