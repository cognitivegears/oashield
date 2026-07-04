package com.oashield.openapi.integration.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Manager for an OWASP ModSecurity v3 (libmodsecurity + nginx) container.
 *
 * Uses the owasp/modsecurity-crs nginx image with the CRS neutralized: our own
 * nginx server config template replaces the default, loading only the generated
 * oashield rules. Requests are proxied to a local upstream because nginx `return`
 * answers before ModSecurity's request-body phase runs.
 *
 * Note: the image currently publishes linux/386 only; on other architectures pull
 * it explicitly with `docker pull --platform linux/386 owasp/modsecurity-crs:nginx`.
 */
public class ModSecurityContainerManager implements WafContainerManager {
    private static final String MODSECURITY_IMAGE = "owasp/modsecurity-crs:nginx";
    private static final int MODSECURITY_PORT = 8080;

    private final GenericContainer<?> container;

    /**
     * Constructor using the default image.
     *
     * @param rulesDirectory The absolute path to the directory containing the rules (main.conf + *Api.conf)
     */
    public ModSecurityContainerManager(String rulesDirectory) {
        this(rulesDirectory, MODSECURITY_IMAGE);
    }

    /**
     * Constructor with custom image.
     *
     * @param rulesDirectory The absolute path to the directory containing the rules
     * @param containerImage Docker image name for the container
     */
    public ModSecurityContainerManager(String rulesDirectory, String containerImage) {
        this.container = new GenericContainer<>(DockerImageName.parse(containerImage))
                .withCreateContainerCmdModifier(cmd -> cmd.withPlatform("linux/386"))
                .withExposedPorts(MODSECURITY_PORT)
                .withFileSystemBind(rulesDirectory, "/etc/modsecurity.d/oashield")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("container/modsec3-default.conf.template"),
                        "/etc/nginx/templates/conf.d/default.conf.template")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("container/modsec3-modsecurity.conf.template"),
                        "/etc/nginx/templates/conf.d/modsecurity.conf.template")
                // A WAF that answers 403 at / (undefined endpoint, default deny) is healthy.
                .waitingFor(Wait.forHttp("/").forStatusCode(200).forStatusCode(403));
    }

    @Override
    public String start() {
        container.start();
        return getBaseUrl();
    }

    @Override
    public void stop() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    @Override
    public String getBaseUrl() {
        return String.format("http://%s:%d", container.getHost(), container.getMappedPort(MODSECURITY_PORT));
    }
}
