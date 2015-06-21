package org.docker.hackathon.util;

import java.util.*;
import java.text.*;

/**
 * @author rthummalapenta
 *
 */
public class DateUtils {
	
    public static String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
}

}
