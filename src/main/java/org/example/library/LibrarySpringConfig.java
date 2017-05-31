package org.example.library;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentialsProvider;

@Configuration
public class LibrarySpringConfig {

	@Bean
	public Library library(final AWSCredentialsProvider credentialsProvider) {
		return new Library() {
			public boolean libraryOk() {
				return credentialsProvider.getCredentials().getAWSSecretKey().equals("secret");
			}
		};
	}

	@Bean
	public ApplicationListener<ApplicationEvent> eventListener(final Library lib) {
		return new ApplicationListener<ApplicationEvent>() {
			public void onApplicationEvent(ApplicationEvent event) {
				//initialize or invoke some method on Library
				lib.libraryOk();
			}
		};
	}
}
