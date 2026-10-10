
package com.nettransfer.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class TransferHistory {

    private static final String FILE_NAME = "transfer_history.csv";

    private static boolean headerWritten = false;

    public static synchronized void recordTransfer(
            String timestamp,
            String clientIp,
            String fileName,
            long bytesReceived,
            long durationMs,
            double processingRate,
            String status) {

        try (PrintWriter writer = new PrintWriter(
                new FileWriter(FILE_NAME, true))) {

            // Write the header only when the file is empty
            if (!headerWritten) {
                java.io.File file = new java.io.File(FILE_NAME);

                if (file.length() == 0) {
                    writer.println(
                            "Timestamp,Client IP,Filename,Bytes Received,"
                                    + "Duration (ms),Processing Rate (MiB/s),Status"
                    );
                }

                headerWritten = true;
            }

            writer.printf(
                    "%s,%s,%s,%d,%d,%.2f,%s%n",
                    escapeCsv(timestamp),
                    escapeCsv(clientIp),
                    escapeCsv(fileName),
                    bytesReceived,
                    durationMs,
                    processingRate,
                    escapeCsv(status)
            );

        } catch (IOException e) {
            System.out.println(
                    "Could not save transfer history: " + e.getMessage()
            );
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
