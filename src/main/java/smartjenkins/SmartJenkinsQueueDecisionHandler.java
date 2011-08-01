package smartjenkins;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue.QueueDecisionHandler;
import hudson.model.Queue.Task;

import java.util.List;

import smartjenkins.SmartJenkinsConstants.EventType;

@Extension
public class SmartJenkinsQueueDecisionHandler extends QueueDecisionHandler {
	private static final SmartJenkinsConfiguration CONF = SmartJenkinsConfiguration.getInstance();
	private static final SmartJenkinsJobManager JOB_MANAGER = SmartJenkinsJobManager.getInstance();

	@Override
	public boolean shouldSchedule(Task p, List<Action> actions) {
		if (!CONF.enable) {
			return true;
		}

		final String name = p.getName();
		final SmartJenkinsJobConfiguration jobConf = CONF.jobConfigurations.get(name);
		if (jobConf == null) {
			return true;
		}

		for (Action action : actions) {
			if (CauseAction.class.isAssignableFrom(action.getClass())) {
				List<Cause> causes = ((CauseAction) action).getCauses();
				for (Cause cause : causes) {
					if (SmartJenkinsCause.class.isAssignableFrom(cause.getClass())) {
						final long time = ((SmartJenkinsCause) cause).getTime();
						SmartJenkinsTimeLine.removeEvent(name, time, -1, EventType.scheduled);
						jobConf.lastScheduled = time;
						jobConf.nextScheduled = -1;
						CONF.save();
						return true;
					}
				}
			}
		}

		final SmartJenkinsTimeSlot timeSlot = CONF.timeSlot;
		final long currentTime = System.currentTimeMillis();
		final long timeSlotNextStart = timeSlot.next(SmartJenkinsTimeSlot.START);
		final long jobNextStart = jobConf.nextScheduled;
		long nextStart = -1;
		if (timeSlotNextStart > 0) {
			nextStart = jobNextStart > 0 ? Math.min(timeSlotNextStart, jobNextStart) : timeSlotNextStart;
		} else {
			nextStart = jobNextStart > 0 ? jobNextStart : -1;
		}

		if (timeSlot.canBuild(currentTime)) {
			return true;
		}

		if (jobConf.enable) {
			if (Math.abs(jobConf.nextScheduled - currentTime) <= SmartJenkinsConstants.TIME_ERROR) {
				jobConf.lastScheduled = currentTime;
				jobConf.nextScheduled = -1;
				CONF.save();

				SmartJenkinsTimeLine.removeEvent(name, EventType.scheduled);

				return true;
			} else {				
				boolean block = true;
				for (SmartJenkinsComputer computer : SmartJenkinsComputerManager.getInstance().getSlaves()) {
					if (!computer.getConfiguration().enable) {
						if (canTake(computer.getComputer().getNode(), p)) {
							block = false;
							break;
						}
					}
				}
				if ("ruby-benchmark".equals(p.getName())) {
					block = true;
				} else if ("ruby-test".equals(p.getName())) {
					block = false;
				}

				if (block) {
					jobConf.lastBlocked = currentTime;
					jobConf.nextScheduled = nextStart;
					CONF.save();

					SmartJenkinsTimeLine.addEvent(name, currentTime, -1, name, "Blocked", EventType.blocked);
					SmartJenkinsTimeLine.addEvent(name, jobConf.nextScheduled, -1, name, "Scheduled", EventType.scheduled);

					return false;
				} else {
					return true;
				}
			}
		} else {
			return true;
		}

		// 2011/7/12
//		final String name = p.getName();
//		final ScheduledJobConfiguration jobConf = conf.jobConfigurations.get(name);
//		if (jobConf == null) {
//			return true;
//		}
//		final SmartJenkinsTimeSlot timeSlot = conf.timeSlot;
//		final long currentTime = System.currentTimeMillis();
//		final long timeSlotNextStart = timeSlot.next(SmartJenkinsTimeSlot.START);
//		final long jobNextStart = jobConf.schedule.next();
//		long nextStart = -1;
//		if (timeSlotNextStart > 0) {
//			nextStart = jobNextStart > 0 ? Math.min(timeSlotNextStart, jobNextStart) : timeSlotNextStart;
//		} else {
//			nextStart = jobNextStart > 0 ? jobNextStart : -1;
//		}
//
//		if (timeSlot.canBuild(currentTime)) {
//			jobConf.nextScheduled = timeSlotNextStart > 0 ? Math.min(jobConf.schedule.next(), timeSlotNextStart) : timeSlotNextStart;
//			conf.save();
//
//			SmartJenkinsTimeLine.addEvent(name, jobConf.nextScheduled, -1, name, "Scheduled", EventType.scheduled);
//
//			return true;
//		}
//
//		if (jobConf.enable) {
//			if (jobConf.schedule.match(currentTime)) {
//				jobConf.lastScheduled = currentTime;
//				jobConf.nextScheduled = -1;
//				conf.save();
//
////				SmartJenkinsTimeLine.addEvent(name, jobConf.nextScheduled, -1, name, "Scheduled", EventType.scheduled);
//
//				return true;
//			} else {
//				jobConf.lastBlocked = currentTime;
//				jobConf.nextScheduled = nextStart;
//				conf.save();
//
//				SmartJenkinsTimeLine.addEvent(name, currentTime, -1, name, "Blocked", EventType.blocked);
//				SmartJenkinsTimeLine.addEvent(name, jobConf.nextScheduled, -1, name, "Scheduled", EventType.scheduled);
//
//				return false;
//			}
//		} else {
//			return true;
//		}
	}

	private boolean canTake(Node node, Task p) {
		final SmartJenkinsJob job = JOB_MANAGER.getJob(p.getName());
		
		System.out.println("computer tied job");
		for (AbstractProject j : node.toComputer().getTiedJobs()) {
			System.out.println(j.getName());
		}
		System.out.println("------");
		
		System.out.println("label string : " + job.getJob().getAssignedLabelString());
		
		final Label label = job.getJob().getAssignedLabel();
		if (label != null) {
			System.out.println("label" + label.getExpression() + "   contains : " + label.contains(node));
		}
		
//		
//		
//System.out.println(p.getClass().getName());
//		Label l = p.getAssignedLabel();
//if (l != null) {
//System.out.println("label" + l.getExpression() + "   contains : " + l.contains(node));
//}
//        if(l!=null && !l.contains(node))
//            return false;   // the task needs to be executed on label that this node doesn't have.
//
//        if(l==null && node.getMode()== Mode.EXCLUSIVE)
//            return false;   // this node is reserved for tasks that are tied to it
//System.out.println("node mode : " + node.getMode());
//        // Check each NodeProperty to see whether they object to this node
//        // taking the task
//        for (NodeProperty prop: node.getNodeProperties()) {
//            CauseOfBlockage c = prop.canTake(p);
//            return c == null;
//        }

        // Looks like we can take the task
        return true;
	}
}
