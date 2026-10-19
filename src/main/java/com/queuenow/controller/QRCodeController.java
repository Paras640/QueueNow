package com.queuenow.controller;

import com.queuenow.service.QRCodeService;
import com.google.zxing.WriterException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST controller for generating QR codes.
 *
 * <p>Exposes endpoints under {@code /api/qr} that accept arbitrary input text
 * and return rendered PNG QR code images.</p>
 */
@RestController
@RequestMapping("/api/qr")
public class QRCodeController {

    /** Service used to generate QR code images from string content. */
    private final QRCodeService qrService;

    /**
     * Constructs a new QRCodeController with required dependencies.
     *
     * @param qrService the QR code generation service injected by Spring
     */
    public QRCodeController(QRCodeService qrService) {
        this.qrService = qrService;
    }

    /**
     * Generates and returns a PNG QR code image for the specified text parameter.
     *
     * <p>Endpoint: {@code GET /api/qr?text=...}</p>
     *
     * @param text the string content to encode into the QR code
     * @return a {@link ResponseEntity} containing the PNG image bytes with HTTP 200 OK,
     *         or HTTP 400 Bad Request if the input text is empty
     * @throws WriterException if QR code encoding encounters an error
     * @throws IOException     if streaming the generated image bytes fails
     */
    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQR(@RequestParam String text)
            throws WriterException, IOException {

        // Validate that the provided text is not empty or composed purely of whitespace
        if (text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Delegate to the service to produce raw PNG image bytes from the input text
        byte[] qrImage = qrService.generate(text);

        // Build and return HTTP 200 OK response with the image/png content type and image payload
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }
}
