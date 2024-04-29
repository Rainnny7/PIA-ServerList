package me.braydon.pia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.braydon.pia.readme.ReadMeManager;
import net.lingala.zip4j.ZipFile;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Braydon
 */
public final class PIAServerList {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final String OPENVPN_FILES_ENDPOINT = "https://www.privateinternetaccess.com/openvpn/openvpn.zip";
    private static final File SERVERS_FILE = new File("servers.json");
    private static final int TOTAL_RUNS = 3;

    @SneakyThrows
    public static void main(@NonNull String[] args) {
        Map<String, String> regionAddresses = getRegionAddresses(); // Get region address from PIA

        for (int i = 0; i < TOTAL_RUNS; i++) {
            Set<PIAServer> servers = getNewServers(regionAddresses); // Get the new servers from PIA
            int before = servers.size();
            servers.addAll(loadServersFromFile()); // Load servers from the file
            int loaded = servers.size() - before;
            System.out.println("Loaded " + loaded + " server(s) from the servers file");

            // Delete servers that haven't been seen in more than a week
            before = servers.size();
            servers.removeIf(server -> (System.currentTimeMillis() - server.getLastSeen()) >= TimeUnit.DAYS.toMillis(7L));
            System.out.println("Removed " + (before - servers.size()) + " server(s) that haven't been seen in more than a week");

            // Write the servers to the servers file
            System.out.println("Writing servers file...");
            try (FileWriter fileWriter = new FileWriter(SERVERS_FILE)) {
                GSON.toJson(servers, fileWriter);
            }
            System.out.println("Done, wrote " + servers.size() + " servers to the file (+" + (servers.size() - loaded) + " New)");

            // Update the README.md file
            ReadMeManager.update(servers);

            // Sleep before running again
            if (i < TOTAL_RUNS - 1) {
                System.out.println("Sleeping...");
                Thread.sleep(TimeUnit.MINUTES.toMillis(4L));
            }
        }
    }

    /**
     * Get the new servers from the
     * OpenVPN files provided by PIA.
     *
     * @param regionAddresses the region addresses
     * @return the new servers
     */
    @SneakyThrows @NonNull
    private static Set<PIAServer> getNewServers(@NonNull Map<String, String> regionAddresses) {
        Set<PIAServer> servers = new HashSet<>(); // The new servers to return
        Lookup.setDefaultResolver(new SimpleResolver("1.1.1.1")); // Use CF DNS

        for (Map.Entry<String, String> entry : regionAddresses.entrySet()) {
            String region = entry.getKey();
            String address = entry.getValue();

            Record[] records = new Lookup(address, Type.A).run(); // Resolve A records
            if (records == null) { // No A records resolved
                continue;
            }
            System.out.println("Resolved " + records.length + " A Records for region " + region);
            for (Record record : records) {
                servers.add(new PIAServer(((ARecord) record).getAddress().getHostAddress(), region, System.currentTimeMillis()));
            }
        }
        return servers;
    }

    /**
     * Get the region addresses from
     * the OpenVPN files provided by PIA.
     *
     * @return the mapped region addresses
     */
    @SneakyThrows
    private static Map<String, String> getRegionAddresses() {
        Map<String, String> regionAddresses = new HashMap<>();
        File serversZip = new File("servers.zip"); // The zip file containing the servers

        // Download the OpenVPN servers zip from PIA
        long before = System.currentTimeMillis();
        System.out.println("Downloading OpenVPN files from PIA...");
        try (InputStream inputStream = new URL(OPENVPN_FILES_ENDPOINT).openStream();
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(serversZip)
        ) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
        assert serversZip.exists(); // Confirm the zip exists
        System.out.println("Downloaded in " + (System.currentTimeMillis() - before) + "ms, extracting...");

        // Extract the servers zip files
        before = System.currentTimeMillis();
        try (ZipFile zip = new ZipFile(serversZip)) {
            zip.extractAll("servers");
        }
        serversZip.delete(); // Delete the zip file after extraction
        System.out.println("Extracted in " + (System.currentTimeMillis() - before) + "ms");

        // Iterate over the OpenVPN files downloaded from PIA
        File serversDir = new File("servers"); // The dir where the downloaded OpenVPN files are stored
        File[] openVpnFiles = serversDir.listFiles();
        assert openVpnFiles != null;
        System.out.println("Found " + openVpnFiles.length + " OpenVPN files, reading them...");

        for (File file : openVpnFiles) {
            String region = file.getName().split("\\.")[0]; // The server region
            try {
                for (String line : Files.readAllLines(file.toPath())) {
                    // Line doesn't contain the remote server, ignore it
                    if (!line.startsWith("remote ")) {
                        continue;
                    }
                    // Store the region -> address mapping
                    regionAddresses.put(region, line.split(" ")[1]);
                }
            } finally {
                file.delete(); // Delete the OpenVPN file after reading it
            }
        }
        serversDir.delete(); // Delete the servers dir after reading the OpenVPN files

        return regionAddresses;
    }

    /**
     * Load the servers from the json file.
     *
     * @return the loaded servers
     */
    @SneakyThrows
    private static List<PIAServer> loadServersFromFile() {
        if (!SERVERS_FILE.exists()) { // No servers file to load
            return new ArrayList<>();
        }
        try (FileReader fileReader = new FileReader(SERVERS_FILE);
             JsonReader jsonReader = new JsonReader(fileReader)
        ) {
            return GSON.fromJson(jsonReader, new TypeToken<List<PIAServer>>() {}.getType());
        }
    }
}