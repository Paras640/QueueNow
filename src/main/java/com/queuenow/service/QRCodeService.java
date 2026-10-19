package com.queuenow.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service component responsible for generating QR code images.
 *
 * <p>Uses Google's ZXing ("Zebra Crossing") library to encode arbitrary text
 * into a 2D matrix and write it out as a PNG formatted byte array.</p>
 */
@Service
public class QRCodeService {

    /**
     * Generates a 250x250 PNG QR code image byte array for the provided input text.
     *
     * @param text the text or URL to be encoded into the QR code
     * @return a byte array containing the generated PNG image data
     * @throws WriterException if an error occurs during encoding of the QR code matrix
     * @throws IOException     if an error occurs when writing the image stream to bytes
     */
    public byte[] generate(String text)
            throws WriterException, IOException {

        // Encode the provided text into a 250x250 pixel 2D BitMatrix representation
        BitMatrix matrix = new QRCodeWriter()
                .encode(text, BarcodeFormat.QR_CODE, 250, 250);

        // Prepare an in-memory byte stream buffer to store the rendered PNG image
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // Convert the BitMatrix to a PNG image format and write it to the output stream
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);

        // Return the binary PNG image data as a byte array
        return output.toByteArray();
    }
}
