package utils.general;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Helper functions for handling dates.
 * 
 * @author Henryk Zschuppan
 */
public class DateUtil {

	public static final String DATE_PATTERN              = "yyyy.MM.dd";
	/** The date pattern that is used for conversion. Change as you wish. */
	public static final String DATE_TIME_PATTERN         = "dd MMM yyyy / HH:mm";
	/** The date pattern that is used for conversion. Change as you wish. */
	public static final String DATE_TIME_SECONDS_PATTERN = "dd MMM yyyy / HH:mm:ss";

	/** The date pattern that is used for conversion. Change as you wish. */

	public enum FORMAT {
		DATE(DATE_PATTERN),
		DATE_TIME(DATE_TIME_PATTERN),
		DATE_TIME_SECONDS(DATE_TIME_SECONDS_PATTERN),
		TIME_ONLY("HH:mm:ss");

		String pattern = "";

		FORMAT(String datePattern) {
			this.pattern = datePattern;
		}

		public String getPattern() {
			return pattern;
		}
	}

	/** The date formatter. */
	private static final DateTimeFormatter DATE_FORMATTER      = DateTimeFormatter.ofPattern(DATE_PATTERN).withLocale(Locale.ENGLISH);
	/** The date formatter. */
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN).withLocale(Locale.ENGLISH);

	/**
	 * Returns the given date as a well formatted String. The above defined {@link DateUtil#DATE_PATTERN} is used.
	 * 
	 * @param date the date to be returned as a string
	 * @return formatted string
	 */
	public static String format(LocalDate date) {
		if (date == null) {
			return null;
		}
		return DATE_FORMATTER.format(date);
	}

	/**
	 * Returns the given date as a well formatted String. The above defined {@link DateUtil#DATE_PATTERN} is used.
	 * 
	 * @param date the date to be returned as a string
	 * @return formatted string
	 */
	public static String format(FORMAT format, LocalDateTime stamp) {
		if (stamp == null) {
			return null;
		}
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format.getPattern()).withLocale(Locale.ENGLISH);
		return formatter.format(stamp);
	}

	/**
	 * Returns the given date as a well formatted String. The above defined {@link DateUtil#DATE_PATTERN} is used.
	 * 
	 * @param date the date to be returned as a string
	 * @return formatted string
	 */
	public static String format(LocalDateTime stamp) {
		if (stamp == null) {
			return null;
		}
		return DATE_TIME_FORMATTER.format(stamp);
	}

	/**
	 * Converts a String in the format of the defined {@link DateUtil#DATETIME_PATTERN} to a {@link LocalDateTime} object. Returns null if the String could not be converted.
	 * 
	 * @param datetimeString the date as String
	 * @return the date object or null if it could not be converted
	 */
	public static LocalDateTime parse(String datetimeString) {
		try {
			return DATE_TIME_FORMATTER.parse(datetimeString, LocalDateTime::from);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Checks the String whether it is a valid date.
	 * 
	 * @param dateString
	 * @return true if the String is a valid date
	 */
	public static boolean validDate(String dateString) {
		// Try to parse the String.
		return DateUtil.parse(dateString) != null;
	}

	/** private constructor hidden **/
	private DateUtil() {
	}
}