package com.abchina.util;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarResourceParser {
    private static final String WEB_CONF_NAME = "WEB-INFO/web.xml";

    public static WebXmlModel parseConfigFromJar(File file) throws IOException {
        JarFile jarFile = new JarFile(file.getAbsolutePath());
        Enumeration<JarEntry> entries = jarFile.entries();
        JarEntry configEntry = null;
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            if (WEB_CONF_NAME.equals(name)) {
                configEntry = jarEntry;
                break;
            }
        }
        if (configEntry == null) return null;
        InputStream input = jarFile.getInputStream(configEntry);
        XMLConfigReader xmlConfigReader = new XMLConfigReader();
        return xmlConfigReader.parseXmlFileAsModel(input);

    }
}
