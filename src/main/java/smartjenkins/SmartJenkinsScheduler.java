package smartjenkins;

import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.triggers.SafeTimerTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import jenkins.model.Jenkins;
import controller.Controller;

public class SmartJenkinsScheduler {
	private static final Timer timer = new Timer(true);
	private static final Map<String, BuildTask> buildTasks = new HashMap<String, BuildTask>();
	private static final Map<String, ControllerTask> controllerTasks = new HashMap<String, ControllerTask>();
	private static final SmartJenkinsConfiguration conf = SmartJenkinsConfiguration.getInstance();

	static {
		timer.schedule(new CheckTask(System.currentTimeMillis()), 30 * 1000, 30 * 1000);
	}

	public static void schedule(AbstractProject job, long time) {
		if (time > 0) {
			BuildTask task = buildTasks.get(job.getName());
			if (task != null) {
				task.cancel();
			}
			task = new BuildTask(job);
			buildTasks.put(job.getName(), task);
			timer.schedule(task, new Date(time));
		}
	}

	public static void remove(AbstractProject job) {
		final BuildTask task = buildTasks.get(job.getName());
		if (task != null) {
			task.cancel();
			buildTasks.remove(job.getName());
		}
	}

	public static void run(AbstractProject job) {
		job.scheduleBuild(0, new SmartJenkinsCause(System.currentTimeMillis()));
	}

	public static void startup(String slaveName, long time) {
		try {
			ControllerTask task = controllerTasks.get(slaveName);
			if (task != null) {
				task.cancel();
			}
			task = new ControllerTask(ControllerTask.Type.on, new String[]{slaveName});
			controllerTasks.put(slaveName, task);
			timer.schedule(task, new Date(time));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void shutdown(String[] slaveNames, long time) {
		final StringBuffer sb = new StringBuffer();
		for (String name : slaveNames) {
			sb.append(name).append(" ");
		}
		try {
			ControllerTask task = controllerTasks.get(sb.toString());
			if (task != null) {
				task.cancel();
			}
			task = new ControllerTask(ControllerTask.Type.off, slaveNames);
			controllerTasks.put(sb.toString(), task);
			timer.schedule(task, new Date(time));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void onJobNameChanged(String oldName, String newName) {
		if (buildTasks.containsKey(oldName)) {
			buildTasks.put(newName, buildTasks.get(oldName));
		}
	}

	public static void onJobDeleted(String jobName) {
		if (buildTasks.containsKey(jobName)) {
			buildTasks.get(jobName).cancel();
			buildTasks.remove(jobName);
		}
	}

	public static void onSlaveNameChanged(String oldName, String newName) {
		if (controllerTasks.containsKey(oldName)) {
			controllerTasks.put(newName, controllerTasks.get(oldName));
		}
	}

	public static void onSlaveDeleted(String name) {
		if (controllerTasks.containsKey(name)) {
			controllerTasks.get(name).cancel();
			controllerTasks.remove(name);
		}
	}

	private static class ControllerTask extends SafeTimerTask {
		private static enum Type {
			on,
			off;
		}
		private Type type;
		private String[] names;

		public ControllerTask(Type type, String[] names) {
			this.type = type;
			this.names = names;
			
			final StringBuffer sb = new StringBuffer();
			for (String name : names) {
				sb.append(name).append(" ");
			}
		}

		@Override
		protected void doRun() throws Exception {
			switch (type) {
			case on:
				Controller.getWakeonlan(names[0]);
				break;
			case off:
				Controller.setShutdown(names);
				break;
			}
		}
	}

	private static class CheckTask extends SafeTimerTask {
		private boolean ok;

		CheckTask(long time) {
			ok = false;
		}

		@Override
		protected void doRun() throws Exception {
			final long currentTime = System.currentTimeMillis();
			final boolean canBuild = conf.timeSlot.canBuild(currentTime);

			if (ok != canBuild) {
				if (ok) {
					final Computer[] computers = Jenkins.getInstance().getComputers();
					final List<String> nameList = new ArrayList<String>();
					for (int i = 1; i < computers.length; i++) {
						String name = computers[i].getName();
						if (computers[i].isOnline()
								&& conf.computerConfigurations.containsKey(name)
								&& conf.computerConfigurations.get(name).enable
								&& computers[i].isIdle()) {
							nameList.add(name);
						}
					}
					final String[] names = nameList.toArray(new String[0]);
					if (names.length > 0) {
						shutdown(names, currentTime);
					}
				} else {
					final Computer[] computers = Jenkins.getInstance().getComputers();
					for (int i = 1; i < computers.length; i++) {
						String name = computers[i].getName();
						if (computers[i].isOffline()
								&& conf.computerConfigurations.containsKey(name)
								&& conf.computerConfigurations.get(name).enable) {
							startup(computers[i].getName(), currentTime);
						}
					}
				}
				ok = canBuild;
			}
		}
	}

	private static class BuildTask extends SafeTimerTask {
		private AbstractProject project;

		BuildTask(AbstractProject project) {
			this.project = project;
		}

		@Override
		protected void doRun() throws Exception {
			project.scheduleBuild(0, new SmartJenkinsCause(System.currentTimeMillis()));
		}
	}
}
