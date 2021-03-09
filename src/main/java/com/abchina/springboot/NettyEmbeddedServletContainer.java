package com.abchina.springboot;

import cn.hutool.core.util.StrUtil;
import com.abchina.servlet.ServletContext;
import com.abchina.servlet.ServletRegistration;
import com.abchina.core.AbstractNettyServer;
import com.abchina.util.JarResourceParser;
import com.abchina.util.WebXmlModel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.servlet.ServletException;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class NettyEmbeddedServletContainer extends AbstractNettyServer implements EmbeddedServletContainer {
    private static Logger log = LoggerFactory.getLogger(NettyEmbeddedServletContainer.class);
    private ServletContext servletContext;
    private EventExecutorGroup dispatcherExecutorGroup;
    private ChannelHandler dispatcherHandler;
    //servlet上下文集合，key为contextPath value为servlet上下文
    private Map<String, ServletContext> servletContextMap = new HashMap<>();
    //调度器
    private Map<String, NettyServletDispatcherHandler> dispatcherHandlerMap = new HashMap<>();
    private Map<String, DefaultEventExecutorGroup> executorGroupMap = new HashMap<>();

    public NettyEmbeddedServletContainer(ServletContext servletContext, int bizThreadCount) {
        super(servletContext.getServerSocketAddress());
        this.servletContext = servletContext;
        this.dispatcherExecutorGroup = new DefaultEventExecutorGroup(bizThreadCount);
        this.dispatcherHandler = new NettyServletDispatcherHandler(servletContext);
        setInitializerMap(newInitializerChannelHandlerMap());
    }

    public NettyEmbeddedServletContainer(List<ServletContext> servletContexts, int bizThreadCount){
        super(servletContexts.get(0).getServerSocketAddress());
        for(ServletContext servletContext : servletContexts){
            String contextPath = servletContext.getFile() == null ? "" : servletContext.getFile().getName();
            servletContextMap.put(contextPath, servletContext);
            dispatcherHandlerMap.put(contextPath, new NettyServletDispatcherHandler(servletContext));
            executorGroupMap.put(contextPath, new DefaultEventExecutorGroup(bizThreadCount));
        }
        setInitializerMap(newInitializerChannelHandlerMap());
    }

    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("HttpCodec", new HttpServerCodec(4096, 8192, 8192, false)); //HTTP编码解码Handler
                pipeline.addLast("Aggregator", new HttpObjectAggregator(512 * 1024));  // HTTP聚合，设置最大消息值为512KB
                pipeline.addLast("ServletCodec",new NettyServletCodecHandler(servletContext) ); //处理请求，读入数据，生成Request和Response对象
                pipeline.addLast(dispatcherExecutorGroup, "Dispatcher", dispatcherHandler); //获取请求分发器，让对应的Servlet处理请求，同时处理404情况
            }
        };
    }

    @Override
    protected Map<String, ChannelInitializer<? extends Channel>> newInitializerChannelHandlerMap() {
        Map<String, ChannelInitializer<? extends Channel>> initializerMap = new HashMap<>();
        Set<String> keySet = servletContextMap.keySet();
        for(String contextPath : keySet){
            initializerMap.put(contextPath, newInitializerChannelHandler(contextPath));
        }
        return initializerMap;
    }

    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler(String contextPath) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("HttpCodec", new HttpServerCodec(4096, 8192, 8192, false)); //HTTP编码解码Handler
                pipeline.addLast("Aggregator", new HttpObjectAggregator(512 * 1024));  // HTTP聚合，设置最大消息值为512KB
                pipeline.addLast("ServletCodec",new NettyServletCodecHandler(servletContextMap.get(contextPath)) ); //处理请求，读入数据，生成Request和Response对象
                pipeline.addLast(executorGroupMap.get(contextPath), "Dispatcher", dispatcherHandlerMap.get(contextPath)); //获取请求分发器，让对应的Servlet处理请求，同时处理404情况
            }
        };
    }

    @Override
    public void start() throws EmbeddedServletContainerException {

        Set<String> keySet = servletContextMap.keySet();
        for(String contextPath : keySet){
            start(servletContextMap.get(contextPath));
        }

    }

    private void start(ServletContext servletContext){
        //加载/build目录下的资源
        loadResources(servletContext);
        //初始化servlet
        initServlet(servletContext);
        servletContext.setInitialized(true);

        String serverInfo = servletContext.getServerInfo();

        Thread serverThread = new Thread(this);
        String threadName = servletContext.getFile() == null ? "" : servletContext.getFile().getName();
        serverThread.setName(threadName);
        serverThread.setUncaughtExceptionHandler((thread,throwable)->{
            //
        });
        serverThread.start();
        log.info("启动成功{}[{}]", serverInfo, getPort());
    }

    /**
     * 加载资源
     * @param servletContext
     */
    private void loadResources(ServletContext servletContext){
        try{
            File file = servletContext.getFile();
            if(file != null){
                WebXmlModel webXmlModel = JarResourceParser.parseConfigFromJar(file);
                loadServletFilter(servletContext, webXmlModel);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    @Override
    public void stop() throws EmbeddedServletContainerException {
        destroyServlet();
        super.stop();
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    private void initServlet(){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            try {
                registration.getServlet().init(registration.getServletConfig());
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException(e.getMessage(),e);
            }
        }
    }

    private void initServlet(ServletContext servletContext){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            try {
                registration.getServlet().init(registration.getServletConfig());
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException(e.getMessage(),e);
            }
        }
    }

    private void destroyServlet(){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            registration.getServlet().destroy();
        }
    }

    /**
     * servlet、filter放入容器
     * @param servletContext
     * @param webXmlModel
     * @throws ClassNotFoundException
     */
    private static void loadServletFilter(ServletContext servletContext, WebXmlModel webXmlModel) throws ClassNotFoundException {
        //servlet部分
        WebXmlModel.ServletMappingNode[] servletMappingNodes = webXmlModel.getServletMappingNodes();
        String name = "";
        Map<String, String> mappingNodes = new HashMap<>();
        if(servletContext.getFile() != null){
            name = StrUtil.subBefore(servletContext.getFile().getName(), ".", true);
        }
        if(!name.startsWith("/")){
            name = "/" + name;
        }
        for(WebXmlModel.ServletMappingNode servletMappingNode : servletMappingNodes){

            mappingNodes.put(servletMappingNode.getServletName(), name + servletMappingNode.getUrlPattern());
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

        //filter部分
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
