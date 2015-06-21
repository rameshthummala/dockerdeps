package org.docker.hackathon.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



public class XmlUtils {
    
    /**
     * Instantiates a new xml util object.
     */
    public XmlUtils () {
    }
    
    /**
     * Format the passed unformatted XML.
     * 
     * @param unformattedXml
     *            the unformatted xml
     * @return the formatted XML in string format
     */
    public String format(String unformattedXml) {
        try {
            final Document document = parseXmlFile(unformattedXml);
            
            OutputFormat format = new OutputFormat(document);
            format.setLineWidth(65);
            format.setIndenting(true);
            format.setIndent(4);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);
            
            return out.toString();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Parses the xml file.
     * 
     * @param in
     *            the in
     * @return the document
     */
    private Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        catch (SAXException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * The XmlFormatter takes an unformatted XML file and returns the formatted
     * version in a string.
     */
    public static void main(String[] args) {
        String unformattedXml = "";
        String src = args[0];
        String dest = args[1];
        String record = null;
        DataInputStream dis = null;
        DataOutputStream dos = null;
        
        try {
            File fi = new File(src);
            FileInputStream fis = new FileInputStream(fi);
            BufferedInputStream bis = new BufferedInputStream(fis);
            dis = new DataInputStream(bis);
            
            File fo = new File(dest);
            FileOutputStream fos = new FileOutputStream(fo);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            dos = new DataOutputStream(bos);
            
            if (dis != null) {
                while ((record = dis.readLine()) != null) {
                    System.out.println("Record is : " + record + "\n");
                    unformattedXml = unformattedXml + record;
                }
                dis.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (bos != null) {
                bos.close();
            }
        }
        catch (IOException e) { // Error reading from input file...
            IOException wrapper = new IOException("XmlFormatter: Unable to read file: "
                    + src + "for formatting XML Content.");
        }
        
        String formattedXML = new XmlUtils().format(unformattedXml);
        
        try {
            File fo = new File(dest);
            FileOutputStream fos = new FileOutputStream(fo);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            dos = new DataOutputStream(bos);
            
            if (dos != null) {
                dos.writeBytes(formattedXML + "\n");
                dos.close();
            }
        }
        catch (IOException e) { // Error writing to output file...
            IOException wrapper = new IOException(
                    "XmlFormatter: Unable to write formatted XML content to file: "
                            + dest + "\n");
        }
        
    }
    

    /**
     * This method serializes a java bean to xml using JAXB
     * 
     * @param objToMarshal
     *            java bean to be serialized to xml
     * @param clazz
     *            type of bean to be serialized
     * @return String representation of serialized xml
     */
    public static String buildXmlString(Object objToMarshal, Class<?> clazz) {
        Writer writer = new StringWriter();
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(clazz);
            Marshaller xmlMarshaller = jaxbContext.createMarshaller();
            xmlMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            xmlMarshaller.marshal(objToMarshal, writer);
        }
        catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return writer.toString();
    }

}
