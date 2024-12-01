package pl.bhsoft;

public class Main {

    public static void main(String[] args) {
        MqttDisplayClient displayClient = new MqttDisplayClient(new Args(args));
        displayClient.run();
    }
}