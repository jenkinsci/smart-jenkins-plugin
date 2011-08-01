package smartjenkins;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.listeners.ItemListener;

import java.util.Map;

import jenkins.model.Jenkins;
import smartjenkins.SmartJenkinsConstants.EventType;

@Extension
public class SmartJenkinsItemListener extends ItemListener {
	private static final SmartJenkinsConfiguration CONF = SmartJenkinsConfiguration.getInstance();
	private static final SmartJenkinsJobManager JOB_MANAGER = SmartJenkinsJobManager.getInstance();

	@Override
	public void onLoaded() {
		SmartJenkinsJobManager.loadJobs();

		if (!CONF.enable) {
			return;
		}

		final Jenkins jenkins = Jenkins.getInstance();
		final long currentTime = System.currentTimeMillis();
		final long timeSlotNextStart = CONF.timeSlot.next(SmartJenkinsTimeSlot.START);
		final long timeSlotNextEnd = CONF.timeSlot.next(SmartJenkinsTimeSlot.END);
		for (Map.Entry<String, SmartJenkinsJobConfiguration> entry : CONF.jobConfigurations.entrySet()) {
			String name = entry.getKey();
			SmartJenkinsJobConfiguration jobConf = entry.getValue();
			if (!jobConf.enable) {
				continue;
			}

			if (jobConf.nextScheduled >= currentTime) {
				SmartJenkinsScheduler.schedule((AbstractProject) jenkins.getItem(name), jobConf.nextScheduled);
				SmartJenkinsTimeLine.addEvent(name, jobConf.nextScheduled, -1, name, "Scheduled", EventType.scheduled);
			} else {
				jobConf.nextScheduled = -1;
			}
//			jobConf.schedule.remove(currentTime);
//			long jobNext = jobConf.schedule.next();
//			if (jobNext > 0) {
//				jobConf.nextScheduled = jobNext;
//				SmartJenkinsScheduler.schedule((AbstractProject) hudson.getItem(name), jobConf.nextScheduled);
//				SmartJenkinsTimeLine.addEvent(name, jobConf.nextScheduled, -1, name, "Scheduled", EventType.scheduled);
//			} else {
//				jobConf.nextScheduled = -1;
//			}
		}
		CONF.save();

//		// startup slaves
//		long startSlaveTime = timeSlotNextStart;
//		if (timeSlotNextStart > timeSlotNextEnd) {
//			startSlaveTime = 0;
//		}
//		final List<Node> nodes = Jenkins.getInstance().getNodes();
//		for (Node node : nodes) {
//			String name = node.getNodeName();
//			if (!SmartJenkinsConstants.NULL_STRING.equals(name)) {
//				SmartJenkinsScheduler.startup(name, startSlaveTime);
//				try {
//					Thread.sleep(2000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//		}
	}

	@Override
	public void onCreated(Item item) {
		if (AbstractProject.class.isAssignableFrom(item.getClass())) {
			CONF.jobConfigurations.put(item.getName(), new SmartJenkinsJobConfiguration());
			CONF.save();

			JOB_MANAGER.add((AbstractProject) item);
		}
	}

	@Override
	public void onDeleted(Item item) {
		if (AbstractProject.class.isAssignableFrom(item.getClass())) {
			SmartJenkinsScheduler.onJobDeleted(item.getName());
			CONF.jobConfigurations.remove(item.getName());
			CONF.save();

			JOB_MANAGER.delete((AbstractProject) item);
		}
	}

	@Override
	public void onRenamed(Item item, String oldName, String newName) {
		if (AbstractProject.class.isAssignableFrom(item.getClass())) {
			SmartJenkinsScheduler.onJobNameChanged(oldName, newName);
			CONF.jobConfigurations.put(newName, CONF.jobConfigurations.get(oldName));
			CONF.jobConfigurations.remove(oldName);
			CONF.save();

			JOB_MANAGER.rename((AbstractProject) item, oldName, newName);
		}
	}
}
