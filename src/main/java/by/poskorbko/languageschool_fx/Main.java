package by.poskorbko.languageschool_fx;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class Main {
    public static void main(String[] args) throws IOException {
        BufferedImage original = ImageIO.read(new File(new File(System.getProperty("user.dir")), "/src/main/resources/by/poskorbko/languageschool_fx/no_avatar.png"));
        BufferedImage resized = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, 48, 48, null);
        g.dispose();

        // 4. Пишем в PNG через ByteArrayOutputStream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "PNG", baos);

        // 5. Кодируем в base64
        System.out.println(Base64.getEncoder().encodeToString(baos.toByteArray()));
    }
}
