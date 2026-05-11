package com.auction.auctionservice.dto.response;

public record ImageUploadResponse(String imageUrl, String fileName, long sizeBytes, String contentType) {
}
