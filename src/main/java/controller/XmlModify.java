package controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class XmlModify {
	public void BuildXMLDoc(String filePath) throws IOException, JDOMException {
		Element root = new Element("node");
		root.addContent(new Element("list"));
		Document Doc = new Document(root);
		Element list = root.getChild("list");
		Element elements = new Element("slave");
		elements.addContent(new Element("name"));
		elements.addContent(new Element("ip"));
		elements.addContent(new Element("mac"));
		elements.addContent(new Element("os"));
		list.addContent(elements);

		XMLOutputter XMLOut = new XMLOutputter();
		XMLOut.output(Doc, new FileOutputStream(filePath));
	}

	public void createxml(String filePath, String command, String name) {
		try {
			SAXBuilder builder = new SAXBuilder();
			File file = new File(filePath);
			if (file.exists()) {
				Document document = (Document) builder.build(file);
				Element root = document.getRootElement();
				Element temp = root.getChild("axes");
				Element tempchild = temp;
				tempchild = (Element) tempchild.clone();
				tempchild.setName("hudson.matrix.LabelAxis");
				temp.addContent(tempchild);
				temp = temp.getChild("hudson.matrix.LabelAxis");
				Element temp1 = (Element) temp.clone();
				temp1.setName("name");
				temp1.setText("label");
				temp.addContent(temp1);
				temp1 = (Element) temp1.clone();
				temp1.setText(null);
				temp1.setName("values");
				temp.addContent(temp1);
				temp = temp.getChild("values");
				temp1 = (Element) temp1.clone();
				temp1.setName("string");
				if (name.contains("wol"))
					temp1.setText("master");
				temp.addContent(temp1);
				Element firstElement = root.getChild("builders");
				Element child = firstElement;
				child = (Element) child.clone();
				if (name.contains("linux")) {
					child.setName("hudson.tasks.Shell");
					firstElement.addContent(child);
					firstElement = firstElement.getChild("hudson.tasks.Shell");
					child = (Element) child.clone();
					child.setName("command");
					child.setText(command);
					firstElement.addContent(child);
				} else {
					child.setName("hudson.tasks.BatchFile");
					firstElement.addContent(child);
					firstElement = firstElement
							.getChild("hudson.tasks.BatchFile");
					child = (Element) child.clone();
					child.setName("command");
					child.setText(command);
					firstElement.addContent(child);
				}
				String xmlFileData = new XMLOutputter().outputString(document);
				FileWriter fileWriter = new FileWriter(file);
				fileWriter.write(xmlFileData);
				fileWriter.close();
			} else {
				System.out.println("File does not exist");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void setSlave(String filePath, String[] slave) {
		try {
			SAXBuilder builder = new SAXBuilder();
			File file = new File(filePath);
			if (file.exists()) {
				Document document = (Document) builder.build(file);
				Element root = document.getRootElement();
				Element firstElement = root.getChild("axes")
						.getChild("hudson.matrix.LabelAxis").getChild("values");
				Element[] child = new Element[slave.length];
				for (int i = 0; i < slave.length; i++)
					child[i] = (Element) firstElement.getChild("string");

				firstElement.removeChildren("string");

				for (int i = 0; i < slave.length; i++) {
					child[i].setText(slave[i]);
					child[i] = (Element) child[i].clone();
					firstElement.addContent(child[i]);
				}
				String xmlFileData = new XMLOutputter().outputString(document);
				FileWriter fileWriter = new FileWriter(file);
				fileWriter.write(xmlFileData);
				fileWriter.close();
			} else {
				System.out.println("File does not exist");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void setMac(String filePath, String mac, String jobname, String ip) {
		try {
			SAXBuilder builder = new SAXBuilder();
			File file = new File(filePath);
			if (file.exists()) {
				Document document = (Document) builder.build(file);
				Element root = document.getRootElement();
				Element firstElement = root.getChild("builders")
						.getChild("hudson.tasks.Shell").getChild("command");
				if ("wol-linux".equals(jobname))
					firstElement.setText("wakeonlan " + mac);
				else
					firstElement.setText("wolcmd " + mac + " " + ip
							+ " 255.255.255.0 23");
				String xmlFileData = new XMLOutputter().outputString(document);
				FileWriter fileWriter = new FileWriter(file);
				fileWriter.write(xmlFileData);
				fileWriter.close();
			} else {
				System.out.println("File does not exist");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void saveState(String filePath, String[][] slave) {
		try {
			SAXBuilder builder = new SAXBuilder();
			File file = new File(filePath);
			if (file.exists()) {
				Document document = (Document) builder.build(file);
				Element root = document.getRootElement();
				Element list = root.getChild("list");
				Element child = (Element) list.getChild("slave").clone();
				list.removeContent();
				if (slave != null) {
					for (int i = 0; i < slave.length; i++) {
						child = (Element) child.clone();
						child.getChild("name").setText(slave[i][0]);
						child.getChild("ip").setText(slave[i][1]);
						child.getChild("mac").setText(slave[i][2]);
						child.getChild("os").setText(slave[i][3]);
						list.addContent(child);
					}
				} else {
					child = (Element) child.clone();
					child.getChild("name").setText(null);
					child.getChild("ip").setText(null);
					child.getChild("mac").setText(null);
					child.getChild("os").setText(null);
					list.addContent(child);
				}
				String xmlFileData = new XMLOutputter().outputString(document);
				FileWriter fileWriter = new FileWriter(file);
				fileWriter.write(xmlFileData);
				fileWriter.close();
			} else {
				System.out.println("File does not exist");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public String[][] read(String filePath) {
		try {
			SAXBuilder builder = new SAXBuilder();
			File file = new File(filePath);
			if (file.exists()) {
				Document document = (Document) builder.build(file);
				Element root = document.getRootElement();
				Element list = root.getChild("list");
				List<Element> children = list.getChildren();
				Iterator iterator = children.iterator();
				Element next, temp;
				List m;
				Iterator iterator1;
				String[][] s = new String[children.size()][4];
				for (int i = 0; iterator.hasNext(); i++) {
					next = (Element) iterator.next();
					m = next.getChildren();
					iterator1 = m.iterator();
					for (int j = 0; j < s[i].length; j++) {
						temp = (Element) iterator1.next();
						s[i][j] = temp.getText();
					}
				}

				return s;
			}

			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
