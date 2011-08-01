package smartjenkins;

import hudson.Extension;
import hudson.model.Computer;
import hudson.slaves.ComputerListener;

import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;

@Extension
public class SmartJenkinsComputerListener extends ComputerListener {
	private static final SmartJenkinsConfiguration CONF = SmartJenkinsConfiguration.getInstance();
	private static final SmartJenkinsComputerManager COM_MANAGER = SmartJenkinsComputerManager.getInstance();

	@Override
	public synchronized void onConfigurationChange() {
		final Computer[] computers = Jenkins.getInstance().getComputers();
		final List<String> computerNames = new ArrayList<String>();
		for (Computer computer : computers) {
			if (computer.getName().length() == 0) {
				continue;
			}

			if (computer.getNode() == null) {
				onDeleted(computer);
			} else if (!COM_MANAGER.contains(computer)) {
				onCreated(computer);
			}

			computerNames.add(computer.getName());
		}

		for (SmartJenkinsComputer slave : COM_MANAGER.getSlaves()) {
			if (!computerNames.contains(slave.getName())) {
				onDeleted(slave.getComputer());
			}
		}

		CONF.save();
	}

	private void onCreated(Computer computer) {
		COM_MANAGER.add(computer);
	}

	private void onDeleted(Computer computer) {
		CONF.computerConfigurations.remove(computer.getName());
		COM_MANAGER.delete(computer);
	}
}
