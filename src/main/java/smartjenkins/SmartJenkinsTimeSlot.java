package smartjenkins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.kohsuke.stapler.export.Exported;

public class SmartJenkinsTimeSlot {
	public static final int START = 0;
	public static final int END = 1;

	private static final int TIME_ZONE_OFFSET = TimeZone.getDefault().getRawOffset();
	private static final int MAX_TIME = 24 * 60 * 60 * 1000;
	private static final int ALL_DAYS_OF_WEEK = 0x007f;

	private static final SmartJenkinsTimeSlot instance = new SmartJenkinsTimeSlot();

	private String timeSlotString;
	private Vector<TimeSlotItem> timeSlot;

	private SmartJenkinsTimeSlot() {
		timeSlotString = SmartJenkinsConstants.NULL_STRING;
		timeSlot = new Vector<TimeSlotItem>();
	}

	public static SmartJenkinsTimeSlot getInstance() {
		return instance;
	}

	public void enable(boolean enable) {
		if (enable) {
			setTimeSlot(timeSlotString, false);
		} else {
			timeSlot = null;
		}
	}

	public boolean setTimeSlot(String timeSlotString, boolean checkSame) {
		if (timeSlotString == null) {
			return false;
		}

		final String trimedTimeSlotString = timeSlotString.replaceAll(SmartJenkinsConstants.SP, SmartJenkinsConstants.NULL_STRING);
		if (checkSame && trimedTimeSlotString.equals(this.timeSlotString)) {
			return false;
		}

		this.timeSlotString = trimedTimeSlotString;
		if (timeSlot != null) {
			timeSlot.clear();
		} else {
			timeSlot = new Vector<TimeSlotItem>();
		}
		if (this.timeSlotString != null && this.timeSlotString.length() > 0) {
			final String[] items = split(this.timeSlotString, SmartJenkinsConstants.COMMA);
			for (int i = 0; i < items.length; i++) {
				timeSlot.add(new TimeSlotItem(items[i]));
			}
		}

		return true;
	}

	@Exported
	public String getDecoratorJSON() {
		final long currentTime = System.currentTimeMillis();
		return toDecoratorJSON(
				currentTime - 7 * SmartJenkinsConstants.DAY_IN_MILLISECONDS
				, currentTime + 7 * SmartJenkinsConstants.DAY_IN_MILLISECONDS
				, "#B7FF00"
				, 30
				, null
				, null
				, "t-highlight1"
				, false);
	}

	private String toDecoratorJSON(long fromDate, long toDate, String color, int opacity, String startLabel, String endLabel, String cssClass, boolean inFront) {
		final StringBuffer sb = new StringBuffer();

		sb.append("[");
		if (timeSlot != null) {
			final long from = getStartOfDay(fromDate);
			final long to = getStartOfDay(toDate);
			String json;
			for (long i = from; i <= to; i += SmartJenkinsConstants.DAY_IN_MILLISECONDS) {
				for (TimeSlotItem item : timeSlot) {
					json = item.toDecoratorJSON(i, color, opacity, startLabel, endLabel, cssClass, inFront);
					if (json != null && json.length() > 0) {
						sb.append(json).append(",");
					}
				}
			}
		}
		sb.append("new Timeline.PointHighlightDecorator({date:'")
			.append(SmartJenkinsUtils.formatDate(new Date()))
			.append("',color:'#FF0000',opacity:50,width:1})]");

		return sb.toString();
	}

	@Exported
	public String getCurrentTime() {
		return SmartJenkinsUtils.formatDate(new Date());
	}

	@Exported
	public String getValue() {
		return timeSlotString;
	}

	public long next(int type) {
		long next = -1;
		
		final long currentTime = System.currentTimeMillis();
		for (TimeSlotItem item : timeSlot) {
			long tmp = item.next2(type, currentTime);
			if (tmp > 0) {
				if (next < 0) {
					next = tmp;
				} else {
					next = tmp < next ? tmp : next;
				}
			}
		}

		return next;
	}

	public boolean canBuild(long time) {
		for (TimeSlotItem item : timeSlot) {
			if (item.check(time)) {
				return true;
			}
		}

		return false;
	}

	public String toString() {
		return timeSlotString;
	}

	private static int[] parseTimeSlotString(String timeSlotString) {
		final int[] timeSlot = new int[2];
		
		final String[] items = timeSlotString.trim().split(SmartJenkinsConstants.HYPHEN, 2);
		if (items.length == 1) {
			timeSlot[START] = parseTimeString(items[0]);
			timeSlot[START] = timeSlot[START] < 0 ? 0 : timeSlot[START];
			timeSlot[END] = timeSlot[START] + SmartJenkinsConstants.HOUR_IN_MILLISECONDS;
			timeSlot[END] = timeSlot[END] > MAX_TIME ? MAX_TIME : timeSlot[END];
		} else if (items.length == 2) {
			timeSlot[START] = parseTimeString(items[0]);
			timeSlot[START] = timeSlot[START] < 0 ? 0 : timeSlot[START];
			timeSlot[END] = parseTimeString(items[1]);
			timeSlot[END] = timeSlot[END] < 0 ? MAX_TIME : timeSlot[END];
		}

		return timeSlot;
	}

	private static int parseTimeString(String timeString) {
		if (timeString.length() == 0) {
			return -1;
		}

		int time = 0;
		int milliseconds = 60 * 60 * 1000;
		final String[] items = split(timeString, SmartJenkinsConstants.COLON);
		for (int i = 0; i < items.length; i++) {
			time += milliseconds * Integer.parseInt(items[i]);
			milliseconds /= 60;
		}

		return time > MAX_TIME ? MAX_TIME : time;
	}
	
	private static int[][] merge(int[][] timeSlot) {
		final List<int[]> list = new ArrayList<int[]>(timeSlot.length + 1);
		
		final int[] start = new int[2 * timeSlot.length];
		final int[] end = new int[2 * timeSlot.length];
		for (int i = 0; i < start.length; i++) start[i] = end [i] = -1;
		int idx = 0;
		for (int i = 0; i < timeSlot.length; i++) {
			start[idx] = timeSlot[i][START];
			end[idx] = timeSlot[i][END];
			idx++;
			if (timeSlot[i][START] > timeSlot[i][END]) {
				start[idx] = 0;
				end[idx] = MAX_TIME;
				idx++;
			}
		}
		Arrays.sort(start);
		Arrays.sort(end);

		int startIdx = 0;
		int endIdx = 0;
		while (startIdx < start.length && start[startIdx] < 0) startIdx++;
		while (endIdx < end.length && end[endIdx] < 0) endIdx++;
		if (startIdx >= start.length || endIdx >= end.length) {
			return new int[][]{{0, MAX_TIME}};
		}

		int listIdx = 0;
		list.add(new int[]{start[startIdx++], end[endIdx++]});
		while (startIdx < start.length && endIdx < end.length) {
			if (start[startIdx] <= end[endIdx - 1]) {
				list.get(listIdx)[END] = end[endIdx];
				startIdx++;
				endIdx++;
			} else {
				listIdx++;
				list.add(new int[]{start[startIdx++], end[endIdx++]});
			}
		}

		return list.toArray(new int[0][2]);
	}

	private static long getStartOfDay(long time) {
		return time - (time + TIME_ZONE_OFFSET) % SmartJenkinsConstants.DAY_IN_MILLISECONDS;
	}

	private static String[] split(String str, String sep) {
		if (str == null) {
			return null;
		}

		if (!str.contains(sep)) {
			return new String[]{str};
		}

		final int index = str.indexOf(sep);
		return new String[]{str.substring(0, index), str.substring(index + 1, str.length())};
	}

	private static class TimeSlotItem {
		private Vector<long[]> dates;
		private int dayOfWeek;
		private Vector<int[]> timetable;

		TimeSlotItem(String cron) {
			dates = new Vector<long[]>();
			dayOfWeek = 0;
			timetable = new Vector<int[]>();

			final String[] items = split(cron, SmartJenkinsConstants.AT);
			setDateAndWeek(items[0]);
			setTime(items[1]);
		}

		public boolean check(long time) {
			if (checkDate(time)) {
				final long startOfDay = getStartOfDay(time);
				final int offset = (int) (time - startOfDay);
				for (int[] t : timetable) {
					if (offset >= t[START] && offset <= t[END]) {
						return true;
					}
				}
			}

			return false;
		}
 
		public long next2(int type, long time) {
			if (timetable.size() == 0
					&& timetable.get(0)[START] == 0
					&& timetable.get(0)[END] == MAX_TIME) {
				return 0;
			}

			final long startOfDay = getStartOfDay(time);
			long next = next(type, time);
			int day = 1;
			while (next == -1 && day <= 7) {
				next = next(type, startOfDay + day * MAX_TIME);
				day++;
			}

			if (type == END && next % MAX_TIME == 0) {
				next = next(type, next + 1);
			}

			return next;
		}

		public long next(int type, long time) {
			if (checkDate(time)) {
				final long startOfDay = getStartOfDay(time);
				final int offset = (int) (time - startOfDay);
				for (int[] t : timetable) {
					if (t[type] >= offset) {
						return startOfDay + t[START];
					}
				}
			}

			return -1;
		}

		public String toDecoratorJSON(long time, String color, int opacity, String startLabel, String endLabel, String cssClass, boolean inFront) {
			if (!checkDate(time)) {
				return null;
			}

			final StringBuffer sb = new StringBuffer();
			final boolean isColorSet = color != null && color.length() > 0;
			final boolean isOpacitySet = opacity >= 0;
			final boolean isStartLabelSet = startLabel != null && startLabel.length() > 0;
			final boolean isEndLabelSet = endLabel != null && endLabel.length() > 0;
			final boolean isCssClassSet = cssClass != null && cssClass.length() > 0;

			for (int[] t : timetable) {
				sb.append("new Timeline.SpanHighlightDecorator({");
				sb.append("startDate:'").append(SmartJenkinsUtils.formatDate(time + t[START])).append("',");
				sb.append("endDate:'").append(SmartJenkinsUtils.formatDate(time + t[END])).append("'");
				if (isColorSet) {
					sb.append(",color:'" + color + "'");
				}
				if (isOpacitySet) {
					sb.append(",opacity:" + opacity + "");
				}
				if (isStartLabelSet) {
					sb.append(",startLabel:'" + startLabel + "'");
				}
				if (isEndLabelSet) {
					sb.append(",endLabel:'" + endLabel + "'");
				}
				if (isCssClassSet) {
					sb.append(",cssClass:'" + cssClass + "'");
				}
				if (inFront) {
					sb.append(",inFront:true");
				}
				sb.append("}),");
			}
			if (sb.length() > 0) {
				sb.deleteCharAt(sb.length() - 1);
			}

			return sb.toString();
		}

		private void setDateAndWeek(String cron) {
			final String[] items = split(cron, SmartJenkinsConstants.PIPE);
			for (String item : items) {
				if (SmartJenkinsConstants.ASTERISK.equals(item)) {
					dates.add(new long[]{0, 0});
					dayOfWeek = ALL_DAYS_OF_WEEK;
					return;
				} else if (item.contains(SmartJenkinsConstants.SLASH)) {
					final String[] dateItems = split(item, SmartJenkinsConstants.HYPHEN);
					final long startDate = SmartJenkinsUtils.parseDateString(dateItems[0]).getTime();
					if (dateItems.length == 2) {
						final long endDate = SmartJenkinsUtils.parseDateString(dateItems[1]).getTime();
						dates.add(new long[]{startDate, endDate + MAX_TIME});
					} else {
						dates.add(new long[]{startDate, startDate + MAX_TIME});
					}
				} else {
					final String[] weekdayItems = split(item, SmartJenkinsConstants.HYPHEN);
					final int startWeekday = Integer.parseInt(weekdayItems[0]);
					if (weekdayItems.length == 2) {
						final int endWeekday = Integer.parseInt(weekdayItems[1]);
						for (int i = startWeekday; i <= endWeekday; i++) {
							dayOfWeek |= 1 << (i == 0 ? 6 : i - 1);
						}
					} else {
						dayOfWeek |= 1 << (startWeekday == 0 ? 6 : startWeekday - 1);
					}
				}
			}
		}

		private void setTime(String cron) {
			if (SmartJenkinsConstants.ASTERISK.equals(cron)) {
				timetable.add(new int[]{0, MAX_TIME});
				return;
			}

			final String[] timeItems = split(cron, SmartJenkinsConstants.PIPE);
			int[][] tmp = new int[timeItems.length][2];
			for (int i = 0; i < timeItems.length; i++) {
				tmp[i] = parseTimeSlotString(timeItems[i]);
			}
			tmp = merge(tmp);
			for (int[] timeItem : tmp) {
				timetable.add(timeItem);
			}
		}

		private boolean checkDate(long time) {
			final int day = SmartJenkinsUtils.getDayOfWeek(time);
			if (((dayOfWeek >> (day - 1)) & 1) != 0) {
				return true;
			}

			for (long[] date : dates) {
				if (time >= date[START] && time <= date[END]) {
					return true;
				}
			}

			return false;
		}
	}
}
