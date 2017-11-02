package eu.nimble.service.dashboard.services;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.core.events.SystemStartupEvent;

@ApplicationScoped
public class StartupConfigurator {
	@Inject
	ConfigurationService config;

	public StartupConfigurator() {
		// TODO Auto-generated constructor stub
	}
	public void onSystemStartup(@Observes SystemStartupEvent event) {
		
		String[] databaseVal = new String [] {"database.type","database.url","database.user","database.password" };
		String[] hostVal = new String [] {"kiwi.path","kiwi.context","kiwi.host" };
		
		if ( processEnvironmentVariables(databaseVal)) {
			config.setBooleanConfiguration("kiwi.setup.database", Boolean.TRUE);
		}
		if ( processEnvironmentVariables(hostVal)) {
			config.setBooleanConfiguration("kiwi.setup.host", Boolean.TRUE);
		}
		/*
			security.enabled
			database.type
			database.url
			database.user
			database.password
			kiwi.setup.database
			kiwi.path
			kiwi.context
			kiwi.host
			kiwi.setup.host
		*/
	}
	

	private boolean processEnvironmentVariables(String[] variables) {
		boolean variablesPresent = true;
		String[] values = new String[variables.length];
		
		for ( int i = 0; i < variables.length; i++ ) {
			// try to get from environment
			values[i] = System.getenv(variables[i]);
		
			if ( values[i]  == null ) {
				values[i] = System.getProperty(variables[i]);
			}
			
			if ( values[i] == null ) {
				variablesPresent = false;
			}
		}
		if (variablesPresent) {
			for ( int i = 0; i < variables.length; i++) {
				config.setConfiguration(variables[i], values[i]);
			}
			return true;
		}
		return false;
	}

}
