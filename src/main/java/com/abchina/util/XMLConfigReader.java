package com.abchina.util;


import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.io.InputStream;

public class XMLConfigReader {

    /**
     * 解析
     * @param in
     * @return
     * @throws IOException
     */
    public WebXmlModel parseXmlFileAsModel(InputStream in) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.readValue(in, WebXmlModel.class);
    }


}