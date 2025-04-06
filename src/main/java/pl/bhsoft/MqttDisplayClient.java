package pl.bhsoft;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import pl.bhsoft.display.DisplayChanger;
import pl.bhsoft.display.DisplayChangerFactory;

import java.nio.charset.StandardCharsets;

public final class MqttDisplayClient {
    private static volatile boolean running = true;
    private final Args arguments;
    private final MqttClient client;
    private final MqttConnectOptions options;
    private final DisplayChanger displayChanger = DisplayChangerFactory.create();

    public MqttDisplayClient(Args args) {
        this.arguments = args;
        try {
            this.client = new MqttClient(
                    arguments.serverUri,
                    arguments.clientId,
                    new MemoryPersistence()
            );
        } catch (MqttException e) {
            throw new IllegalArgumentException(e);
        }
        this.options = new MqttConnectOptions();
        options.setUserName(arguments.username);
        options.setPassword(arguments.password.toCharArray());
        options.setCleanSession(true);
    }

    private void commandArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        if ("external".equals(payload)) {
            System.out.println("External display!");
            displayChanger.switchToExternal();

        } else if ("internal".equals(payload)) {
            System.out.println("Internal display!");
            displayChanger.switchToInternal();
        }
    }

    private void connect() throws MqttException {
        client.connect(options);
        client.subscribe(arguments.commandTopic, this::commandArrived);
    }

    private void sendMessage(String topic, String message) throws MqttException {
        client.publish(topic, message.getBytes(StandardCharsets.UTF_8), 0, true);
    }

    private void tryCloseMqttClient() {
        try {
            sendMessage(arguments.availableTopic, "offline");
            client.disconnect();
            client.close(); // Attempt to close the MQTT client gracefully
            System.out.println("MQTT client closed successfully.");
        } catch (MqttException e) {
            System.err.println("Failed to close MQTT client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void run() {
        Thread workerThread = new Thread(() -> {
            try {
                connect(); // Try to establish the MQTT connection
            } catch (MqttException e) {
                System.err.println("Failed to connect to MQTT broker: " + e.getMessage());
                e.printStackTrace();
                running = false; // Exit the thread if the connection fails
                return;
            }
            while (running) {
                sendAvailabilityInfoAndWait();
            }
            tryCloseMqttClient();
            System.out.println("Worker thread stopped gracefully.");
        });

        // Add shutdown hook to handle Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered. Stopping worker... (Up to 5 seconds)");
            running = false; // Signal the thread to stop
            try {
                workerThread.join(); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        // Start the worker thread
        workerThread.start();
    }

    private void sendAvailabilityInfoAndWait() {
        try {
            // Send online message and wait
            sendMessage(arguments.availableTopic, "online");
            System.out.println("Worker is running...");
            Thread.sleep(this.arguments.waitTimeMilliseconds); // Sleep to avoid busy-waiting
        } catch (MqttException e) {
            System.err.println("Failed to publish message: " + e.getMessage());
            e.printStackTrace();
            running = false; // Exit loop if a critical error occurs
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            System.out.println("Worker thread interrupted");
        }
    }
}
