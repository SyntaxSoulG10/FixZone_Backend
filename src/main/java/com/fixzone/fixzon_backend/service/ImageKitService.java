package com.fixzone.fixzon_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;

/**
 * Service for managing image uploads to ImageKit.io.
 * This implementation uses direct HTTP calls instead of the ImageKit SDK 
 * to ensure maximum stability and zero dependency issues.
 */
@Service
public class ImageKitService {

    @Value("${imagekit.public-key}")
    private String publicKey;

    @Value("${imagekit.private-key}")
    private String privateKey;

    @Value("${imagekit.url-endpoint}")
    private String urlEndpoint;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Uploads an image to ImageKit.
     * @param imageData The image data (Base64 string or existing URL)
     * @param fileName Prefix for the uploaded filename
     * @return The permanent URL of the uploaded image
     */
    public String uploadImage(String imageData, String fileName) {
        if (imageData == null || imageData.isEmpty()) {
            return null;
        }

        // If it's already an external URL (not Base64), just return it
        if (imageData.startsWith("http") && !imageData.contains(";base64,")) {
            return imageData;
        }

        try {
            // 1. Prepare Base64 data
            String base64Data = imageData;
            if (base64Data.contains(";base64,")) {
                base64Data = base64Data.split(";base64,")[1];
            }

            // 2. Setup Headers (Basic Auth)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // ImageKit uses Basic Auth: PrivateKey as username, empty password
            String auth = privateKey + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            // 3. Prepare Multipart Body
            String extension = ".jpg";
            String lowerData = imageData.toLowerCase();
            if (lowerData.contains("image/png")) extension = ".png";
            else if (lowerData.contains("image/webp")) extension = ".webp";
            else if (lowerData.contains("image/gif")) extension = ".gif";
            
            String finalFileName = fileName + "-" + System.currentTimeMillis() + extension;
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", base64Data);
            body.add("fileName", finalFileName);
            body.add("useUniqueFileName", "true");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 4. Execute Upload
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://upload.imagekit.io/api/v1/files/upload",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                String url = (String) responseBody.get("url");
                System.out.println("****************************************************************");
                System.out.println("[IMAGEKIT SUCCESS] -> " + url);
                System.out.println("****************************************************************");
                return url;
            }
            
            throw new RuntimeException("ImageKit API returned error: " + response.getStatusCode());
            
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("ImageKit HTTP Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw new RuntimeException("ImageKit Upload Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: ImageKit Upload Failed: " + e.getMessage());
            throw new RuntimeException("ImageKit Upload Error: " + e.getMessage());
        }
    }
}
