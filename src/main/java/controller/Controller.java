package controller;

import hudson.Extension;
import hudson.matrix.MatrixProject;
import hudson.model.RootAction;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.widgets.Widget;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import smartjenkins.SmartJenkinsConstants;
import smartjenkins.SmartJenkinsScheduler;

@Extension
public class Controller extends Widget implements RootAction {
	private static final Jenkins JENKINS = Jenkins.getInstance();
	private static final String XML_PATH = SmartJenkinsConstants.ROOT_DIR + "/slavestaus.xml";
	public static List<String> Mac;
	static {
		createFile(XML_PATH);
	}

	public static boolean createFile(String filePath) {
		boolean isDone = false;
		final File file = new File(filePath);
		if (!file.exists()) {
			try {
				new XmlModify().BuildXMLDoc(filePath);
				isDone = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return isDone;
	}

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return "Controller";
	}

	public String getUrlName() {
		return "controller";
	}

	public String getRootUrl() {
		return SmartJenkinsConstants.ROOT_PATH;
	}

	public static Computer[] get_all() {
		return JENKINS.getComputers();
	}

	public static void CreateJob() throws IOException {
		String job1 = "shutdown-linux";
		String job2 = "shutdown-win";
		String job3 = "wol-linux";
		String job4 = "wol-win";
		String filepath = "";
		String url = JENKINS.getRootUrl();
		int flag1 = 0, flag2 = 0, flag3 = 0, flag4 = 0;
		List<TopLevelItem> item = JENKINS.getItems();
		MatrixProject s = new MatrixProject("testonjob");
		TopLevelItemDescriptor type = (TopLevelItemDescriptor) s
				.getDescriptor();
		for (int i = 0; i < item.size(); i++) {
			Job obj = (Job) item.get(i);
			if (obj.getName().equals(job1))
				flag1 = 1;
			else if (obj.getName().equals(job2))
				flag2 = 1;
			else if (obj.getName().equals(job3))
				flag3 = 1;
			else if (obj.getName().equals(job4))
				flag4 = 1;
		}
		if (flag1 == 0) {
			Job obj = (Job) JENKINS.createProject(type, job1);
			filepath = obj.getConfigFile().toString();
			new XmlModify().createxml(filepath, "shutdown -h now", job1);
			new PostTest().testPost(url + "job/" + job1 + "/config.xml", obj
					.getConfigFile().toString());
		}
		if (flag2 == 0) {
			Job obj = (Job) JENKINS.createProject(type, job2);
			filepath = obj.getConfigFile().toString();
			new XmlModify().createxml(filepath, "shutdown -s -t 1", job2);
			new PostTest().testPost(url + "job/" + job2 + "/config.xml", obj
					.getConfigFile().toString());
		}
		if (flag3 == 0) {
			Job obj = (Job) JENKINS.createProject(type, job3);
			filepath = obj.getConfigFile().toString();
			new XmlModify().createxml(filepath, "", job3);
			new PostTest().testPost(url + "job/" + job3 + "/config.xml", obj
					.getConfigFile().toString());
		}
		if (flag4 == 0) {
			Job obj = (Job) JENKINS.createProject(type, job4);
			filepath = obj.getConfigFile().toString();
			new XmlModify().createxml(filepath, "", job4);
			new PostTest().testPost(url + "job/" + job4 + "/config.xml", obj
					.getConfigFile().toString());
		}
	}

	public void doRefresh(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException, InterruptedException {
		String result = "";
		List<String> ip =getLog();
		String[] temp = new String[JENKINS.getComputers().length - 1];
		for (int i = 0; i < JENKINS.getComputers().length - 1; i++) {
			if (JENKINS.getComputers()[i + 1].isOnline()) {
				result = result + "Off,";
				result = result + ip.get(i)+",";
				result = result + Mac.get(i)+",";
			} else {
				result = result + "On,";
				result = result + ip.get(i)+",";
				result = result + Mac.get(i)+",";
			}
		}

		PrintWriter out = rsp.getWriter();
		out.write(result);
		out.flush();
		out.close();
	}

	public List<String> getState() {
		List<String> state = new LinkedList<String>();
		for (int i = 1; i < JENKINS.getComputers().length; i++) {
			if (JENKINS.getComputers()[i].isOnline())
				state.add("Off");
			else
				state.add("On");
		}
		getipmac();
		return state;
	}

	public static List<String> getslaveNames() {
		List<String> slaveNames = new LinkedList<String>();
		for (int i = 1; i < JENKINS.getComputers().length; i++)
			slaveNames.add(JENKINS.getComputers()[i].getName());
		try {
			CreateJob();
		} catch (Exception e) {
			System.out.println("Error - Create Job");
		}

		return slaveNames;
	}

	
	public List<String> getLog() throws IOException, InterruptedException {
		String[][] slave = new String[get_all().length - 1][4];
		int flag[];
		String[][] sla;
		int m = get_all().length;
		Mac = new LinkedList<String>();
		List<String> Log = new LinkedList<String>();
		String Logall[] = new String[m];
		sla = new String[m - 1][4];
		flag = new int[m];
		for (int i = 0; i < m; i++)
			flag[i] = 0;
		for (int i = 1; i < m; i++) {
			if (get_all()[i].isOnline() && flag[i] == 0) {
				flag[i] = 1;
				Logall[i] = get_all()[i].getLog();
				if (Logall[i].contains("Windows"))
					slave[i - 1][3] = "windows";
				else
					slave[i - 1][3] = "linux";
				Logall[i] = Logall[i].substring(Logall[i].indexOf("/") + 1,
						Logall[i].indexOf("<") - 1);
				slave[i - 1][0] = get_all()[i].getName();
				slave[i - 1][1] = Logall[i];
				slave[i - 1][2] = new WakeUpServlet().getmac(Logall[i]);
				sla[i - 1][2] = slave[i - 1][2];
			} else if (get_all()[i].isOffline()) {
				Logall[i] = "Slave is offline";
				sla[i - 1][2] = "no mac";
			}
			Log.add(Logall[i]);
		}
		String[] mac = new String[m - 1];
		for (int i = 0; i < m - 1; i++) {
			mac[i] = sla[i][2];
			Mac.add(mac[i]);
		}
		return Log;
	}
	public static String[][] getipmac() {
		final Computer[] computers = JENKINS.getComputers();
		final String[][] slave = new String[getslaveNames().size()][4];
		final String[][] m = new XmlModify().read(XML_PATH);
		int i = m.length;
		if (slave.length >= 1) {
			if (slave.length < m.length) {
				for (int s = 0; s < slave.length; s++) {
					slave[s][0] = computers[s + 1]
							.getName();
					int j = 0;
					while (j < slave.length) {
						if (slave[s][0].equals(m[j][0]))
							break;
						j++;
					}
					slave[s][1] = m[j][1];
					slave[s][2] = m[j][2];
					slave[s][3] = m[j][3];
				}
			}
			if (slave.length >= m.length) {
				for (int s = 0; s < i; s++) {
					slave[s][0] = m[s][0];
					slave[s][1] = m[s][1];
					slave[s][2] = m[s][2];
					slave[s][3] = m[s][3];
				}
				while (i < slave.length) {
					slave[i][0] = computers[i + 1]
							.getName();
					slave[i][1] = "no ip";
					slave[i][2] = "no mac";
					slave[i][3] = "";
					i++;
				}
			}
		}
		new XmlModify().saveState(XML_PATH, slave);
		try {
			getMac();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return slave;
	}

	@Exported
	public String getPluginResourcePath() {
		return SmartJenkinsConstants.PLUGIN_RESOURCE_PATH;
	}

	public void doOff(StaplerRequest req, StaplerResponse rsp)
			throws ServletException, IOException, InterruptedException {
		final Computer[] computers = JENKINS.getComputers();
		String result = null;
		int count = computers.length;
		// int c = 1;
		String url = JENKINS.getRootUrl();
		String[][] slave = getipmac();
		// String temp=null;

		int c = Integer.parseInt(req.getParameter("id"));
		String temp = req.getParameter("name");

		String s = computers[c].getName();
		if (temp.equals("On")) {
			String mac = "";
			String ip = "";
			for (int i = 0; i < slave.length; i++) {
				if (s.equals(slave[i][0])) {
					ip = slave[i][1];
					mac = slave[i][2];
					break;
				}
			}
			if (mac == null || ip == null || "".equals(mac) || "".equals(ip)) {
				result = "Please make the slave Online at least once";
				// getResult(result);
				PrintWriter out = rsp.getWriter();
				out.write(result); 
				out.flush();
				out.close();
				// rsp.forwardToPreviousPage(req);
			} else {
				getWakeonlan(null, mac, ip, false);
				// rsp.forwardToPreviousPage(req);
				result = "Now waking up the slave";
				// getResult(result);
				// Thread.sleep(1000);
				// rsp.setContentType("text/html;charset=utf-8");
				PrintWriter out = rsp.getWriter();
				out.write(result); 
				out.flush();
				out.close();
				// rsp.forwardToPreviousPage(req);
			}
		} else {
			String os = "";
			for (int i = 0; i < slave.length; i++) {
				if (s.equals(slave[i][0])) {
					os = slave[i][3];
					break;
				}
			}

			List<TopLevelItem> item = JENKINS.getItems();
			AbstractProject obj = (AbstractProject) item.get(0);
			if (os.equals("linux")) {
				for (int i = 0; i < item.size(); i++) {
					obj = (AbstractProject) item.get(i);
					System.out.println(obj.getName());
					if (obj.getName().equals("shutdown-linux"))
						break;
				}
			} else {
				for (int i = 0; i < item.size(); i++) {
					obj = (AbstractProject) item.get(i);
					if (obj.getName().equals("shutdown-win"))
						break;
				}
			}
			String[] sla = new String[1];
			sla[0] = s;
			new XmlModify().setSlave(obj.getConfigFile().toString(), sla);
			new PostTest().testPost(url + "job/" + obj.getName()
					+ "/config.xml", obj.getConfigFile().toString());

			result = "Now shutting down";
			// getResult(result);

			SmartJenkinsScheduler.run(obj);
			// Thread.sleep(1000);
			// rsp.setContentType("text/html;charset=utf-8");
			PrintWriter out = rsp.getWriter();
			out.write(result); 
			out.flush();
			out.close();
			// rsp.forwardToPreviousPage(req);
		}
	}

	public static void getWakeonlan(String slaveName) throws IOException,
			InterruptedException {
		getWakeonlan(slaveName, null, null, true);
	}

	public static void getWakeonlan(String s, String mac, String ip, boolean wait)
			throws IOException, InterruptedException {
		if (s != null) {
			String[][] slave = getipmac();
			for (int i = 0; i < slave.length; i++) {
				if (s.equals(slave[i][0])) {
					ip = slave[i][1];
					mac = slave[i][2];
					break;
				}
			}
		}
		String url = JENKINS.getRootUrl();
		String jobname = "unix";
		String os = JENKINS.getComputers()[0].getEnvironment()
				.toString().toLowerCase();
		if (os.contains("windows")) {
			jobname = "wol-win";
		} else {
			jobname = "wol-linux";
		}
		List<TopLevelItem> item = JENKINS.getItems();
		String filepath = "";
		int i = 0;
		AbstractProject obj = null;
		while (i < item.size()) {
			obj = (AbstractProject) item.get(i);
			if (obj.getName().equals(jobname)) {
				filepath = obj.getConfigFile().toString();
				break;
			}
			i++;
		}

		new WakeUpServlet().wakeup(filepath, mac, url + "job/" + jobname
				+ "/config.xml", jobname, ip);

		if (obj != null) {
			SmartJenkinsScheduler.run(obj);
			if (wait) {
				Thread.sleep(5 * 1000);
			}
		}
	}

	private static void getMac() throws IOException, InterruptedException {
		int m = JENKINS.getComputers().length;
		String[] os = new String[m - 1];
		String[][] slave = new String[m - 1][4];
		String[] Logall = new String[m];
		for (int i = 1; i < m; i++) {
			slave[i - 1][0] = get_all()[i].getName();
			if (get_all()[i].isOnline()) {
				Logall[i] = get_all()[i].getLog();
				if (Logall[i].contains("Windows"))
					slave[i - 1][3] = "windows";
				else
					slave[i - 1][3] = "linux";
				Logall[i] = Logall[i].substring(Logall[i].indexOf("/") + 1,
						Logall[i].indexOf("<") - 1);
				slave[i - 1][0] = get_all()[i].getName();
				slave[i - 1][1] = Logall[i];
				slave[i - 1][2] = new WakeUpServlet().getmac(Logall[i]);
			} else {
				String[][] local = new XmlModify().read(XML_PATH);
				slave[i - 1][1] = local[i - 1][1];
				slave[i - 1][2] = local[i - 1][2];
				slave[i - 1][3] = local[i - 1][3];
			}
		}
		new XmlModify().saveState(XML_PATH, slave);
	}

	public static void setShutdown(String[] needDoslave) throws IOException {
		String url = JENKINS.getRootUrl();
		if (url != null && !url.endsWith("/")) {
			url += '/';
		}
		String[][] slave = new XmlModify().read(XML_PATH);
		int count = 0;
		int countwin = 0;
		int m = 0;
		int n = 0;
		String[] os = new String[needDoslave.length];
		for (int i = 0; i < os.length; i++) {
			int j = 0;
			while (j < slave.length) {
				if (needDoslave[i].equals(slave[j][0])) {
					break;
				}
				j++;
			}
			os[i] = slave[j][3];
		}

		String[] sl1 = new String[needDoslave.length];
		String[] slwin = new String[needDoslave.length];
		for (int i = 0; i < needDoslave.length; i++) {
			if (os[i].equals("windows")) {
				slwin[m] = needDoslave[i];
				m++;
				countwin++;
			} else {
				sl1[n] = needDoslave[i];
				count++;
				n++;
			}
		}
		List<TopLevelItem> item = JENKINS.getItems();
		AbstractProject obj = (AbstractProject) item.get(0);
		if (n > 0) {
			for (int i = 0; i < item.size(); i++) {
				obj = (AbstractProject) item.get(i);
				if (obj.getName().equals("shutdown-linux"))
					break;
			}
			new XmlModify().setSlave(obj.getConfigFile().toString(), sl1);
			new PostTest().testPost(url + "job/" + obj.getName()
					+ "/config.xml", obj.getConfigFile().toString());

			SmartJenkinsScheduler.run(obj);
		}
		if (m > 0) {
			for (int i = 0; i < item.size(); i++) {
				obj = (AbstractProject) item.get(i);
				if (obj.getName().equals("shutdown-win"))
					break;
			}
			new XmlModify().setSlave(obj.getConfigFile().toString(), slwin);
			new PostTest().testPost(url + "job/" + obj.getName()
					+ "/config.xml", obj.getConfigFile().toString());

			SmartJenkinsScheduler.run(obj);
		}
	}
}
