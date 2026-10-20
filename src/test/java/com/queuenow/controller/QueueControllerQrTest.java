package com.queuenow.controller;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.queuenow.service.QRCodeService;
import com.queuenow.service.QueueService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class QueueControllerQrTest {

    private final QueueController controller =
            new QueueController(mock(QueueService.class), new QRCodeService());

    @Test
    void joinQrUsesCurrentForwardedHost() throws Exception {
        MockHttpServletRequest request = forwardedRequest();

        byte[] image = (byte[]) controller.getJoinQRCode(request).getBody();

        assertEquals("https://queuenow.example/join.html", decodeQr(image));
    }

    @Test
    void trackingQrUsesCurrentForwardedHost() throws Exception {
        MockHttpServletRequest request = forwardedRequest();

        byte[] image = (byte[]) controller.getTrackQRCode(12, "Alex Doe", request).getBody();

        assertEquals("https://queuenow.example/track.html?token=12&name=Alex+Doe", decodeQr(image));
    }

    @Test
    void joinQrUsesConfiguredLocalTunnelWhenNoProxyHostIsPresent() throws Exception {
        ReflectionTestUtils.setField(controller, "localPublicUrl", "https://local-tunnel.example/");
        MockHttpServletRequest request = new MockHttpServletRequest();

        byte[] image = (byte[]) controller.getJoinQRCode(request).getBody();

        assertEquals("https://local-tunnel.example/join.html", decodeQr(image));
    }

    @Test
    void forwardedHostTakesPrecedenceOverConfiguredLocalTunnel() throws Exception {
        ReflectionTestUtils.setField(controller, "localPublicUrl", "https://local-tunnel.example");
        MockHttpServletRequest request = forwardedRequest();

        byte[] image = (byte[]) controller.getJoinQRCode(request).getBody();

        assertEquals("https://queuenow.example/join.html", decodeQr(image));
    }

    private MockHttpServletRequest forwardedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("internal-service");
        request.addHeader("X-Forwarded-Host", "queuenow.example");
        request.addHeader("X-Forwarded-Proto", "https");
        return request;
    }

    private String decodeQr(byte[] imageBytes) throws Exception {
        assertNotNull(imageBytes);
        var image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        assertNotNull(image);
        return new MultiFormatReader().decode(
                new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))
        ).getText();
    }
}
