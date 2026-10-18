
package com.queuenow.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QRCodeService {

    public byte[] generate(String text)
            throws WriterException, IOException {

        BitMatrix matrix = new QRCodeWriter()
                .encode(text, BarcodeFormat.QR_CODE, 250, 250);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);

        return output.toByteArray();
    }
}
