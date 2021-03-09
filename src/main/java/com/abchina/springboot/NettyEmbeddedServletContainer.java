package com.abchina.springboot;

import com.abchina.core.constants.HttpConstants;
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
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class NettyEmbeddedServletContainer extends AbstractNettyServer implements EmbeddedServletContainer {
    private static Logger log = LoggerFactory.getLogger(NettyEmbeddedServletContainer.class);
    private ServletContext servletContext;
    private EventExecutorGroup dispatcherExecutorGroup;
    private ChannelHandler dispatcherHandler;

    public NettyEmbeddedServletContainer(ServletContext servletContext, int bizThreadCount) {
        super(servletContext.getServerSocketAddress());
        this.servletContext = servletContext;
        this.dispatcherExecutorGroup = new DefaultEventExecutorGroup(bizThreadCount);
        this.dispatcherHandler = new NettyServletDispatcherHandler(servletContext);
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
    public void start() throws EmbeddedServletContainerException {
        //加载/build目录下的资源
        loadResources(servletContext);
        //初始化servlet
        initServlet();
        servletContext.setInitialized(true);

        String serverInfo = servletContext.getServerInfo();

        Thread serverThread = new Thread(this);
        serverThread.setName(serverInfo);
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
            File buildDir = new File(HttpConstants.ROOT_PATH+HttpConstants.BUILD_PATH);
            File[] files;
            if (buildDir.isDirectory() && (files = buildDir.listFiles()) != null) {
                for (File file : files) {
                    WebXmlModel webXmlModel = JarResourceParser.parseConfigFromJar(file);
                    loadServletFilter(servletContext, webXmlModel);
                }
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
