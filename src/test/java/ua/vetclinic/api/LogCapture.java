package ua.vetclinic.api;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.slf4j.LoggerFactory;

/** Перехоплює події іменованого логера: за замовчуванням журналу аудиту AUDIT (AD-24.1). */
public final class LogCapture {

  public static final String AUDIT_LOGGER_NAME = "AUDIT";

  private final Logger logger;

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  /** Створює перехоплювач журналу аудиту {@code AUDIT}. */
  public LogCapture() {
    this(AUDIT_LOGGER_NAME);
  }

  /**
   * Створює перехоплювач указаного логера, наприклад {@code HTTP}.
   *
   * @param loggerName ім'я логера
   */
  public LogCapture(String loggerName) {
    this.logger = (Logger) LoggerFactory.getLogger(loggerName);
  }

  /** Починає збирати події; викликається перед дією, що перевіряється. */
  public void start() {
    appender.start();
    logger.addAppender(appender);
  }

  /** Від'єднує перехоплювач від логера; викликається після тесту. */
  public void stop() {
    logger.detachAppender(appender);
    appender.stop();
  }

  /**
   * Повертає зібрані події як відформатовані рядки.
   *
   * @return рядки журналу в порядку запису
   */
  public List<String> lines() {
    return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }
}
