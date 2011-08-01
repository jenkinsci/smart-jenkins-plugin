package smartjenkins;

import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Saveable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.export.Exported;

public class SmartJenkinsConfiguration implements Saveable {
	private static final Logger LOGGER = Logger.getLogger(SmartJenkinsConfiguration.class.getName());
	private static final File CONFIGURE_XML_FILE = new File(Jenkins.getInstance().getRootDir(), "smart-jenkins.xml");
	private static SmartJenkinsConfiguration instance = null;
	static {
		load();
	}

	@Exported
	public boolean enable;
	@Exported
	public SmartJenkinsTimeSlot timeSlot;
	@Exported
	public String fontSize;
	@Exported
	public Map<String, SmartJenkinsJobConfiguration> jobConfigurations;
	@Exported
	public int tabType;
	@Exported
	public Map<String, SmartJenkinsComputerConfiguration> computerConfigurations;

	private SmartJenkinsConfiguration() {
		enable = false;
		timeSlot = SmartJenkinsTimeSlot.getInstance();
		fontSize = SmartJenkinsConstants.FONT_SIZE_SMALL;
		jobConfigurations = new HashMap<String, SmartJenkinsJobConfiguration>();
		tabType = 0;
		computerConfigurations = new HashMap<String, SmartJenkinsComputerConfiguration>();
	}

	public static SmartJenkinsConfiguration getInstance() {
		return instance;
	}

	public synchronized void save() {
		if (BulkChange.contains(this)) {
			return;
		}

        try {
            final XmlFile configureFile = new XmlFile(Jenkins.XSTREAM, CONFIGURE_XML_FILE);
            configureFile.write(this);
        } catch (IOException e) {
        	LOGGER.log(Level.WARNING, "FAILED - Write to configure file \"" + CONFIGURE_XML_FILE + "\".", e);
        }
	}

	private static synchronized void load() {
		try {
            if (CONFIGURE_XML_FILE.exists()) {
            	instance = (SmartJenkinsConfiguration) new XmlFile(Jenkins.XSTREAM, CONFIGURE_XML_FILE).read();
            	if (instance.jobConfigurations == null) {
            		instance.jobConfigurations = new HashMap<String, SmartJenkinsJobConfiguration>();
            	}
            	if (instance.computerConfigurations == null) {
            		instance.computerConfigurations = new HashMap<String, SmartJenkinsComputerConfiguration>();
            	}
            }
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "FAILED - Read from configure file \"" + CONFIGURE_XML_FILE + "\".", e);
		}

		if (instance == null) {
			instance = new SmartJenkinsConfiguration();
		}
	}
}
