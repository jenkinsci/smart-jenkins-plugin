package smartjenkins;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class SmartJenkinsUtils {
	private static final DateFormat df = DateFormat.getDateTimeInstance();
	private static final Calendar calendar = Calendar.getInstance();

	public static String formatDate(Date date) {
		return df.format(date);
	}

	public static String formatDate(long time) {
		if (time > 0) {
			return formatDate(new Date(time));
		} else {
			return "-";
		}
	}

	public static Date parseDateString(String dateString) {
		try {
			if (!dateString.contains(SmartJenkinsConstants.COLON)) {
				return df.parse(dateString + " 00:00:00");
			} else {
				return df.parse(dateString);
			}
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the day in a week. (1-7:Monday-Sunday)
	 * @param time a time in milliseconds
	 * @return what the day the specified time is in a week.
	 */
	public synchronized static int getDayOfWeek(long time) {
		calendar.setTimeInMillis(time);
		int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
		return day == 0 ? 7 : day;
	}
}
