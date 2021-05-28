package wzjtech.test;

import com.github.sarxos.webcam.Webcam;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CameraTest {

  @Test
  public void testCamera() throws IOException {
    // get default webcam and open it
    Webcam webcam = Webcam.getDefault();
    webcam.open();

    // get image
    BufferedImage image = webcam.getImage();

    // save image to PNG file
    ImageIO.write(image, "PNG", new File("test.png"));
  }
}
