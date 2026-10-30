package ua.vetclinic.api;

import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

/** Перехоплює події журналу аудиту (іменований логер AUDIT, AD-24.1). */
public final class AuditLogCapture {

	public static final String AUDIT_LOGGER_NAME = "AUDIT";

	private final Logger logger = (Logger) LoggerFactory.getLogger(AUDIT_LOGGER_NAME);

	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

	public void start() {
		appender.start();
		logger.addAppender(appender);
	}

	public void stop() {
		logger.detachAppender(appender);
		appender.stop();
	}

	public List<String> lines() {
		return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
	}
}
