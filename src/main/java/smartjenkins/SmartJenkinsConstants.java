package smartjenkins;

import hudson.model.Hudson;
import jenkins.model.Jenkins;


public class SmartJenkinsConstants {
	// String Constants
	public static final String COMMA = ",";
	public static final String HYPHEN = "-";
	public static final String COLON = ":";
	public static final String SP = " ";
	public static final String PIPE = "|";
	public static final String ASTERISK = "*";
	public static final String SLASH = "/";
	public static final String AT = "@";
	public static final String NULL_STRING = "";

	public static final int HOUR_IN_MILLISECONDS = 60 * 60 * 1000;
	public static final int DAY_IN_MILLISECONDS = 24 * HOUR_IN_MILLISECONDS;

	// Request Parameter Name
	public static final String TIME_SLOT = "timeSlot";
	public static final String ENABLE_SUFFIX = "_enable";
	public static final String SCHEDULE_SUFFIX = "_schedule";

	// Font size
	public static final String FONT_SIZE_SMALL = "S";
	public static final String FONT_SIZE_MEDIUM = "M";
	public static final String FONT_SIZE_LARGE = "L";

	// Path
	public static final String ROOT_DIR = Jenkins.getInstance().getRootDir().getAbsolutePath();
	public static final String ROOT_PATH = Jenkins.getInstance().getRootUrl();
	public static final String PLUGIN_RESOURCE_PATH = Jenkins.getInstance().getRootUrl() + "plugin/smart-jenkins/";

	// Timeline event type
	public static enum EventType {
		normal,
		blocked,
		scheduled;
	}

	// Job for shutdown/wol slaves
	public static enum ControllerJobName {
		shutdown_windows("shutdown-win"),
		shutdown_linux("shutdown-linux"),
		wol_windows("wol-win"),
		wol_linux("wol-linux");

		private String name;
		private ControllerJobName(String name) {
			this.name = name;
		}

		public static boolean contains(String name) {
			for (ControllerJobName cjn : values()) {
				if (cjn.name.equals(name)) {
					return true;
				}
			}

			return false;
		}
	}

	// Time error in milliseconds
	public static final long TIME_ERROR = 5000;
}
