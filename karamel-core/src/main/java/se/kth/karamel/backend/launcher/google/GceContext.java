package se.kth.karamel.backend.launcher.google;

import java.util.Arrays;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.domain.Credentials;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.googlecomputeengine.GoogleComputeEngineApi;
import org.jclouds.googlecomputeengine.compute.config.GoogleComputeEngineServiceContextModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;

/**
 *
 * @author Hooman
 */
public class GceContext {
    private String projectName;
    Credentials credentials;
    private final ComputeService computeService;
    private final GoogleComputeEngineApi gceApi;

    public GceContext(Credentials credentials) {
        ComputeServiceContext context = ContextBuilder.newBuilder("google-compute-engine")
                .modules(Arrays.asList(new SshjSshClientModule(), new EnterpriseConfigurationModule(), new SLF4JLoggingModule()))
                .credentials(credentials.identity, credentials.credential)
                .buildView(ComputeServiceContext.class);
        computeService = context.getComputeService();
        gceApi = context.unwrapApi(GoogleComputeEngineApi.class);
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public ComputeService getComputeService() {
        return computeService;
    }

    public GoogleComputeEngineApi getGceApi() {
        return gceApi;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
