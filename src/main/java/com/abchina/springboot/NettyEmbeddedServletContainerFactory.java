package com.abchina.springboot;

import com.abchina.core.constants.HttpConstants;
import com.abchina.servlet.ServletContext;
import com.abchina.servlet.ServletDefaultHttpServlet;
import com.abchina.servlet.ServletSessionCookieConfig;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * servlet容器工厂
 */
public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements EmbeddedServletContainerFactory , ResourceLoaderAware {

    protected ResourceLoader resourceLoader;

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        try {
            ServletContext servletContext = newServletContext();
            NettyEmbeddedServletContainer container = newNettyEmbeddedServletContainer(servletContext);

            if (isRegisterDefaultServlet()) {
                registerDefaultServlet(servletContext);
            }

            for (ServletContextInitializer initializer : initializers) {
                initializer.onStartup(servletContext);
            }
            return container;
        }catch (Exception e){
            throw new IllegalStateException(e);
        }
    }

    /**
     * 注册默认servlet
     * @param servletContext servlet上下文
     */
    protected void registerDefaultServlet(ServletContext servletContext){
        ServletDefaultHttpServlet defaultServlet = new ServletDefaultHttpServlet();
        servletContext.addServlet("default",defaultServlet);
    }

    /**
     * 新建netty容器
     * @param servletContext servlet上下文
     * @return netty容器
     */
    public NettyEmbeddedServletContainer newNettyEmbeddedServletContainer(ServletContext servletContext){
        NettyEmbeddedServletContainer container = new NettyEmbeddedServletContainer(servletContext, 50);
        return container;
    }

    /**
     * 新建servlet上下文
     * @return
     */

    public ServletContext newServletContext(){
        //加载外部jar资源 URL[]
        URL[] urls = getExternalJarResources();
        return this.newServletContext(urls);
    }

    /**
     * servlet上下文并加载外部jar资源
     * @param urls
     * @return
     */
    public ServletContext newServletContext(URL[] urls){
        //获取类加载器
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
        //session配置信息
        ServletSessionCookieConfig sessionCookieConfig = loadSessionCookieConfig();
        ServletContext servletContext = new ServletContext(
                new InetSocketAddress(getAddress(),getPort()),
                new URLClassLoader(urls, parentClassLoader),
                getContextPath(),
                getServerHeader(),
                sessionCookieConfig);
        return servletContext;
    }

    private URL[] getExternalJarResources() {
        List<URL> urlList = new ArrayList<>();
        try{
            File buildDir = new File(HttpConstants.ROOT_PATH+HttpConstants.BUILD_PATH);
            File[] files;
            if (buildDir.isDirectory() && (files = buildDir.listFiles()) != null) {
                for (File file : files) {
                    URL url = new URL("file:" + HttpConstants.ROOT_PATH+HttpConstants.BUILD_PATH + File.separator+ file.getName());
                    urlList.add(url);
                }
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
            return new URL[]{};
        }

        URL[] urls = new URL[urlList.size()];
        for(int i = 0; i < urlList.size(); i++){
            urls[i] = urlList.get(i);
        }
        return urls;


    }

    /**
     * 加载session的cookie配置
     * @return cookie配置
     */
    protected ServletSessionCookieConfig loadSessionCookieConfig(){
        ServletSessionCookieConfig sessionCookieConfig = new ServletSessionCookieConfig();
        sessionCookieConfig.setMaxAge(-1);

        sessionCookieConfig.setSessionTimeout(getSessionTimeout());
        return sessionCookieConfig;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }



}
