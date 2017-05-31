package org.example.application;

import javax.inject.Inject;

import org.example.library.Library;
import org.example.library.LibrarySpringConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.guice.module.SpringModule;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Stage;
import com.google.inject.servlet.ServletModule;
import com.netflix.archaius.guice.ArchaiusModule;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.guice.jetty.JettyModule;
import com.netflix.runtime.health.api.Health;
import com.netflix.runtime.health.api.HealthIndicator;
import com.netflix.runtime.health.api.HealthIndicatorCallback;
import com.netflix.runtime.health.guice.HealthModule;
import com.netflix.runtime.health.servlet.HealthStatusServlet;

public class Main {

	public static void main(String[] args) throws Exception {
		LifecycleInjector injector = InjectorBuilder.fromModules(new ApplicationModule())
				.createInjector(Stage.PRODUCTION);
		System.out.println("Application started on port 8080. Check /health endpoint.");
		injector.awaitTermination();
	}
}

class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new ArchaiusModule()); // Configuration
		install(new AwsModule()); // App provides AWS Credentials
		install(new JettyModule()); // Start embedded Jetty
		install(new AppServletModule()); // REST resources
		install(new HealthModule() { // Health Checks
			@Override
			protected void configureHealth() {
				bindAdditionalHealthIndicator().to(AppHealthCheck.class);
			}
		});

		//Install Library written in Spring
//		install(new SpringModule(new AnnotationConfigApplicationContext(LibrarySpringConfig.class)));  //Required befores change
		install(new SpringModule(LibrarySpringConfig.class));	//My proposed approach, works with my current PR
	}

}

class AppServletModule extends ServletModule {
	@Override
	protected void configureServlets() {

		serve("/health").with(new HealthStatusServlet());
	}
}

class AwsModule extends AbstractModule {

	@Override
	protected void configure() {
		// Application provides credentials. Normally we use some smart
		// implementation that is aware of the environment its being run in
		bind(AWSCredentialsProvider.class).toInstance(new AWSStaticCredentialsProvider(new AWSCredentials() {

			public String getAWSSecretKey() {
				return "secret";
			}

			public String getAWSAccessKeyId() {
				return "accessKeyId";
			}
		}));
	}
}

class AppHealthCheck implements HealthIndicator {

	@Inject
	Library library;

	public void check(HealthIndicatorCallback healthCallback) {
		if (library.libraryOk()) // Application's health check makes sure
									// library is "working"
			healthCallback.inform(Health.healthy().build());
		else
			healthCallback.inform(Health.unhealthy().build());
	}
}
