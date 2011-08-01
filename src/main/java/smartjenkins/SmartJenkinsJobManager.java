package smartjenkins;

import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jenkins.model.Jenkins;

public class SmartJenkinsJobManager {
	private static final SmartJenkinsJobManager INSTANCE = new SmartJenkinsJobManager();
	private static Map<String, SmartJenkinsJob> jobs;

	private SmartJenkinsJobManager() {
		jobs = new HashMap<String, SmartJenkinsJob>();
	}
 
	public List<SmartJenkinsJob> getJobs(View view) {
		final List<SmartJenkinsJob> ret = new ArrayList<SmartJenkinsJob>();
		for (TopLevelItem item : view.getItems()) {
			SmartJenkinsJob job = jobs.get(item.getName());
			if (job != null && !SmartJenkinsConstants.ControllerJobName.contains(job.getName())) {
				ret.add(job);
			}
		}

		return ret;
	}

	public SmartJenkinsJob getJob(String name) {
		return jobs.get(name);
	}

	public String getEventsJSON(View view) {
		final List<SmartJenkinsJob> jobList = getJobs(view);
		final StringBuffer sb = new StringBuffer();

    	sb.append("({'events':[");
    	SmartJenkinsJob job;
    	String json;
    	for (final Iterator<SmartJenkinsJob> it = jobList.iterator(); it.hasNext();) {
    		job = it.next();
    		json = job.toEventJSON();
    		if (json != null && json.length() > 0) {
    			sb.append(json).append(SmartJenkinsConstants.COMMA);
    		}
    		json = SmartJenkinsTimeLine.getEventJSON(job.getName());
    		if (json != null && json.length() > 0) {
    			sb.append(json).append(SmartJenkinsConstants.COMMA);
    		}
    	}
    	if (sb.charAt(sb.length() - 1) == ',') {
    		sb.deleteCharAt(sb.length() - 1);
    	}
    	sb.append("]})");

    	return sb.toString();
	}

	public void add(AbstractProject job) {
		jobs.put(job.getName(), new SmartJenkinsJob(job));
	}

	public void rename(AbstractProject job, String oldName, String newName) {
		if (jobs.containsKey(oldName)) {
			jobs.put(newName, jobs.get(oldName));
			jobs.remove(oldName);
		}
	}

	public void delete(AbstractProject job) {
		jobs.remove(job.getName());
	}

	public static SmartJenkinsJobManager getInstance() {
		return INSTANCE;
	}
	
	public static void loadJobs() {
		final List<TopLevelItem> items = Jenkins.getInstance().getItems();
		for (TopLevelItem item : items) {
			if (AbstractProject.class.isAssignableFrom(item.getClass())) {
				jobs.put(item.getName(), new SmartJenkinsJob((AbstractProject) item));
			}
		}
	}
}
