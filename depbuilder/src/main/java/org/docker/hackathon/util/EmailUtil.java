package org.docker.hackathon.util;

import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

public class EmailUtil {
	
	public static void sendMail(InternetAddress toAddresses[], String subject, String body) {
		try {
			InternetAddress fromAddress = new InternetAddress("dc-releng@jigsaw.com");
			EmailUtil.sendMail(toAddresses,fromAddress,subject,body);
		} catch(AddressException e) {
			System.out.println("sendMail : Error while sending email.");
		}
	}

	   public static void sendMail(Address [] toAddresses, Address fromAddress, String subject, String body)
	   {
	      String host = "localhost"; 
	      Properties properties = System.getProperties();
	      properties.setProperty("mail.smtp.host", host);
	      Session session = Session.getDefaultInstance(properties);
	      try{
	         MimeMessage message = new MimeMessage(session);
	         message.setFrom(fromAddress);
	         message.addRecipients(Message.RecipientType.TO, toAddresses);
	         message.setSubject(subject);
	         message.setContent(body,"text/html" );
	         Transport.send(message);
	         System.out.println("Sent message successfully....");
	      }catch (MessagingException mex) {
	         mex.printStackTrace();
	      }
	}

	   public static void sendMail(Address toAddress, Address fromAddress, String subject, String body)
	   {
	      String host = "localhost"; 
	      Properties properties = System.getProperties();
	      properties.setProperty("mail.smtp.host", host);
	      Session session = Session.getDefaultInstance(properties);
	      try{
	         MimeMessage message = new MimeMessage(session);
	         message.setFrom(fromAddress);
	         message.addRecipient(Message.RecipientType.TO, toAddress);
	         message.setSubject(subject);
	         message.setContent(body,"text/html" );
	         Transport.send(message);
	         System.out.println("Sent message successfully....");
	      }catch (MessagingException mex) {
	         mex.printStackTrace();
	      }
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InternetAddress toaddr = new InternetAddress();
		InternetAddress fromaddr = new InternetAddress();
		try {
			toaddr = new InternetAddress("ramesh.thummala@gmail.com");
			fromaddr = new InternetAddress("ramesh.thummala@gmail.com");
		}catch (MessagingException mex) {
			mex.printStackTrace();
		}
		sendMail(toaddr,fromaddr,"test subject","test msg");

	}

}
