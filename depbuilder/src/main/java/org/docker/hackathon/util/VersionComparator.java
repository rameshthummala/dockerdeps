
package org.docker.hackathon.util;

import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;

/**
 * @author rthummalapenta
 *
 */
public class VersionComparator implements Comparator<String> {
	
    public int compare(String v1, String v2) {
        String s1 = normalisedVersion(v1);
        String s2 = normalisedVersion(v2);
        int compRes = s1.compareTo(s2);
        //System.out.println("compare : comparison result for "+v1+" and "+v2+" is : "+compRes);
        return compRes;
    }

    public static String normalisedVersion(String version) {
        return normalisedVersion(version, ".", 7);
    }

    public static String normalisedVersion(String version, String sep, int maxWidth) {
        String[] split = Pattern.compile(sep, Pattern.LITERAL).split(version);
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            sb.append(String.format("%" + maxWidth + 's', s));
        }
        return sb.toString();
    }
    
	public static void main(String[] args) {
		VersionComparator vc = new VersionComparator();
		ArrayList <String> al = new ArrayList<String> ();
		al.add("13.2.24-1");
		al.add("13.2.1-1");
		al.add("13.5.4-1");
		al.add("186.0.121-1");
		al.add("186.0.99-1");
		Collections.sort(al,vc);
		for( String ver : al) {
			System.out.println(ver);
		}
		
		String v1 = "186.0.121-1";
		String v2 = "186.0.99-1";
	    String s1 = normalisedVersion(v1);
        String s2 = normalisedVersion(v2);
        int comp = s1.compareTo(s2);
        System.out.println("comp between "+s1+" and "+s2+" and "+v1+ " and "+v2+ " is : "+comp);
		
	}
}
