import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class GenerateTestFiles {

    public static void main(String[] args) throws IOException {
        createFile("fileA.txt", 20);
        createFile("fileB.txt", 20);

        System.out.println("Both test files created.");
    }

    private static void createFile(String fileName, int sizeInMB)
            throws IOException {

        byte[] buffer = new byte[8192];
        Random random = new Random();

        try (FileOutputStream output =
                     new FileOutputStream(fileName)) {

            long bytesToWrite = sizeInMB * 1024L * 1024L;

            while (bytesToWrite > 0) {
                random.nextBytes(buffer);

                int bytes = (int) Math.min(
                        buffer.length, bytesToWrite
                );

                output.write(buffer, 0, bytes);
                bytesToWrite -= bytes;
            }
        }

        System.out.println(fileName + " created: " + sizeInMB + " MB");
    }
}