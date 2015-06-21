package org.docker.hackathon.util;

import java.io.*;
import java.util.zip.*;

public class ArtifactUtils {

        public boolean isDDCArtifact(String artifactPath) {
                ZipInputStream zip = null;
                boolean ddcartifact = false;
                try {  
                        zip = new ZipInputStream(new FileInputStream(artifactPath));
                        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                                //System.out.println("isDDCArtifact : Checking "+entry.getName());
                                if (   entry.getName().endsWith("pom.xml") && entry.getName().contains("org.docker.hackathon") ) {
                                        //System.out.println("isDDCArtifact : Checking "+entry.getName()+" returning true");
                                        ddcartifact = true;
                                }
                                if (   entry.getName().endsWith(".class") && entry.getName().contains("com.sforce") ) {
                                        //System.out.println("isDDCArtifact : Checking "+entry.getName()+" returning true");
                                        ddcartifact = true;
                                }
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                } finally {
                        if(zip != null) {
                                try {  
                                        zip.close();
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }
                }
                return ddcartifact;
        }

}
