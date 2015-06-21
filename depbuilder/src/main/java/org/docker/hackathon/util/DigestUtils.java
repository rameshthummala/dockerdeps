package org.docker.hackathon.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class DigestUtils {
	Level logLevel = Level.toLevel("INFO");
	static Logger logger = Logger.getLogger(DigestUtils.class.getName());
	public static boolean saveHash(String filePath,String digestType) {
		logger.info("saveHash : Incoming filePath : "+filePath+" and digestType : "+digestType);
		String hash = getHash(filePath,digestType);
		boolean retval = false;
		try {
			
			String digestTypeStr = new String();
			if(digestType.equals("MD5")) {
				digestTypeStr = "md5";
			}
			if(digestType.equals("SHA-1")) {
				digestTypeStr = "sha1";
			}
		File hashFile = new File(filePath+"."+digestTypeStr);
		if (!hashFile.exists()) {
			hashFile.createNewFile();
		}
		FileWriter hashFileWriter = new FileWriter(hashFile.getAbsoluteFile());
		BufferedWriter hashBufferWriter = new BufferedWriter(hashFileWriter);
		hashBufferWriter.write(hash);
		hashBufferWriter.close();
		logger.info("saveHash : Successfully created "+digestType+" hash file at : "+hashFile);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return retval;
	}
	
    public static String getHash(String filePath,String digestType) {
        MessageDigest messageDigest;
        FileInputStream fis = null;
        try {
            messageDigest = MessageDigest.getInstance(digestType);
            fis = new FileInputStream(filePath);
            messageDigest.reset();
            
            byte[] dataBytes = new byte[1024];
            int bytesread = 0; 
            while ((bytesread = fis.read(dataBytes)) != -1) {
              messageDigest.update(dataBytes, 0, bytesread);
            };
            final byte[] resultByte = messageDigest.digest();
            if (fis != null) {
        		fis.close();
        	}
            return new String(Hex.encodeHex(resultByte));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
