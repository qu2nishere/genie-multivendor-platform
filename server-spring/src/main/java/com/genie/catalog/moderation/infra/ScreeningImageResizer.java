// Thu nhỏ ảnh trước khi gửi Gemini (Java ImageIO + Graphics2D) — UC-CAT-12 (update_gemini.md B-D3, mục 11.2)
package com.genie.catalog.moderation.infra;

import com.genie.catalog.moderation.application.ScreeningImagePort;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thu nhỏ ảnh về cạnh dài ≤ 1024 px, nén JPEG chất lượng 0,85, tô nền trắng cho PNG có kênh trong suốt.
 * WebP không được {@link ImageIO} chuẩn đọc → gửi nguyên nếu ≤ 4 MB, lớn hơn thì bỏ (không thêm thư viện).
 */
@Component
public class ScreeningImageResizer implements ScreeningImagePort {

    private static final Logger log = LoggerFactory.getLogger(ScreeningImageResizer.class);
    static final int MAX_SIDE = 1024;
    static final int MAX_SOURCE_BYTES = 8 * 1024 * 1024;
    static final int MAX_WEBP_BYTES = 4 * 1024 * 1024;
    /** Chặn "bom giải nén": tệp nhỏ nhưng kích thước khổng lồ (vd PNG 20.000×20.000 một màu ≈ 1,6 GB khi giải nén). */
    static final long MAX_PIXELS = 40_000_000L;
    private static final float JPEG_QUALITY = 0.85f;

    @Override
    public Optional<Image> prepare(byte[] source, String mimeType) {
        if (source == null || source.length == 0 || source.length > MAX_SOURCE_BYTES) {
            return Optional.empty();
        }
        if ("image/webp".equalsIgnoreCase(mimeType)) {
            return source.length <= MAX_WEBP_BYTES
                    ? Optional.of(new Image("image/webp", source))
                    : Optional.empty();
        }
        try {
            BufferedImage img = decodeSafely(source);
            if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) {
                return Optional.empty();
            }
            int w = img.getWidth();
            int h = img.getHeight();
            double scale = Math.min(1.0, MAX_SIDE / (double) Math.max(w, h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));

            BufferedImage rgb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, nw, nh);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(img, 0, 0, nw, nh, null);
            } finally {
                g.dispose();
            }

            byte[] jpeg = writeJpeg(rgb);
            return jpeg.length == 0 ? Optional.empty() : Optional.of(new Image("image/jpeg", jpeg));
        } catch (IOException | RuntimeException e) {
            log.debug("Bỏ ảnh khi thu nhỏ cho AI sàng lọc: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Đọc kích thước TRƯỚC khi giải nén (không dùng {@code ImageIO.read} — nó dựng nguyên ảnh gốc trong RAM, ảnh
     * khổng lồ gây {@code OutOfMemoryError} mà {@code catch} không bắt). Quá {@link #MAX_PIXELS} → bỏ; còn lại giải nén
     * thưa ({@code setSourceSubsampling}) sao cho cạnh dài vẫn ≥ {@link #MAX_SIDE} rồi mới thu nhỏ đẹp bằng Graphics2D.
     */
    static BufferedImage decodeSafely(byte[] source) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (in == null) return null;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || (long) w * h > MAX_PIXELS) {
                    log.debug("Bỏ ảnh khi sàng lọc AI: kích thước {}×{} vượt giới hạn", w, h);
                    return null;
                }
                ImageReadParam param = reader.getDefaultReadParam();
                int factor = Math.max(1, Math.max(w, h) / MAX_SIDE);
                if (factor > 1) {
                    param.setSourceSubsampling(factor, factor, 0, 0);
                }
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    private static byte[] writeJpeg(BufferedImage rgb) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("Không tìm thấy JPEG ImageWriter");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(rgb, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
