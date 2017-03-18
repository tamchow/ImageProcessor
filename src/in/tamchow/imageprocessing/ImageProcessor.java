package in.tamchow.imageprocessing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Resamples an image for 3D output generation
 */
public final class ImageProcessor {
    private ImageProcessor() {
    }

    public static BufferedImage resample(BufferedImage input,
                                         int horizontalCells,
                                         int verticalCells) {
        horizontalCells = (horizontalCells <= 0) ? input.getWidth() : Math.min(input.getWidth(), horizontalCells);
        verticalCells = (verticalCells <= 0) ? input.getHeight() : Math.min(input.getHeight(), verticalCells);
        BufferedImage output = new BufferedImage(horizontalCells, verticalCells, input.getType());
        final int hDistance = (input.getWidth() / horizontalCells), vDistance = (input.getHeight() / verticalCells);
        final int nPixels = vDistance * hDistance;
        for (int y = 0, outY = 0; y < input.getHeight() && outY < output.getHeight(); y += vDistance, ++outY) {
            for (int x = 0, outX = 0; x < input.getWidth() && outX < output.getWidth(); x += hDistance, ++outX) {
                int rAccum = 0, gAccum = 0, bAccum = 0;
                for (int dy = 0; dy < vDistance; ++dy) {
                    for (int dx = 0; dx < hDistance; ++dx) {
                        Color pixel = new Color(input.getRGB(x + dx, y + dy));
                        rAccum += pixel.getRed();
                        gAccum += pixel.getGreen();
                        bAccum += pixel.getBlue();
                    }
                }
                Color averageColor = new Color(rAccum / nPixels, gAccum / nPixels, bAccum / nPixels);
                int intensity = toGray(averageColor);
                output.setRGB(outX, outY, new Color(intensity, intensity, intensity).getRGB());
            }
        }
        return output;
    }

    public static int toGray(Color color) {
        return (int) (0.21 * color.getRed() + 0.72 * color.getGreen() + 0.07 * color.getBlue());
    }

    public static String[][] extractData(BufferedImage input/*Assume greyscale*/,
                                         double scaleFactor,
                                         boolean for3D/*otherwise intensity map*/) {
        if (for3D) {
            String[][] output = new String[input.getWidth() * input.getHeight()][3];
            int k = 0;
            for (int y = 0; y < input.getHeight(); ++y) {
                for (int x = 0; x < input.getWidth(); ++x) {
                    output[k][0] = "" + x;
                    output[k][1] = "" + y;
                    output[k++][2] = "" + (new Color(input.getRGB(x, y)).getRed() * scaleFactor / 255.0);
                }
            }
            return output;
        }
        String[][] output = new String[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                output[y][x] = "" + (new Color(input.getRGB(x, y)).getRed() * scaleFactor / 255.0);
            }
        }
        return output;
    }

    public static String asCSV(String[][] data) {
        return Arrays.stream(data).map(line -> String.join(", ", line)).collect(Collectors.joining("\n"));
    }

    public static void writeCSV(String data, File output) {
        try (FileWriter fileWriter = new FileWriter(output)) {
            fileWriter.write(data);
            fileWriter.flush();
        } catch (IOException ioException) {
            throw new RuntimeException("I/O error encountered", ioException);
        }
    }

    public static void processImageToCSV(String inputPath, int horizontalCells, int verticalCells,
                                         double scaleFactor, boolean for3D, String outputPath) {
        try {
            writeCSV(asCSV(extractData(
                    resample(ImageIO.read(new File(inputPath)),
                            horizontalCells, verticalCells),
                    scaleFactor, for3D)
            ), new File(outputPath));
        } catch (IOException ioException) {
            throw new RuntimeException("I/O error encountered", ioException);
        }
    }

    public static void main(String[] args) {
        if (args.length != 6) {
            throw new IllegalArgumentException("Improper arguments");
        }
        processImageToCSV(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                Double.parseDouble(args[3]), Boolean.parseBoolean(args[4]), args[5]);
    }
}
