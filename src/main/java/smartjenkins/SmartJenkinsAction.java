package smartjenkins;

import hudson.model.RootAction;
import hudson.model.AbstractModelObject;
import hudson.model.View;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import smartjenkins.SmartJenkinsConstants.EventType;

@ExportedBean
public class SmartJenkinsAction implements RootAction {
	private static final SmartJenkinsConfiguration CONF = SmartJenkinsConfiguration.getInstance();
	private static final SmartJenkinsJobManager JOB_MANAGER = SmartJenkinsJobManager.getInstance();
	private static final SmartJenkinsComputerManager COM_MANAGER = SmartJenkinsComputerManager.getInstance();

	private View view;

	public SmartJenkinsAction() {
		this.view = Jenkins.getInstance().getPrimaryView();
	}

	public SmartJenkinsAction(View view) {
		this.view = view;
	}

	@Exported
	public AbstractModelObject getParentObject() {
		return view;
	}

    @Exported
    public List<SmartJenkinsJob> getJobs() {
    	return JOB_MANAGER.getJobs(view);
    }

    @Exported
	public String getPluginResourcePath() {
		return SmartJenkinsConstants.PLUGIN_RESOURCE_PATH;
	}

    @Exported
    public String getRootPath() {
    	return SmartJenkinsConstants.ROOT_PATH;
    }

    @Exported
    public SmartJenkinsConfiguration getConfiguration() {
    	return CONF;
    }

    @Exported
    public double getTimeZoneOffset() {
    	return Calendar.getInstance().getTimeZone().getRawOffset() / 60.0 / 60.0 / 1000.0;
    }

    @Exported
    public List<SmartJenkinsComputer> getSlaves() {
    	return COM_MANAGER.getSlaves();
    }

    @Exported
    public String getJobEventsJSON() {
    	return JOB_MANAGER.getEventsJSON(view);
    }

	public String getIconFileName() {
		return "clock.png";
	}

	public String getDisplayName() {
		return "Smart Jenkins";
	}

	public String getUrlName() {
		return "smart-jenkins";
	}

	public synchronized final void doEnable(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
		CONF.enable = !CONF.enable;
		CONF.timeSlot.enable(CONF.enable);

		CONF.save();
		rsp.forwardToPreviousPage(req);
	}

	public synchronized final void doFontSize(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
		final String fontSize = req.getParameter("size");
		if (fontSize != null && fontSize.matches("[SML]")) {
			CONF.fontSize = fontSize;
		}

		CONF.save();
		rsp.forwardToPreviousPage(req);
	}

	public synchronized final void doChangeTab(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
		final String tabStr = req.getParameter("tab");
		if (tabStr != null && tabStr.length() > 0) {
			final int tab = Integer.parseInt(tabStr);
			if (tab >= 0 && tab <= 1 && CONF.tabType != tab) {
				CONF.tabType = tab;
				CONF.save();
			}
		}
		rsp.forwardToPreviousPage(req);
	}

	public synchronized final void doConfigureTimeSlot(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {		
		final String timeSlotString = req.getParameter(SmartJenkinsConstants.TIME_SLOT);
		if (CONF.timeSlot.setTimeSlot(timeSlotString, true)) {
			CONF.save();
		}
		rsp.forwardToPreviousPage(req);
	}

	public synchronized final void doConfigure(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {		
		if (CONF.tabType == 0) {
			for (SmartJenkinsJob job : JOB_MANAGER.getJobs(view)) {
				String name = job.getJob().getName();
				String enable = req.getParameter(name + SmartJenkinsConstants.ENABLE_SUFFIX);
				String schedule = req.getParameter(name + SmartJenkinsConstants.SCHEDULE_SUFFIX);
				if (job.getConfiguration().update(enable, schedule)) {
					final long next = job.getConfiguration().nextScheduled;
					if (next > 0) {
						SmartJenkinsTimeLine.removeEvent(name, EventType.scheduled);
						SmartJenkinsTimeLine.addEvent(name, next, -1, name, "Scheduled", EventType.scheduled);
						SmartJenkinsScheduler.schedule(job.getJob(), next);
					} else {
						SmartJenkinsTimeLine.removeEvent(name, EventType.scheduled);
						SmartJenkinsScheduler.remove(job.getJob());
					}
				}
			}

			CONF.save();
			rsp.forwardToPreviousPage(req);
		} else if (CONF.tabType == 1) {
			for (SmartJenkinsComputer node : SmartJenkinsComputerManager.getInstance().getSlaves()) {
				String name = node.getName();
				String enable = req.getParameter(name + SmartJenkinsConstants.ENABLE_SUFFIX);
				if (enable == null && node.getConfiguration().enable) {
					node.getConfiguration().enable = false;
				}
			}
			CONF.save();
			rsp.forwardToPreviousPage(req);
		}
	}
}
