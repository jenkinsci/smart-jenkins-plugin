package smartjenkins;

import hudson.model.Computer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;

public class SmartJenkinsComputerManager {
	private static final SmartJenkinsComputerManager INSTANCE = new SmartJenkinsComputerManager();

	private Computer master;
	private Map<String, SmartJenkinsComputer> slaves;

	private SmartJenkinsComputerManager() {
		slaves = new HashMap<String, SmartJenkinsComputer>();
		for (Computer computer : Jenkins.getInstance().getComputers()) {
			if (computer.getName().length() == 0) {
				master = computer;
			} else {
				slaves.put(computer.getName(), new SmartJenkinsComputer(computer));
			}
		}
	}

	public Computer getMaster() {
		return master;
	}

	public List<SmartJenkinsComputer> getSlaves() {
		final List<SmartJenkinsComputer> slaveList = new ArrayList<SmartJenkinsComputer>();
		for (SmartJenkinsComputer slave : slaves.values()) {
			slaveList.add(slave);
		}
		return slaveList;
	}

	public SmartJenkinsComputer getSlave(String name) {
		return slaves.get(name);
	}

	public boolean contains(Computer computer) {
		return slaves.containsKey(computer.getName());
	}

	public void add(Computer computer) {
		slaves.put(computer.getName(), new SmartJenkinsComputer(computer));
	}

	public void delete(Computer computer) {
		slaves.remove(computer.getName());
	}

	public static SmartJenkinsComputerManager getInstance() {
		return INSTANCE;
	}
}
