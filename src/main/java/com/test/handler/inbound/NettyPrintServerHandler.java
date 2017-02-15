/**
 * hongshiwl.com Inc.
 * Copyright (c) 2015-2017 All Rights Reserved.
 */
package com.test.handler.inbound;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * 
 * @author Gushu
 * @version $Id: NettyServerHandler.java, v 0.1 2017年2月14日 下午1:37:44 Gushu Exp $
 */
public class NettyPrintServerHandler extends ChannelInboundHandlerAdapter {

    /** 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        //        int count = 0;
        //        while (count < 20) {
        //            ctx.writeAndFlush(Unpooled.copiedBuffer("print Server sent: " + count + "\n",
        //                CharsetUtil.UTF_8));
        //            Thread.sleep(10000);
        //            count++;
        //        }
    }

    /** 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //        System.out.println("Server ChannelRead:" + msg);
        //        //        ctx.write("from Server:" + msg);
        //        ctx.writeAndFlush("from Server:" + msg);

        //        ByteBuf in = (ByteBuf) msg;
        //        try {
        //            while (in.isReadable()) { // (1)
        //                System.out.print((char) in.readByte());
        //                System.out.flush();
        //            }
        //        } finally {
        //            //            ReferenceCountUtil.release(msg); // (2)
        //        }
        //        System.out.println("print Server handler");
        //        ctx.writeAndFlush(Unpooled.copiedBuffer("Print Server echo \n", CharsetUtil.UTF_8));
        ByteBuf param = Unpooled.copiedBuffer((ByteBuf) msg);
        ctx.write(Unpooled.copiedBuffer("Print Server echo : ", CharsetUtil.UTF_8));
        ctx.write(msg);
        ctx.write(Unpooled.copiedBuffer("\n", CharsetUtil.UTF_8));
        ctx.flush();

        ctx.fireChannelRead(param);
    }

    /** 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelReadComplete(io.netty.channel.ChannelHandlerContext)
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("print server completed.");
        //        ctx.flush();
        //        super.channelReadComplete(ctx);
    }

    /** 
     * @see io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.writeAndFlush("from Server, exception found:" + cause);
        //        super.exceptionCaught(ctx, cause);
    }
}
