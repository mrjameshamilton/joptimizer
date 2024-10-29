package eu.jameshamilton;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.*;

public class ZipDebug {
    record EntryInfo(
        String name,
        long size,
        long compressedSize,
        long crc,
        int method,
        byte[] extra,
        String comment,
        long time
    ) {
        static EntryInfo from(ZipEntry entry) {
            return new EntryInfo(
                entry.getName(),
                entry.getSize(),
                entry.getCompressedSize(),
                entry.getCrc(),
                entry.getMethod(),
                entry.getExtra(),
                entry.getComment(),
                entry.getTime()
            );
        }

        void printDiff(EntryInfo other) {
            if (!name.equals(other.name)) System.out.printf("  Name: %s -> %s%n", name, other.name);
            if (size != other.size) System.out.printf("  Size: %d -> %d%n", size, other.size);
            if (compressedSize < other.compressedSize) System.out.printf("  Compressed: %d -> %d%n", compressedSize, other.compressedSize);
            if (crc != other.crc) System.out.printf("  CRC: %016x -> %016x%n", crc, other.crc);
            if (method != other.method) System.out.printf("  Method: %d -> %d%n", method, other.method);
            if (!Arrays.equals(extra, other.extra)) System.out.printf("  Extra: %s -> %s%n", 
                extra != null ? Base64.getEncoder().encodeToString(extra) : "null",
                other.extra != null ? Base64.getEncoder().encodeToString(other.extra) : "null");
            if (!Objects.equals(comment, other.comment)) System.out.printf("  Comment: %s -> %s%n", comment, other.comment);
            if (time != other.time) System.out.printf("  Time: %d -> %d%n", time, other.time);
        }
    }

    public static void compareJars(String input, String output) throws IOException {
        Map<String, EntryInfo> inputEntries = new LinkedHashMap<>();
        Map<String, EntryInfo> outputEntries = new LinkedHashMap<>();
        Set<String> allEntries = new TreeSet<>();

        // Gather input entries
        try (ZipFile zipFile = new ZipFile(input)) {
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                inputEntries.put(entry.getName(), EntryInfo.from(entry));
                allEntries.add(entry.getName());
            }
        }

        // Gather output entries
        try (ZipFile zipFile = new ZipFile(output)) {
            for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                outputEntries.put(entry.getName(), EntryInfo.from(entry));
                allEntries.add(entry.getName());
            }
        }

        System.out.printf("Input file size: %d%n", new File(input).length());
        System.out.printf("Output file size: %d%n", new File(output).length());
        System.out.printf("Size difference: %d bytes%n%n", new File(output).length() - new File(input).length());

        // Compare entries
        System.out.println("Entry differences:");
        for (String name : allEntries) {
            EntryInfo inputEntry = inputEntries.get(name);
            EntryInfo outputEntry = outputEntries.get(name);

            if (inputEntry == null) {
                System.out.printf("%s: Added in output%n", name);
                continue;
            }
            if (outputEntry == null) {
                System.out.printf("%s: Missing from output%n", name);
                continue;
            }
            if (!inputEntry.equals(outputEntry)) {
                System.out.printf("%s:%n", name);
                inputEntry.printDiff(outputEntry);
            }
        }

        // Compression statistics
        long totalInputCompressed = 0;
        long totalInputUncompressed = 0;
        long totalOutputCompressed = 0;
        long totalOutputUncompressed = 0;

        for (EntryInfo entry : inputEntries.values()) {
            if (entry.compressedSize >= 0) totalInputCompressed += entry.compressedSize;
            if (entry.size >= 0) totalInputUncompressed += entry.size;
        }
        for (EntryInfo entry : outputEntries.values()) {
            if (entry.compressedSize >= 0) totalOutputCompressed += entry.compressedSize;
            if (entry.size >= 0) totalOutputUncompressed += entry.size;
        }

        System.out.printf("%nCompression Statistics:%n");
        System.out.printf("Input - Uncompressed: %d, Compressed: %d, Ratio: %.2f%%%n",
            totalInputUncompressed, totalInputCompressed,
            100.0 * totalInputCompressed / totalInputUncompressed);
        System.out.printf("Output - Uncompressed: %d, Compressed: %d, Ratio: %.2f%%%n",
            totalOutputUncompressed, totalOutputCompressed,
            100.0 * totalOutputCompressed / totalOutputUncompressed);

        // Directory entries
        System.out.printf("%nDirectory entries:%n");
        System.out.println("Input:");
        inputEntries.keySet().stream()
            .filter(n -> n.endsWith("/"))
            .forEach(n -> System.out.println("  " + n));
        System.out.println("Output:");
        outputEntries.keySet().stream()
            .filter(n -> n.endsWith("/"))
            .forEach(n -> System.out.println("  " + n));

        // Entry order
        System.out.printf("%nEntry order differences:%n");
        List<String> inputOrder = new ArrayList<>(inputEntries.keySet());
        List<String> outputOrder = new ArrayList<>(outputEntries.keySet());
        if (!inputOrder.equals(outputOrder)) {
            System.out.println("Entries are in different order:");
            for (int i = 0; i < Math.min(inputOrder.size(), outputOrder.size()); i++) {
                if (!inputOrder.get(i).equals(outputOrder.get(i))) {
                    System.out.printf("Position %d: %s -> %s%n", i, inputOrder.get(i), outputOrder.get(i));
                }
            }
        }
    }
    public static void analyzeJarCompression(String filename) throws IOException {
        System.out.println("Analyzing: " + filename);

        File file = new File(filename);
        System.out.printf("File size: %,d bytes%n", file.length());

        try (JarFile jar = new JarFile(filename)) {
            class Stats {
                long totalUncompressed = 0;
                long totalCompressed = 0;
                long totalExtra = 0;
                int storedCount = 0;
                int deflatedCount = 0;
                int processedCount = 0;
            }

            Stats stats = new Stats();
            Map<String, Long> largestDiffs = new TreeMap<>();
            long totalEntries = jar.stream().count();

            System.out.printf("Processing %d entries...%n", totalEntries);

            // Process all entries
            jar.stream().forEach(entry -> {
                long uncompressed = entry.getSize();
                long compressed = entry.getCompressedSize();
                long extra = entry.getExtra() != null ? entry.getExtra().length : 0;

                if (entry.getMethod() == ZipEntry.STORED) {
                    stats.storedCount++;
                } else if (entry.getMethod() == ZipEntry.DEFLATED) {
                    stats.deflatedCount++;
                    // Track entries with big compression differences
                    long diff = compressed - uncompressed;
                    if (Math.abs(diff) > 1000) {  // Only track significant differences
                        largestDiffs.put(entry.getName(), diff);
                    }
                }

                stats.totalUncompressed += uncompressed;
                stats.totalCompressed += compressed;
                stats.totalExtra += extra;

                stats.processedCount++;
                if (stats.processedCount % 5000 == 0) {
                    System.out.printf("Processed %d/%d entries (%.1f%%)%n",
                        stats.processedCount, totalEntries,
                        (100.0 * stats.processedCount / totalEntries));
                }
            });

            System.out.println("\nCompression Statistics:");
            System.out.printf("Total entries: %d%n", totalEntries);
            System.out.printf("STORED entries: %d%n", stats.storedCount);
            System.out.printf("DEFLATED entries: %d%n", stats.deflatedCount);
            System.out.printf("Total uncompressed: %,d bytes%n", stats.totalUncompressed);
            System.out.printf("Total compressed: %,d bytes%n", stats.totalCompressed);
            System.out.printf("Total extra data: %,d bytes%n", stats.totalExtra);
            System.out.printf("Overall compression ratio: %.2f%%%n",
                (100.0 * stats.totalCompressed / stats.totalUncompressed));

            System.out.println("\nEntries with largest compression differences:");
            largestDiffs.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .forEach(e -> System.out.printf("%s: %,d bytes%n", e.getKey(), e.getValue()));

            // Analyze the structure using chunks to find EOCD more efficiently
            try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
                long fileLength = raf.length();
                // Search for EOCD in chunks from the end
                byte[] buffer = new byte[4096];
                long pos = fileLength - 22;  // Start at minimum EOCD position
                boolean found = false;
                long eocdPos = -1;

                while (pos >= 0 && !found) {
                    int readSize = (int)Math.min(buffer.length, pos + 1);
                    pos -= readSize - 4;  // Overlap by 4 bytes to catch signatures split across chunks
                    raf.seek(pos);
                    raf.readFully(buffer, 0, readSize);

                    // Search this chunk
                    for (int i = readSize - 4; i >= 0; i--) {
                        if (buffer[i] == 0x50 && buffer[i + 1] == 0x4b &&
                            buffer[i + 2] == 0x05 && buffer[i + 3] == 0x06) {
                            eocdPos = pos + i;
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    raf.seek(eocdPos + 10);  // Skip to entry counts
                    int totalEntriesInCD = Short.toUnsignedInt(raf.readShort());
                    long cdSize = Integer.toUnsignedLong(raf.readInt());
                    long cdOffset = Integer.toUnsignedLong(raf.readInt());

                    System.out.println("\nStructure Analysis:");
                    System.out.printf("EOCD position: %d%n", eocdPos);
                    System.out.printf("Entries in CD: %d%n", totalEntriesInCD);
                    System.out.printf("Data area: %,d bytes%n", cdOffset);
                    System.out.printf("Central directory: %,d bytes%n", cdSize);
                    System.out.printf("End of central directory: %,d bytes%n",
                        fileLength - eocdPos);
                    System.out.printf("Average data per entry: %.2f bytes%n",
                        (double)cdOffset / totalEntries);
                    System.out.printf("Average CD size per entry: %.2f bytes%n",
                        (double)cdSize / totalEntries);
                } else {
                    System.out.println("Could not find End of Central Directory!");
                }
            }
        }
    }

    public static void analyzeCentralDirectory(String filename) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
            long fileLength = raf.length();
            byte[] fullContent = new byte[(int)fileLength];
            raf.readFully(fullContent);

            // Find first PK signature
            int firstPKPos = -1;
            for (int i = 0; i < fullContent.length - 4; i++) {
                if (fullContent[i] == 0x50 && fullContent[i + 1] == 0x4b &&
                    fullContent[i + 2] == 0x03 && fullContent[i + 3] == 0x04) {
                    firstPKPos = i;
                    break;
                }
            }

            // Find EOCD
            int eocdPos = -1;
            for (int i = (int)fileLength - 22; i >= 0; i--) {
                if (fullContent[i] == 0x50 && fullContent[i + 1] == 0x4b &&
                    fullContent[i + 2] == 0x05 && fullContent[i + 3] == 0x06) {
                    eocdPos = i;
                    break;
                }
            }

            if (eocdPos == -1 || firstPKPos == -1) {
                System.out.println("Could not find required ZIP structures");
                return;
            }

            // Look for all local file headers and central directory entries
            Map<Integer, String> localHeaders = new TreeMap<>();
            Map<Integer, String> cdHeaders = new TreeMap<>();
            Map<Integer, Integer> gapSizes = new TreeMap<>();

            int lastPos = firstPKPos;
            System.out.println("Scanning local file entries...");

            // First scan local file headers (PK\003\004)
            for (int i = firstPKPos; i < eocdPos - 4; i++) {
                if (fullContent[i] == 0x50 && fullContent[i + 1] == 0x4b) {
                    if (fullContent[i + 2] == 0x03 && fullContent[i + 3] == 0x04) {
                        // Local file header
                        if (i > lastPos + 4) {
                            int gapSize = i - lastPos;
                            gapSizes.merge(gapSize, 1, Integer::sum);
                        }

                        // Read file name length
                        int nameLength = ((fullContent[i + 26] & 0xFF) |
                            ((fullContent[i + 27] & 0xFF) << 8));
                        int extraLength = ((fullContent[i + 28] & 0xFF) |
                            ((fullContent[i + 29] & 0xFF) << 8));

                        // Read filename
                        String name = new String(fullContent, i + 30, nameLength, StandardCharsets.UTF_8);
                        localHeaders.put(i, name);

                        lastPos = i;
                    }
                }
            }

            // Read central directory
            int cdOffset = ((fullContent[eocdPos + 16] & 0xFF) |
                ((fullContent[eocdPos + 17] & 0xFF) << 8) |
                ((fullContent[eocdPos + 18] & 0xFF) << 16) |
                ((fullContent[eocdPos + 19] & 0xFF) << 24));

            lastPos = cdOffset;
            for (int i = cdOffset; i < eocdPos - 4; i++) {
                if (fullContent[i] == 0x50 && fullContent[i + 1] == 0x4b &&
                    fullContent[i + 2] == 0x01 && fullContent[i + 3] == 0x02) {
                    // Central directory entry
                    if (i > lastPos + 4) {
                        int gapSize = i - lastPos;
                        System.out.printf("CD gap at 0x%08X: %d bytes%n", lastPos, gapSize);
                        // Print the gap contents if small enough
                        if (gapSize < 32) {
                            System.out.print("Gap contents: ");
                            for (int j = 0; j < gapSize; j++) {
                                System.out.printf("%02X ", fullContent[lastPos + j]);
                            }
                            System.out.println();
                        }
                    }

                    int nameLength = ((fullContent[i + 28] & 0xFF) |
                        ((fullContent[i + 29] & 0xFF) << 8));
                    String name = new String(fullContent, i + 46, nameLength, StandardCharsets.UTF_8);
                    cdHeaders.put(i, name);

                    lastPos = i;
                }
            }

            System.out.println("\nAnalysis Summary:");
            System.out.println("First PK signature at: " + firstPKPos);
            System.out.println("Central directory at: " + cdOffset);
            System.out.println("End of central directory at: " + eocdPos);
            System.out.println("Local file entries: " + localHeaders.size());
            System.out.println("Central directory entries: " + cdHeaders.size());

            System.out.println("\nGap distribution between local entries:");
            gapSizes.forEach((size, count) ->
                System.out.printf("%d bytes: %d occurrences%n", size, count));

            // Check for padding at start
            if (firstPKPos > 0) {
                System.out.println("\nInitial padding:");
                for (int i = 0; i < Math.min(firstPKPos, 32); i++) {
                    System.out.printf("%02X ", fullContent[i]);
                }
                System.out.println();
            }

            // Sample some entries for debugging
            System.out.println("\nSample entries (first 5):");
            localHeaders.entrySet().stream()
                .limit(5)
                .forEach(e -> System.out.printf("At 0x%08X: %s%n", e.getKey(), e.getValue()));
        }
    }



    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.out.println("Usage: ZipDebug <input.jar> <output.jar>");
            return;
        }
        System.out.println("start");
        //analyzeCentralDirectory(args[0]);
        //analyzeCentralDirectory(args[1]);
        analyzeJarCompression(args[0]);
        analyzeJarCompression(args[1]);
       // compareJars(args[0], args[1]);
    }
}