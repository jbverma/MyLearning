package org.iht.bootstrap.application;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.util.Properties;
import java.util.StringTokenizer;

public class ApplicationBootStrap {
    final static org.apache.log4j.Logger logger = Logger.getLogger(ApplicationBootStrap.class);
    Properties applicationProperties = new Properties();
    public static void main(String [] args){
        ApplicationBootStrap application = new ApplicationBootStrap();

        //Load application properties
        application.loadProperties(args[0]);

        //Load command line properties
        application.parseCmdProperties(args);

        String applicationPropFile = application.applicationProperties.getProperty("IHT_HOME") +
                        "/conf/appProperties/"+application.applicationProperties.getProperty("application.name")+".properties";
        application.loadProperties(applicationPropFile);

        //Load logger configuration
        application.configureLogger();

        //Call main application
        application.runApplication(args);
    }

    private void loadProperties(String path)  {
        InputStream input = null;
        try {
            input = new FileInputStream(path);
            // load a properties file
            applicationProperties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runApplication(String [] args){
        Class<?> classObject = null;
        try {
            logger.info("Application main class: "+applicationProperties.getProperty("Start-Class"));
            classObject = Class.forName(applicationProperties.getProperty("Start-Class"));
            Application instance = (Application) classObject.newInstance();
            logger.info("Calling main of start-class");
            instance.run(args);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            logger.error("Unable to load or intantiate class (" + applicationProperties.getProperty("Start-Class") + "): ", e);
            String message = "Unable to load or intantiate class (" + applicationProperties.getProperty("Start-Class") + "): " + e.getMessage();
            throw new RuntimeException(message, e);
        }
    }

    private void configureLogger(){
        File log4jConfig = new File(applicationProperties.getProperty("IHT_HOME") + "/conf/appProperties/log4j.properties");
        PropertyConfigurator.configure(log4jConfig.getAbsolutePath());
        logger.info("Application logger configured");
    }

    private void parseCmdProperties(String [] args){
        for(String arg : args){
            String cmdPropPrefix = CommonConstants.CMDLINE_PROPERTY_PREFIX;
            if(arg.startsWith(cmdPropPrefix)){
                String prop = arg.substring( cmdPropPrefix.length() );
                StringTokenizer st = new StringTokenizer( prop, "=", true );
                if( st.countTokens() == 3 ){
                    // Get property name
                    String name = st.nextToken().trim();
                    // Discard delimiter "="
                    st.nextToken();
                    // Get property value
                    String value = st.nextToken().trim();
                    //Overwrite property from file
                    applicationProperties.put(name, value);
                }else{
                    throw new RuntimeException(arg + " is not a valid property specification: [" + cmdPropPrefix + "key=value]" );
                }
            }
        }
    }
}
