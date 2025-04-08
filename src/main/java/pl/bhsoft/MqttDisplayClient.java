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

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::tryCloseMqttClient));

        while (running) {
            try {
                if (!client.isConnected()) {
                    connect();
                }
                sendAvailabilityInfoAndWait();
            } catch (MqttException e) {
                System.err.println("Connection lost or error occurred: " + e.getMessage());
                try {
                    // Wait before attempting to reconnect
                    Thread.sleep(5000); // 5 second delay between reconnection attempts
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    running = false;
                    break;
                }
            }
        }
    }

    private void connect() throws MqttException {
        int maxRetries = 3;
        int retryCount = 0;
        MqttException lastException = null;

        while (retryCount < maxRetries && running) {
            try {
                client.connect(options);
                client.subscribe(arguments.commandTopic, this::commandArrived);
                System.out.println("Connected successfully to MQTT broker");
                return;
            } catch (MqttException e) {
                lastException = e;
                retryCount++;
                System.err.printf("Connection attempt %d of %d failed: %s%n",
                        retryCount, maxRetries, e.getMessage());

                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION);
                    }
                }
            }
        }

        if (lastException != null) {
            throw lastException; // Rethrow the last exception if we've exhausted all retries
        }
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

    private void sendMessage(String topic, String message) throws MqttException {
        client.publish(topic, message.getBytes(StandardCharsets.UTF_8), 0, false);
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
