package com.hospital;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;

import java.net.URI;

public class ModernDockerStrategy extends DockerClientProviderStrategy {

    private static final String DOCKER_SOCKET = "unix:///var/run/docker.sock";
    private static final String API_VERSION = "1.41";

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        return TransportConfig.builder()
                .dockerHost(URI.create(DOCKER_SOCKET))
                .build();
    }

    @Override
    public String getDescription() {
        return "Docker socket with API " + API_VERSION;
    }

    @Override
    protected boolean isPersistable() {
        return true;
    }

    @Override
    protected int getPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean test() {
        System.out.println("[ModernDockerStrategy] test() called - using API " + API_VERSION);
        try {
            com.github.dockerjava.api.model.Info info = buildClient().infoCmd().exec();
            System.out.println("[ModernDockerStrategy] Connected: " + info.getServerVersion());
            return true;
        } catch (Exception e) {
            System.out.println("[ModernDockerStrategy] Failed: " + e.getMessage());
            return false;
        }
    }

    private DockerClient cachedClient;

    @Override
    public DockerClient getDockerClient() {
        if (cachedClient == null) {
            cachedClient = buildClient();
        }
        return cachedClient;
    }

    private DockerClient buildClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(DOCKER_SOCKET)
                .withApiVersion(API_VERSION)
                .build();

        ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }
}
