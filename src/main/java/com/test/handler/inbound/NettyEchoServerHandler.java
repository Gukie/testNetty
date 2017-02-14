/**
 * hongshiwl.com Inc.
 * Copyright (c) 2015-2017 All Rights Reserved.
 */
package com.test.handler.inbound;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * 
 * @author Gushu
 * @version $Id: NettyEchoServerHandler.java, v 0.1 2017年2月14日 下午2:35:20 Gushu Exp $
 */
public class NettyEchoServerHandler extends ChannelInboundHandlerAdapter {

    /** 
     * here we can put the business class to impl related task.
     * 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //        super.channelActive(ctx);
        int count = 0;
        while (count < 20) {
            ctx.writeAndFlush(Unpooled.copiedBuffer("Echo Server sent: " + count + "\n",
                CharsetUtil.UTF_8));
            Thread.sleep(5000);
            count++;
        }
    }

    /** 
     * handle the connection lost case.
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    /**
     * ctx.writeAndFlush(String) not work, since msg should not be String. exception: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion)
     * 
     * 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //        System.out.println("Server response: " + msg);
        //        ctx.writeAndFlush("Server response: " + msg); //  not work, since msg should not be String. exception: java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion)
        //        ctx.writeAndFlush(Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
        //        String str = "hello";
        //        str.toCharArray();
        //        
        //        ByteBuf byteBuf = (ByteBuf) msg;
        //        ctx.writeAndFlush(msg);

        ctx.write(Unpooled.copiedBuffer("Server received: ", CharsetUtil.UTF_8));
        ctx.write(msg);
        ctx.flush();

    }

    /** 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelReadComplete(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //        ctx.flush();
        //        System.out.println("completed.");
    }

    /** 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
