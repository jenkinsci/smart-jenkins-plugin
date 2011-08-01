package smartjenkins;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class SmartJenkinsComputerConfiguration {
	@Exported
	public boolean enable;

	public SmartJenkinsComputerConfiguration() {
		enable = true;
	}
}
