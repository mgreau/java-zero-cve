package com.mgreau.maven.plugin.patcher;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CommandExecutor {
    private final Log log;

    public CommandExecutor(Log log) {
        this.log = log;
    }

    public void executeCommand(final List<String> command, final File workingDirectory) throws MojoExecutionException {
        try {
            final ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDirectory);
            pb.redirectErrorStream(true);
            
            final Process process = pb.start();
            
            // Read the output
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
            }
            
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new MojoExecutionException(
                    "Command " + String.join(" ", command) + " failed with exit code: " + exitCode
                );
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Failed to execute command: " + String.join(" ", command), e);
        }
    }
}