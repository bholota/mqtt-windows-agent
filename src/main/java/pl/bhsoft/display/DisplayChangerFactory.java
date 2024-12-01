package pl.bhsoft.display;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DisplayChangerFactory {

    public static DisplayChanger create() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return new DisplayChangerFactory().createWindowsDisplayChanger();
        } else if (os.contains("mac")) {
            return new DisplayChangerFactory().createMacosDisplayChanger();
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return new DisplayChangerFactory().createLinuxDisplayChanger();
        } else {
            throw new IllegalCallerException("The operating system is not recognized: " + os);
        }
    }

    private DisplayChanger createLinuxDisplayChanger() {
        throw new IllegalCallerException("Linux is unsupported");
    }
    // debug only
    private DisplayChanger createMacosDisplayChanger() {
        return new DisplayChanger() {
            @Override
            public void switchToExternal() {
                executeCommand("say external display");
            }

            @Override
            public void switchToInternal() {
                executeCommand("say internal display");
            }
        };
    }

    private DisplayChanger createWindowsDisplayChanger() {
        return new DisplayChanger() {
            @Override
            public void switchToExternal() {
                executeCommand("DisplaySwitch.exe /external");
            }

            @Override
            public void switchToInternal() {
                executeCommand("DisplaySwitch.exe /internal");
            }
        };
    }

    private int executeCommand(String command) {
        try {
            // Create a ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));

            // Start the process
            Process process = processBuilder.start();

            // Capture the output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            return exitCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
