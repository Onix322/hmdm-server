package com.hmdm;

import com.google.inject.Injector;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

/**
 * A configuration for HMDM server application.
 *
 * @author isv
 */
@OpenAPIDefinition(
    info = @Info(title = "Headwind MDM API", version = "0.0.2", description = "API Documentation"))
public class HMDMApplication extends ResourceConfig {

  /** Constructs new <code>HMDMApplication</code> instance and initializes the Guice-HK2 bridge. */
  @Inject
  public HMDMApplication(final ServiceLocator serviceLocator) {
    packages("com.hmdm");
    register(MultiPartFeature.class);
    register(
        new ContainerLifecycleListener() {
          public void onStartup(Container container) {
            ServletContainer servletContainer = (ServletContainer) container;
            GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
            GuiceIntoHK2Bridge guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge.class);
            Injector injector =
                (Injector)
                    servletContainer.getServletContext().getAttribute(Injector.class.getName());
            guiceBridge.bridgeGuiceInjector(injector);
          }

          public void onReload(Container container) {}

          public void onShutdown(Container container) {}
        });

    register(OpenApiResource.class);
    register(AcceptHeaderOpenApiResource.class);
  }
}
