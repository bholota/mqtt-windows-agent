package pl.bhsoft;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Args {
    public final String serverUri;
    public final String clientId;
    public final String username;
    public final String password;
    public final String hostName;
    public final String availableTopic;
    public final String availablePayloadOnline;
    public final String availablePayloadOffline;
    public final String commandTopic;
    public final long waitTimeMilliseconds;

    public Args(String[] args) {
        // Parse command-line arguments
        String configFilePath = null;
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) {
                configFilePath = args[i + 1];
                break;
            }
        }
        Properties properties = new Properties();
        // Validate and use the config file path
        if (configFilePath != null) {
            Path path = Paths.get(configFilePath);
            if (Files.exists(path)) {
                System.out.println("Config file found at: " + configFilePath);
                // Further processing of the file
                try {
                    properties.load(new FileReader(configFilePath));
                } catch (IOException e) {
                    System.err.println("Error: couldn't load the configuration file");
                    throw new IllegalArgumentException();
                }
            } else {
                System.err.println("Config file not found at: " + configFilePath);
                throw new IllegalArgumentException();
            }
        } else {
            System.err.println("Usage: java ConfigParser --config <properties_file_path>");
            throw new IllegalArgumentException();
        }
        serverUri = properties.getProperty("mqtt.serverUri");
        clientId = properties.getProperty("mqtt.clientId");
        username = properties.getProperty("mqtt.username");
        password = properties.getProperty("mqtt.password");
        hostName = properties.getProperty("mqtt.agent.hostname");
        availableTopic = properties.getProperty("mqtt.agent.available.topic");
        availablePayloadOnline = properties.getProperty("mqtt.agent.available.payload.online");
        availablePayloadOffline = properties.getProperty("mqtt.agent.available.payload.offline");
        commandTopic = properties.getProperty("mqtt.agent.command.topic");
        waitTimeMilliseconds = Long.parseLong(properties.getProperty("mqtt.agent.available.wait_time_milliseconds"));
    }


}
