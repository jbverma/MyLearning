package com.sample.datagenerator;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Node;
import java.io.File;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;

//@author Jitendra


public class GenerateStructuralData {
	private Document requestDoc;
	private Properties envProperties;
	private Properties appProperties;
	int messageCounterStart =1;
	int messagesPerFile = 30;
	int maxSdpMessage = 120;
	int maxAllowedFilesInIncoming = 300;
	int abortAfterTries = 12;
	int maxFilesRetryCounter=0;
	String outputDir;

	Map<String, Queue<Integer>> counterMap = new HashMap<String, Queue<Integer>>();
	Map<String, Integer> currentCounter = new HashMap<String, Integer>();

	private GenerateStructuralData(){

	}

	public void onInit(String [] args) {
		envProperties = loadProperties(args[0]);
		appProperties = loadProperties(envProperties.getProperty("HOME_DIR") + envProperties.getProperty("applicationProperties"));
		requestDoc = loadXMLTemplate(appProperties.getProperty("baseDir") + "conf/templates/FlexSyncRequestTemplate.xml");

		messagesPerFile = Integer.parseInt(appProperties.getProperty("messagesPerFile"));
		maxSdpMessage = Integer.parseInt(appProperties.getProperty("maxSdpMessage"));
		maxAllowedFilesInIncoming = Integer.parseInt(appProperties.getProperty("maxAllowedFilesInIncoming"));
		abortAfterTries = Integer.parseInt(appProperties.getProperty("abortAfterTries"));
		outputDir = appProperties.getProperty("outputDir");
		messageCounterStart = Integer.parseInt(appProperties.getProperty("messageCounterStart"));

		setEntityCounter("sdpPerTransformer");
		setEntityCounter("sdpPerRateCode");
		setEntityCounter("sdpPerZipCode");
	}

	public void execute() throws XPathExpressionException {
		int messagePerFileCounter =0;
		int deleteFilesCounter = 0;
		for(int messageCounter = messageCounterStart; messageCounter <= maxSdpMessage; messageCounter++ ) {
			messagePerFileCounter++;
			requestDoc = buildSdpEntity(requestDoc, messageCounter, getEntityCounter("sdpPerTransformer"), getEntityCounter("sdpPerZipCode"), getEntityCounter("sdpPerRateCode"));
			if(messagePerFileCounter == messagesPerFile){
				try {
					messagePerFileCounter = 0;
					//remove template node
					removeTemplateNode();
					//write the file
					prettyPrint(new File(outputDir+"/Request_"+messageCounter+"_org1170.xml"), requestDoc);
					System.out.println("Request created File: Request_"+messageCounter+"_org1170.xml");
					requestDoc = loadXMLTemplate(envProperties.getProperty("HOME_DIR") + "conf/templates/FlexSyncRequestTemplate.xml");
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e.getMessage());
				}
			}
			if(deleteFilesCounter >= 300) {
				checkForMaxFilesInFolder();
				deleteFilesCounter = 0;
			}
			deleteFilesCounter++;
		}
	}


	private Document buildSdpEntity(Document requestDocument, int messageCounter, int dnUdcIdCounter, int postalCode, int rateCode) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node messageNode = (Node) xPath.evaluate("//SDPSyncMessage", requestDoc, XPathConstants.NODE);
		Node messageNodeClone = messageNode.cloneNode(true);

		Node sdpSyncMessagesNode = (Node) xPath.evaluate("/SDPSyncMessages", requestDoc, XPathConstants.NODE);
		sdpSyncMessagesNode.appendChild(messageNodeClone);

		Node messageIdNode = (Node) xPath.evaluate("//SDPSyncMessage[last()]/header/messageID", messageNodeClone, XPathConstants.NODE);
		if(messageIdNode.hasChildNodes())
			messageIdNode.removeChild(messageIdNode.getFirstChild());
		messageIdNode.appendChild(requestDoc.createTextNode("Message-"+messageCounter));
		//---------- DN --------
		Node dnMridNode = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePoint[type='DistributionNode']/mRID", messageNodeClone, XPathConstants.NODE);
		if(dnMridNode.hasChildNodes())
			dnMridNode.removeChild(dnMridNode.getFirstChild());
		dnMridNode.appendChild(requestDoc.createTextNode("DN-PERF-"+dnUdcIdCounter));

		Node dnMridNode1 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePointServicePointAssociation/parServicePointId[type='DistributionNode']/mRID", messageNodeClone, XPathConstants.NODE);
		if(dnMridNode1.hasChildNodes())
			dnMridNode1.removeChild(dnMridNode1.getFirstChild());
		dnMridNode1.appendChild(requestDoc.createTextNode("DN-PERF-"+dnUdcIdCounter));

		//----------Premise ---------
		Node prmsMridNode = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/serviceLocation/mRID", messageNodeClone, XPathConstants.NODE);
		if(prmsMridNode.hasChildNodes())
			prmsMridNode.removeChild(prmsMridNode.getFirstChild());
		prmsMridNode.appendChild(requestDoc.createTextNode("PRMS-PERF-"+messageCounter));

		Node prmsMridNode1 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/serviceLocation/postalCode", messageNodeClone, XPathConstants.NODE);
		if(prmsMridNode1.hasChildNodes())
			prmsMridNode1.removeChild(prmsMridNode1.getFirstChild());
		prmsMridNode1.appendChild(requestDoc.createTextNode(formatPostalCode(postalCode)));

		Node prmsMridNode2 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePoint/premiseId/mRID", messageNodeClone, XPathConstants.NODE);
		if(prmsMridNode2.hasChildNodes())
			prmsMridNode2.removeChild(prmsMridNode2.getFirstChild());
		prmsMridNode2.appendChild(requestDoc.createTextNode("PRMS-PERF-"+messageCounter));

		Node prmsMridNode3 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/serviceLocation/city", messageNodeClone, XPathConstants.NODE);
		if(prmsMridNode3.hasChildNodes())
			prmsMridNode3.removeChild(prmsMridNode3.getFirstChild());
		prmsMridNode3.appendChild(requestDoc.createTextNode("US-City-"+postalCode));

		//----------SDP UDC ID ---------
		Node sdpMridNode = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePoint[type='ServiceDeliveryPoint']/mRID", messageNodeClone, XPathConstants.NODE);
		if(sdpMridNode.hasChildNodes())
			sdpMridNode.removeChild(sdpMridNode.getFirstChild());
		sdpMridNode.appendChild(requestDoc.createTextNode("SDP-PERF-"+messageCounter));

		Node sdpMridNode1 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePointDeviceAssociation/servicePointId[type='ServiceDeliveryPoint']/mRID", messageNodeClone, XPathConstants.NODE);
		if(sdpMridNode1.hasChildNodes())
			sdpMridNode1.removeChild(sdpMridNode1.getFirstChild());
		sdpMridNode1.appendChild(requestDoc.createTextNode("SDP-PERF-"+messageCounter));

		Node sdpMridNode2 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePointServicePointAssociation/servicePointId[type='ServiceDeliveryPoint']/mRID", messageNodeClone, XPathConstants.NODE);
		if(sdpMridNode2.hasChildNodes())
			sdpMridNode2.removeChild(sdpMridNode2.getFirstChild());
		sdpMridNode2.appendChild(requestDoc.createTextNode("SDP-PERF-"+messageCounter));

		Node sdpMridNode3 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePoint/parameter[name='Rate Code']/value", messageNodeClone, XPathConstants.NODE);
		if(sdpMridNode3.hasChildNodes())
			sdpMridNode3.removeChild(sdpMridNode3.getFirstChild());
		sdpMridNode3.appendChild(requestDoc.createTextNode("R-"+rateCode));

		//----------Meter UDC ID ---------
		Node meterMridNode = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/device[type='Meter']/mRID", messageNodeClone, XPathConstants.NODE);
		if(meterMridNode.hasChildNodes())
			meterMridNode.removeChild(meterMridNode.getFirstChild());
		meterMridNode.appendChild(requestDoc.createTextNode("MTR-PERF-"+messageCounter));

		Node meterMridNode1 = (Node) xPath.evaluate("//SDPSyncMessage[last()]/payload/servicePointDeviceAssociation/deviceId[type='Meter']/mRID", messageNodeClone, XPathConstants.NODE);
		if(meterMridNode1.hasChildNodes())
			meterMridNode1.removeChild(meterMridNode1.getFirstChild());
		meterMridNode1.appendChild(requestDoc.createTextNode("MTR-PERF-"+messageCounter));

		return requestDocument;
	}

	private void setEntityCounter(String entity) {
		String[] entityCountArray = appProperties.getProperty(entity).split(",");
		Queue<Integer> entityQueue = new LinkedList<>();
		for (String string : entityCountArray) {
			entityQueue.add(Integer.parseInt(string));
		}

		currentCounter.put(entity+"Counter", 0);
		if(entity.equalsIgnoreCase("sdpPerRateCode")) {
			currentCounter.put(entity, 1975);
		}else if(entity.equalsIgnoreCase("sdpPerZipCode")){
			currentCounter.put(entity, 944101645);
		}else {
			currentCounter.put(entity, 133031);
		}

		counterMap.put(entity, entityQueue);
	}

	private int getEntityCounter(String entity) {
		Integer entityVal = currentCounter.get(entity+"Counter");
		if(entityVal == 0) {
			int nextVal = counterMap.get(entity).poll();
			currentCounter.put(entity+"Counter", nextVal);
			currentCounter.put(entity, currentCounter.get(entity)+1);
		}
		currentCounter.put(entity+"Counter", currentCounter.get(entity+"Counter")-1);
		return currentCounter.get(entity);
	}

	private Document loadXMLTemplate(String templateFilePath) {
		Document doc = null;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder;

			dBuilder = dbFactory.newDocumentBuilder();

			doc = dBuilder.parse(new File(templateFilePath));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		return doc;
	}

	private void removeTemplateNode(){
		try{
		//Remove template node
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node sdpSyncTemplateNode = (Node) xPath.evaluate("/SDPSyncMessages/SDPSyncMessage[./payload/servicePoint/mRID='Template']", requestDoc, XPathConstants.NODE);

		sdpSyncTemplateNode.getParentNode().removeChild(sdpSyncTemplateNode);
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	private void prettyPrint(File file, Document xml) throws Exception {

		Transformer tf = TransformerFactory.newInstance().newTransformer();

		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

		tf.setOutputProperty(OutputKeys.INDENT, "yes");

		if (!file.exists()) {
			file.createNewFile();
		}

		Writer writer = new FileWriter(file);

		tf.transform(new DOMSource(xml), new StreamResult(writer));

		writer.close();
	}

	private Properties loadProperties(String filePath) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(filePath);

			// load a properties file
			prop.load(input);

			return prop;

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private boolean checkForMaxFilesInFolder(){
		File f = new File(outputDir);

		// create new filename filter
		FilenameFilter fileNameFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (name.endsWith(".xml")) {
					return true;
				}
				return false;
			}
		};

		File[] files = f.listFiles(fileNameFilter);


		if (files.length >= maxAllowedFilesInIncoming) {
			System.out.println("Max allowed files are present. counter:"+maxFilesRetryCounter);
			for (int j = 0; j < abortAfterTries; j++) {
				files = f.listFiles(fileNameFilter);
				if (files.length >= maxAllowedFilesInIncoming) {
					maxFilesRetryCounter++;
					try {
						Thread.currentThread();
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					maxFilesRetryCounter = 0;
				}

			}
			if (maxFilesRetryCounter >= abortAfterTries) {
				throw new RuntimeException("FlexSync files are not being consumed. Please check the FlexSync status.");
			}
		}
		return false;
	}

	private void deleteFilesInDir(String directoryName) {
		File directory = new File(directoryName);
		// Get all files in directory
		File[] files = directory.listFiles();
		for (File file : files)		{
		   // Delete each file
		   if (!file.delete()) {
		       // Failed to delete file
		       System.out.println("Failed to delete "+file);
		   }
		}
	}


	public void onShutdown() throws IOException {

	}

	private String formatPostalCode(int postalCode) {
		StringBuilder postalCodeStr = new StringBuilder(""+postalCode);
		return postalCodeStr.insert(5, '-').toString();
	}

	public static void main (String [] args) {
		GenerateStructuralData generateStructuralData = new GenerateStructuralData();
		generateStructuralData.onInit(args);
		try {
			generateStructuralData.execute();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
