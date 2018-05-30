package org.djar.football.tests.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class DockerCompose {

    private static final Logger logger = LoggerFactory.getLogger(DockerCompose.class);

    private static final String UP_COMMAND = "docker-compose up -d";
    private static final String DOWN_COMMAND = "docker-compose down";

    private final Map<String, String> healthCheckUrls = new LinkedHashMap<>();

    private boolean createdContainers;
    private boolean startupFailed;

    public DockerCompose addHealthCheck(String serviceUrl, String expectedBody) {
        healthCheckUrls.put(serviceUrl, expectedBody);
        return this;
    }

    public void up() {
        long time = System.currentTimeMillis();

        try {
            String output = execProcess(UP_COMMAND);
            // containers are not started
            createdContainers = output.contains("Creating network");
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
            execProcess(DOWN_COMMAND);
        } else {
            logger.warn("Not shutting down Docker Compose - was already up when the test started");
        }
    }

    public void waitUntilServicesAreAvailable(long timeout, TimeUnit unit) {
        long healthCheckTimeout = unit.toMillis(timeout);
        RestTemplate rest = restTemplate(healthCheckTimeout);
        Map<String, String> urls = new LinkedHashMap<>(healthCheckUrls);

        long startTime = System.currentTimeMillis();
        long maxTime = startTime + healthCheckTimeout;
        logger.info("Waiting for services to be ready (with timeout {} s)...", healthCheckTimeout / 1000);

        while (true) {
            for (Iterator<Map.Entry<String, String>> urlIterator = urls.entrySet().iterator(); urlIterator.hasNext();) {
                Map.Entry<String, String> entry = urlIterator.next();

                if (serviceAvailable(rest, entry.getKey(), entry.getValue())) {
                    urlIterator.remove();
                    continue;
                }
                if (System.currentTimeMillis() > maxTime) {
                    startupFailed = true;
                    throw new RuntimeException("Timeout waiting for services. No response from: " + urls.keySet());
                }
            }
            if (urls.isEmpty()) {
                break;
            }
            sleep(1000);
        }
        logger.info("Started services in {} s", Math.round((System.currentTimeMillis() - startTime) / 1000d));
    }

    private boolean serviceAvailable(RestTemplate rest, String url, String expectedResponse) {
        try {
            ResponseEntity<String> result = rest.getForEntity(url, String.class);

            if (result.getStatusCode().is2xxSuccessful()) {
                if (result.getBody().trim().matches(expectedResponse)) {
                    logger.debug("{} is UP", url);
                    return true;
                }
            }
            logger.trace("{} responded: {} {}", url, result.getStatusCode(), result.getBody());
        } catch (RestClientException e) {
            logger.trace("{} responded: {}", url, e);
        }
        return false;
    }

    private RestTemplate restTemplate(long healthCheckTimeout) {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(2000);
        clientHttpRequestFactory.setConnectionRequestTimeout(2000);
        return new RestTemplate(clientHttpRequestFactory);
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupted();
        }
    }

    private String execProcess(String command) {
        logger.debug(command);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CommandLine commandline = CommandLine.parse(command);
        DefaultExecutor exec = new DefaultExecutor();
        exec.setStreamHandler(new PumpStreamHandler(outputStream));
        int exitCode;

        try {
            exitCode = exec.execute(commandline);
        } catch (IOException e) {
            throw new RuntimeException("Unable to execute " + command + ": " + outputStream, e);
        }
        if (exitCode != 0) {
            throw new RuntimeException(command + " exited with code " + exitCode + ", " + outputStream);
        }
        String output = outputStream.toString();
        logger.debug(System.lineSeparator() + output);
        return output;
    }
}
