package com.abchina.core;

import com.abchina.util.NamespaceUtil;
import com.abchina.util.ProxyUtil;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFactory;

/**
 *
 */
public class NioServerChannelFactory implements ChannelFactory<NioServerSocketChannel> {

    @Override
    public NioServerSocketChannel newChannel() {
        try {
            NioServerSocketChannel myNioServerSocketChannel = ProxyUtil.newProxyByCglib(NioServerSocketChannel.class,
                    toString() + "-"+ NamespaceUtil.newIdName(this,"serverSocketChannel"),
                    true);

            return myNioServerSocketChannel;
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class nioServerSocketChannel", t);
        }
    }

    @Override
    public String toString() {
        return NamespaceUtil.getIdNameClass(this,getClass().getSimpleName());
    }

}