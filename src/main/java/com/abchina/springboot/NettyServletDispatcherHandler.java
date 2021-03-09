package com.abchina.springboot;

import com.abchina.servlet.ServletContext;
import com.abchina.servlet.ServletHttpServletRequest;
import com.abchina.servlet.ServletHttpServletResponse;
import com.abchina.servlet.ServletRequestDispatcher;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@ChannelHandler.Sharable
public class NettyServletDispatcherHandler extends SimpleChannelInboundHandler<ServletHttpServletRequest> {

    private ServletContext servletContext;

    public NettyServletDispatcherHandler(ServletContext servletContext) {
        super();
        this.servletContext = servletContext;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, ServletHttpServletRequest servletRequest) throws Exception {
        ServletHttpServletResponse servletResponse = new ServletHttpServletResponse(ctx, servletContext,servletRequest);

        try {
            ServletRequestDispatcher dispatcher = servletContext.getRequestDispatcher(servletRequest.getRequestURI());
            if (dispatcher == null) {
                servletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            dispatcher.dispatch(servletRequest, servletResponse, DispatcherType.REQUEST);
        } finally {
            if (!servletRequest.isAsyncStarted()) {
                servletResponse.getOutputStream().close();
            }
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(null != cause) {
            cause.printStackTrace();
        }
        if(null != ctx) {
            ctx.close();
        }
    }

}
