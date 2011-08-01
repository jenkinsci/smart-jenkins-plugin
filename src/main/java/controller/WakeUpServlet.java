package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smartjenkins.SmartJenkinsConstants;

public class WakeUpServlet {
	private static final Pattern MAC_ADDRESS_PATTERN = Pattern.compile("\\w{1,2}([-:]\\w{2}){5}");
	public void wakeup(String filepath,String mac,String url,String jobname,String ip){
		new XmlModify().setMac(filepath,mac,jobname,ip);
		new PostTest().testPost(url,filepath);
	} 

	public String getmac(String ip){
		Properties props=System.getProperties();
		String osName = props.getProperty("os.name"); 
		Runtime rt2 = Runtime.getRuntime();
		ip=ip.replace(" ", "");
		String mac="no mac";
		if (SmartJenkinsConstants.ROOT_PATH.contains(ip)) {
			mac= "master computer";
			return mac; 
		}
		try {
			if(osName.contains("Mac"))
			{
				String m="arp "+ip;
				Process p1= rt2.exec(m);
				BufferedReader in = null;
				try {
					String str1=null;
					in = new BufferedReader(new InputStreamReader(p1.getInputStream()));
					Matcher matcher;
					while ((str1=in.readLine()) != null) {
						if(str1.contains(ip)) {
							matcher = MAC_ADDRESS_PATTERN.matcher(str1);
							if (matcher.find()) {
								mac = matcher.group();
								if (mac.indexOf(":") == 1 || mac.indexOf("-") == 1) {
									mac = "0" + mac;
								}
								return mac;
							}
						}
						return mac;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				} finally {
					in.close();
				}
				return mac;
			}
			else{
				String m="arp -a "+ip;
				Process p1= rt2.exec(m);
				BufferedReader in = null;
				try {
					String str1=null;
					in = new BufferedReader(new InputStreamReader(p1.getInputStream()));
					Matcher matcher;
					while ((str1=in.readLine()) != null) {
						if(str1.contains(ip)) {
							matcher = MAC_ADDRESS_PATTERN.matcher(str1);
							if (matcher.find()) {
								mac = matcher.group();
								if (mac.indexOf(":") == 1 || mac.indexOf("-") == 1) {
									mac = "0" + mac;
								}
								return mac;
							}
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				} finally {
					in.close();
				}
				return mac;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return mac;
		}
	}
}