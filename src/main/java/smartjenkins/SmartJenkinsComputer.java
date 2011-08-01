package smartjenkins;

import hudson.model.Computer;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class SmartJenkinsComputer {
	private Computer computer;
	private SmartJenkinsComputerConfiguration configuration;

	public SmartJenkinsComputer(Computer computer) {
		this.computer = computer;
		final String name = computer.getName();
		this.configuration = SmartJenkinsConfiguration.getInstance().computerConfigurations.get(name);
		if (this.configuration == null) {
			this.configuration = new SmartJenkinsComputerConfiguration();
			SmartJenkinsConfiguration.getInstance().computerConfigurations.put(name, this.configuration);
		}
	}

	@Exported
	public String getName() {
		return computer.getName();
	}

	@Exported
	public SmartJenkinsComputerConfiguration getConfiguration() {
		return configuration;
	}

	public Computer getComputer() {
		return computer;
	}
}
