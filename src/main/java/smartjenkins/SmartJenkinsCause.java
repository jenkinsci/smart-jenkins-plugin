package smartjenkins;

import hudson.model.Cause;

public class SmartJenkinsCause extends Cause {
	private long time;

	public SmartJenkinsCause(long time) {
		this.time = time;
	}

	@Override
	public String getShortDescription() {
		return "Smart-Jenkins Cause";
	}

	public long getTime() {
		return time;
	}
}
