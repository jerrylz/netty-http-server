package com.abchina.springboot;

import com.abchina.servlet.ServletContext;
import com.abchina.servlet.ServletDefaultHttpServlet;
import com.abchina.servlet.ServletSessionCookieConfig;
import com.abchina.util.JarResourceParser;
import com.abchina.util.WebXmlModel;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * EmbeddedWebApplicationContext - createEmbeddedServletContainer
 * ImportAwareBeanPostProcessor
 */
public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements EmbeddedServletContainerFactory , ResourceLoaderAware {
    private static String rootPath = System.getProperty("user.dir");
    private static String bulidPath = "/build";
    protected ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

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
     * @throws SSLException ssl异常
     */
    public NettyEmbeddedServletContainer newNettyEmbeddedServletContainer(ServletContext servletContext) throws SSLException {
        Ssl ssl = getSsl();
        NettyEmbeddedServletContainer container = new NettyEmbeddedServletContainer(servletContext,ssl,50);
        return container;
    }

    /**
     * 新建servlet上下文
     * @return
     */

    public ServletContext newServletContext(){
        URL[] urls = getExternalJarResources();
        return this.newServletContext(urls);
    }

    public ServletContext newServletContext(URL[] urls){
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
        ServletSessionCookieConfig sessionCookieConfig = loadSessionCookieConfig();
        ServletContext servletContext = new ServletContext(
                new InetSocketAddress(getAddress(),getPort()),
                new URLClassLoader(urls, parentClassLoader),
                getContextPath(),
                getServerHeader(),
                sessionCookieConfig);
                loadResources(servletContext);
        return servletContext;
    }

    private void loadResources(ServletContext servletContext){
        try{
            File buildDir = new File(rootPath+bulidPath);
            File[] files;
            if (buildDir.isDirectory() && (files = buildDir.listFiles()) != null) {
                for (File file : files) {
                    WebXmlModel webXmlModel = JarResourceParser.parseConfigFromJar(file);
                    System.out.println(webXmlModel);
                    initServlet(servletContext, webXmlModel);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    private URL[] getExternalJarResources() {
        List<URL> urlList = new ArrayList<>();
        try{
            File buildDir = new File(rootPath+bulidPath);
            File[] files;
            if (buildDir.isDirectory() && (files = buildDir.listFiles()) != null) {
                for (File file : files) {
                    URL url = new URL("file:" + rootPath + bulidPath + "/" + file.getName());
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

    private static void initServlet(ServletContext servletContext, WebXmlModel webXmlModel) throws ClassNotFoundException {
        WebXmlModel.ServletMappingNode[] servletMappingNodes = webXmlModel.getServletMappingNodes();
        Map<String, String> mappingNodes = new HashMap<>();
        for(WebXmlModel.ServletMappingNode servletMappingNode : servletMappingNodes){
            mappingNodes.put(servletMappingNode.getServletName(), servletMappingNode.getUrlPattern());
        }
        WebXmlModel.ServletNode[] servletNodes = webXmlModel.getServletNodes();

        for(WebXmlModel.ServletNode node : servletNodes){
            String servletClass = node.getServletClass();
            String servletName = node.getServletName();
            Map<String, String> initParamMap = new HashMap<>();
            WebXmlModel.ServletInitParam[] servletInitParams = node.getServletInitParams();
            if(servletInitParams != null){
                for(WebXmlModel.ServletInitParam servletInitParam : node.getServletInitParams()){
                    initParamMap.put(servletInitParam.getParamName(), servletInitParam.getParamValue());
                }
            }
            servletContext.addServlet(servletName, servletClass).addMapping(mappingNodes.get(servletName));

        }


        WebXmlModel.FilterMapping[] filterMappings = webXmlModel.getFilterMappings();
        Map<String, String> filterMappingNodes = new HashMap<>();
        for(WebXmlModel.FilterMapping filterMapping : filterMappings){
            filterMappingNodes.put(filterMapping.getFilterName(), filterMapping.getUrlPattern());
        }
        WebXmlModel.FilterNode[] filterNodes = webXmlModel.getFilterNodes();

        for(WebXmlModel.FilterNode node : filterNodes){
            String filterClass = node.getFilterClass();
            String filterName = node.getFilterName();
            servletContext.addFilter(filterName, filterClass)
                    .addMappingForUrlPatterns(null, true, filterMappingNodes.get(filterName));

        }
    }

}
