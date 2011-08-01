package smartjenkins;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class SmartJenkinsJobConfiguration {
	@Exported
	public boolean enable;
	public long lastBlocked;
	public long lastScheduled;
	public long nextScheduled;

	public SmartJenkinsJobConfiguration() {
		enable = true;
		lastBlocked = 0;
		lastScheduled = 0;
		nextScheduled = 0;
	}

	@Exported
	public String getLastBlocked() {
		return SmartJenkinsUtils.formatDate(lastBlocked);
	}

	@Exported
	public String getLastScheduled() {
		return SmartJenkinsUtils.formatDate(lastScheduled);
	}

	@Exported
	public String getNextScheduled() {
		final String date = SmartJenkinsUtils.formatDate(nextScheduled);
		if (SmartJenkinsConstants.HYPHEN.equals(date)) {
			return SmartJenkinsConstants.NULL_STRING;
		} else {
			return date;
		}
	}

	public boolean update(String enableString, String scheduleString) {
		boolean updated = false;
		
		if (scheduleString != null) {
			if (enableString == null) {
				updated = updated || enable;
				enable = false;
			} else {
				updated = updated || !enable;
				enable = true;
			}

			boolean scheduleUpdated = false;
			if (scheduleString != null && scheduleString.length() > 0) {
				final long time = SmartJenkinsUtils.parseDateString(scheduleString).getTime();
				if (time != nextScheduled) {
					nextScheduled = time;
					scheduleUpdated = true;
				}
			} else {
				scheduleUpdated = nextScheduled == -1 ? false : true;
				nextScheduled = -1;
			}
			updated = updated || scheduleUpdated;
		}

		return updated;
	}
}
