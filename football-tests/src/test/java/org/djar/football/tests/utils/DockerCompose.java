package org.djar.football.tests.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class DockerCompose {

    private static final Logger logger = LoggerFactory.getLogger(DockerCompose.class);

    private static final String UP_COMMAND = "docker-compose up -d";
    private static final String DOWN_COMMAND = "docker-compose down";
    private static final String DOCKER_COMPOSE_YML = "docker-compose.yml";

    private final Map<String, String> healthCheckUrls = new LinkedHashMap<>();
    private long healthCheckTimeout;

    private boolean createdContainers;
    private boolean startupFailed;

    public DockerCompose addHealthCheck(String url, String expectedBody) {
        healthCheckUrls.put(url, expectedBody);
        return this;
    }

    public DockerCompose healthCheckTimeout(long timeout, TimeUnit unit) {
        this.healthCheckTimeout = unit.toMillis(timeout);
        return this;
    }

    public void up() {
        Path workingDir = Paths.get(".").toAbsolutePath().normalize();
        Path dockerComposeFile = workingDir.getParent().resolve(DOCKER_COMPOSE_YML);

        if (!Files.exists(dockerComposeFile)) {
            throw new RuntimeException(
                    "No " + DOCKER_COMPOSE_YML + " file inside parent directory, current working dir:" + workingDir);
        }
        long time = System.currentTimeMillis();

        try {
            String output = execProcess(UP_COMMAND, 60);
            createdContainers = output.contains("Creating network");
            waitUntilServicesAreReady();
            logger.info("Started services in {} s", Math.round((System.currentTimeMillis() - time) / 1000d));
        } catch (RuntimeException e) {
            startupFailed = true;
            throw e;
        } catch (Throwable e) {
            startupFailed = true;
            throw new RuntimeException("Docker Compose startup error", e);
        }
    }

    @AfterClass
    public void down() {
        if (startupFailed) {
            logger.warn("Not shutting down Docker Compose - check the error");
            return;
        }
        if (createdContainers) {
            execProcess(DOWN_COMMAND, 60);
        } else {
            logger.warn("Not shutting down Docker Compose - was already up when the playAMatch started");
        }
    }

    private void waitUntilServicesAreReady() {
        long endTime = System.currentTimeMillis() + healthCheckTimeout;
        Map<String, String> urls = new LinkedHashMap<>(healthCheckUrls);
        logger.info("Waiting for services to be ready (with timeout {} s)...", healthCheckTimeout / 1000);
        RestTemplate rest = new RestTemplate();

        while (true) {
            for (Iterator<Map.Entry<String, String>> urlIterator = urls.entrySet().iterator(); urlIterator.hasNext();) {
                Map.Entry<String, String> entry = urlIterator.next();
                String url = entry.getKey();
                String expectedResponse = entry.getValue();

                try {
                    ResponseEntity<String> result = rest.getForEntity(url, String.class);

                    if (result.getStatusCode().is2xxSuccessful()) {
                        if (result.getBody().trim().equals(expectedResponse)) {
                            logger.debug("{} is UP", url);
                            urlIterator.remove();
                            continue;
                        }
                    }
                    logger.trace("{} responded: {} {}", url, result.getStatusCode(), result.getBody());
                } catch (RestClientException e) {
                    logger.trace("{} responded: {}", url, e);
                }
                if (System.currentTimeMillis() > endTime) {
                    throw new RuntimeException("No response from " + urls.keySet());
                }
            }
            if (urls.isEmpty()) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupted();
            }
        }
    }

    private String execProcess(String command, long timeout) {
        logger.debug(command);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CommandLine commandline = CommandLine.parse(command);
        DefaultExecutor exec = new DefaultExecutor();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        exec.setStreamHandler(streamHandler);
        int exitCode;

        try {
            exitCode = exec.execute(commandline);
        } catch (IOException e) {
            throw new RuntimeException("Unable to execute " + command, e);
        }
        if (exitCode != 0) {
            throw new RuntimeException(command + " exited with code " + exitCode);
        }
        String output = outputStream.toString();
        logger.debug(System.lineSeparator() + output);
        return output;
    }
}
