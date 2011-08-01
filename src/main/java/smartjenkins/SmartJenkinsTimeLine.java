package smartjenkins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smartjenkins.SmartJenkinsConstants.EventType;

public class SmartJenkinsTimeLine {
	private static SmartJenkinsTimeLine instance = new SmartJenkinsTimeLine();
	private static Map<String, List<Event>> events;

	private SmartJenkinsTimeLine() {
		events = new HashMap<String, List<Event>>();
	}

	public static SmartJenkinsTimeLine getInstance() {
		return instance;
	}

	public static void addEvent(String jobName, long start, long end, String title, String description, EventType type) {
		List<Event> list = events.get(jobName);
		if (list == null) {
			list = new ArrayList<Event>();
			events.put(jobName, list);
		}
		final Event event = new Event(start, end, title, description, type);
		list.remove(event);
		list.add(event);
	}
	
	public static void removeEvent(String jobName, long start, long end, EventType type) {
		List<Event> list = events.get(jobName);
		if (list == null) {
			return;
		}
		Event removeEvent = null;
		for (Event e : list) {
			if (e.type == type
					&& Math.abs(e.start - start) <= SmartJenkinsConstants.TIME_ERROR
					&& Math.abs(e.end - end) <= SmartJenkinsConstants.TIME_ERROR) {
				removeEvent = e;
				break;
			}
		}
		if (removeEvent != null) {
			list.remove(removeEvent);
		}
	}

	public static void removeEvent(String jobName, EventType type) {
		List<Event> list = events.get(jobName);
		if (list == null) {
			return;
		}
		final List<Event> removeList = new ArrayList<Event>();
		for (Event e : list) {
			if (e.type == type) {
				removeList.add(e);
			}
		}
		list.removeAll(removeList);
	}

	public static String getEventJSON(String jobName) {
		final StringBuffer sb = new StringBuffer();
		
		final List<Event> list = events.get(jobName);
		if (list != null) {
			for (Event event : list) {
				sb.append(event.toJSON()).append(SmartJenkinsConstants.COMMA);
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

		return sb.toString();
	}

	private static class Event {
		private long start;
		private long end;
		private String title;
		private String description;
		private EventType type;

		public Event(long start, long end, String title, String description, EventType type) {
			this.start = start;
			this.end = end;
			this.title = title;
			this.description = description;
			this.type = type;
		}

		public String toJSON() {
			final StringBuffer sb = new StringBuffer();
			sb.append("{");
			sb.append("'start':'").append(SmartJenkinsUtils.formatDate(start)).append("'");
			if (end >= start) {
				sb.append(",'end':'").append(SmartJenkinsUtils.formatDate(end)).append("'");
				sb.append(",'durationEvent':true");
			} else {
				sb.append(",'durationEvent':false");
			}
			if (title != null) {
				sb.append(",'title':'").append(title).append("'");
			}
			if (description != null) {
				sb.append(",'description':'").append(description).append("'");
			}
			switch(type) {
			case blocked:
				sb.append(",'icon':'").append(SmartJenkinsConstants.PLUGIN_RESOURCE_PATH).append("timeline_2.3.0/timeline_js/images/dark-red-circle.png'");
				break;
			case scheduled:
				sb.append(",'icon':'").append(SmartJenkinsConstants.PLUGIN_RESOURCE_PATH).append("timeline_2.3.0/timeline_js/images/dark-blue-circle.png'");
				break;
			}
			sb.append("}");
			return sb.toString();
		}

		public boolean equals(Object o) {
			if (!(o instanceof Event)) {
				return false;
			}
			final Event e = (Event) o;
			return type == e.type && start == e.start && end == e.end;
		}
	}
}
