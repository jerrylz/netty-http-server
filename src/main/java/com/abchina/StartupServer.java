package com.abchina;

import com.abchina.servlet.ServletContext;
import com.abchina.springboot.NettyEmbeddedServletContainer;
import com.abchina.springboot.NettyEmbeddedServletContainerFactory;

import java.io.IOException;

public class StartupServer {
    public static void main(String[] args) throws IOException {
        NettyEmbeddedServletContainerFactory factory = new NettyEmbeddedServletContainerFactory();

        ServletContext servletContext = factory.newServletContext();
        NettyEmbeddedServletContainer container = factory.newNettyEmbeddedServletContainer(servletContext);
        container.start();
    }

}
