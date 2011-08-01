package smartjenkins;

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.triggers.TimerTrigger;
import hudson.triggers.Trigger;
import hudson.triggers.SCMTrigger;
import hudson.util.RunList;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class SmartJenkinsJob {
	private AbstractProject job;
	private SmartJenkinsJobConfiguration configuration;

	public SmartJenkinsJob(AbstractProject job) {
		this.job = job;
		this.configuration = SmartJenkinsConfiguration.getInstance().jobConfigurations.get(job.getName());
		if (this.configuration == null) {
			this.configuration = new SmartJenkinsJobConfiguration();
			SmartJenkinsConfiguration.getInstance().jobConfigurations.put(job.getName(), this.configuration);
		}
	}

	@Exported
	public String getName() {
		return job.getName();
	}

	@Exported
	public AbstractProject getJob() {
		return job;
	}

	@Exported
	public SmartJenkinsJobConfiguration getConfiguration() {
		return configuration;
	}

	@Exported
	public String getTrigger() {
		final StringBuffer sb = new StringBuffer();

		final Trigger scmTrigger = job.getTrigger(SCMTrigger.class);
		final Trigger timerTrigger = job.getTrigger(TimerTrigger.class);
		if (scmTrigger != null) {
			if (timerTrigger != null) {
				sb.append("SCM&nbsp;&nbsp;&nbsp;: ").append(scmTrigger.getSpec());
			} else {
				sb.append("SCM : ").append(scmTrigger.getSpec());
			}
		}

		if (timerTrigger != null) {
			if (scmTrigger != null) {
				sb.append("<BR>");
			}
			sb.append("Timer : ").append(timerTrigger.getSpec());
		}

		return sb.toString();
	}

	public String toEventJSON() {
		final StringBuffer sb = new StringBuffer();

		final RunList<Run> runList = job.getBuilds().newBuilds();
		for (Run run : runList) {
			sb.append("{'start':'").append(SmartJenkinsUtils.formatDate(run.getTime())).append("'")
				.append(",'end':'").append(SmartJenkinsUtils.formatDate(run.getTime().getTime() + (run.getDuration() < 1000 ? 1000 : run.getDuration()))).append("'")
				.append(",'title':'").append(job.getName()).append("(").append(run.getDisplayName()).append(")'")
				.append(",'description':'").append(run.getResult().toString()).append("'")
				.append(",'durationEvent':true},");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}
}
