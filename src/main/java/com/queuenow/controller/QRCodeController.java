
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

@RestController
@RequestMapping("/api/qr")
public class QRCodeController {

    private final QRCodeService qrService;

    public QRCodeController(QRCodeService qrService) {
        this.qrService = qrService;
    }

    @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQR(@RequestParam String text)
            throws WriterException, IOException {

        if (text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] qrImage = qrService.generate(text);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }
}
