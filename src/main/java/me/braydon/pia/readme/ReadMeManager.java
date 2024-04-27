package me.braydon.pia.readme;

import lombok.NonNull;
import me.braydon.pia.PIAServer;
import me.braydon.pia.common.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Braydon
 */
public final class ReadMeManager {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM d yyyy h:m a");

    /**
     * Copy the template README.md file from the jar
     * and update it with data from the given servers.
     *
     * @param servers the server data
     */
    public static void update(@NonNull Set<PIAServer> servers) {
        System.out.println("Updating README.md...");
        try (InputStream templateResource = ReadMeManager.class.getClassLoader().getResourceAsStream("README_TEMPLATE.md")) {
            assert templateResource != null; // Ensure the template is present

            // Copy the template README.md to the root directory
            Path localReadMe = new File("README.md").toPath(); // The local README.md file
            Files.copy(templateResource, localReadMe, StandardCopyOption.REPLACE_EXISTING);

            // Replace variables in the README.md file
            String contents = new String(Files.readAllBytes(localReadMe));
            contents = contents.replace("<total-servers>", DECIMAL_FORMAT.format(servers.size())); // Total servers variable
            contents = contents.replace("<last-updated>", DATE_FORMAT.format(new Date()).replace(" ", "_")); // Total servers variable

            // Write the total servers per-region table
            Map<String, Integer> regionCounts = new HashMap<>();
            for (PIAServer server : servers) {
                regionCounts.put(server.getRegion(), regionCounts.getOrDefault(server.getRegion(), 0) + 1);
            }
            contents = contents.replace("<total-regions>", DECIMAL_FORMAT.format(regionCounts.keySet().size())); // Total regions variable

            contents = contents.replace("<region-table-entry>", regionCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // Sort from highest to lowest
                    .map(entry -> "| " + StringUtils.capitalizeFully(entry.getKey(), '_') + " | " + entry.getValue() + " |") // Map the region to the count
                    .reduce((a, b) -> a + "\n" + b).orElse("")); // Reduce the entries to a single string

            // Write the contents to the file
            Files.write(localReadMe, contents.getBytes());
            System.out.println("Done!");
        } catch (IOException ex) {
            System.err.println("Failed to update the README.md file");
            ex.printStackTrace();
        }
    }
}