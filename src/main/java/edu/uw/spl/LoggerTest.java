package edu.uw.spl;

import java.util.logging.*;

/**Confirming logging.properties file works*/
public class LoggerTest {
    
    public static void main(String[] args) {
        Logger logger = Logger.getLogger("LoggerTest");
        
        logger.fine("Test logger");
        System.out.println("Testing logger properties");
        

    }

}
