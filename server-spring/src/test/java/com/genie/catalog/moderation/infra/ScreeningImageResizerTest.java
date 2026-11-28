// Unit test thu nhỏ ảnh gửi Gemini — UC-CAT-12 (update_gemini.md mục 11.2)
package com.genie.catalog.moderation.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.genie.catalog.moderation.application.ScreeningImagePort;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ScreeningImageResizerTest {

    private final ScreeningImageResizer resizer = new ScreeningImageResizer();

    private static byte[] encode(BufferedImage img, String format) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    @Test
    void downscalesLargePngToMax1024Jpeg() throws Exception {
        BufferedImage big = new BufferedImage(3000, 2000, BufferedImage.TYPE_INT_RGB);
        byte[] src = encode(big, "png");

        Optional<ScreeningImagePort.Image> res = resizer.prepare(src, "image/png");
        assertThat(res).isPresent();
        assertThat(res.get().mimeType()).isEqualTo("image/jpeg");

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(res.get().data()));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(1024);
        assertThat(decoded.getHeight()).isBetween(682, 684);
    }

    @Test
    void doesNotUpscaleSmallJpeg() throws Exception {
        BufferedImage small = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        byte[] src = encode(small, "jpg");

        Optional<ScreeningImagePort.Image> res = resizer.prepare(src, "image/jpeg");
        assertThat(res).isPresent();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(res.get().data()));
        assertThat(decoded.getWidth()).isEqualTo(800);
        assertThat(decoded.getHeight()).isEqualTo(600);
    }

    @Test
    void fillsTransparentPngWithWhiteBackground() throws Exception {
        BufferedImage alpha = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = alpha.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        byte[] src = encode(alpha, "png");

        Optional<ScreeningImagePort.Image> res = resizer.prepare(src, "image/png");
        assertThat(res).isPresent();
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(res.get().data()));
        int rgb = decoded.getRGB(0, 0) & 0xFFFFFF;
        assertThat(rgb).isEqualTo(Color.WHITE.getRGB() & 0xFFFFFF);
    }

    @Test
    void returnsEmptyOnGarbageBytes() {
        assertThat(resizer.prepare(new byte[] {1, 2, 3, 4, 5}, "image/jpeg")).isEmpty();
    }

    @Test
    void passesSmallWebpThroughUnchanged() {
        byte[] webp = new byte[1024];
        webp[0] = 'R';
        Optional<ScreeningImagePort.Image> res = resizer.prepare(webp, "image/webp");
        assertThat(res).isPresent();
        assertThat(res.get().mimeType()).isEqualTo("image/webp");
        assertThat(res.get().data()).isSameAs(webp);
    }

    @Test
    void rejectsImagesLargerThan8Mb() {
        byte[] huge = new byte[8 * 1024 * 1024 + 1];
        assertThat(resizer.prepare(huge, "image/jpeg")).isEmpty();
    }

    @Test
    void rejectsDecompressionBombWithoutDecodingIt() throws Exception {
        // ảnh 1 bit 8.000×6.000 = 48 triệu điểm ảnh (> 40 triệu): tệp PNG rất nhỏ nhưng giải nén RGB sẽ rất lớn
        BufferedImage huge = new BufferedImage(8000, 6000, BufferedImage.TYPE_BYTE_BINARY);
        byte[] png = encode(huge, "png");
        assertThat(png.length).isLessThan(1024 * 1024);
        assertThat(resizer.prepare(png, "image/png")).isEmpty();
    }

    @Test
    void largeButAllowedImageIsDecodedWithSubsamplingAndStillReaches1024() throws Exception {
        // 6.000×4.000 = 24 triệu điểm ảnh (< 40 triệu) → giải nén thưa, cạnh dài kết quả vẫn đúng 1024
        BufferedImage big = new BufferedImage(6000, 4000, BufferedImage.TYPE_BYTE_BINARY);
        Optional<ScreeningImagePort.Image> out = resizer.prepare(encode(big, "png"), "image/png");
        assertThat(out).isPresent();
        BufferedImage back = ImageIO.read(new ByteArrayInputStream(out.get().data()));
        assertThat(Math.max(back.getWidth(), back.getHeight())).isEqualTo(1024);
    }
}
