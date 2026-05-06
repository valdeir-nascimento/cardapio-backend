package com.cardapio.ordering.infrastructure.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZxingQrCodeGeneratorTest {

    private final ZxingQrCodeGenerator generator = new ZxingQrCodeGenerator();

    @Test
    void generatesPngThatDecodesBackToToken() throws Exception {
        String token = UUID.randomUUID().toString();

        byte[] png = generator.generatePng(token, 256);

        assertThat(png).isNotEmpty();
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(256);

        var source = new BufferedImageLuminanceSource(img);
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));
        var result = new MultiFormatReader().decode(bitmap);

        assertThat(result.getText()).isEqualTo(token);
    }
}
