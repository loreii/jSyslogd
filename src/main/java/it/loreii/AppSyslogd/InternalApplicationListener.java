package it.loreii.AppSyslogd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.context.ApplicationListener;

public class InternalApplicationListener implements ApplicationListener<ExitCodeEvent> {

	private static final Logger log = LoggerFactory.getLogger(InternalApplicationListener.class);

	@Override
	public void onApplicationEvent(ExitCodeEvent event) {

		log.info(">> " + event);
		System.exit(-1);

	}
}