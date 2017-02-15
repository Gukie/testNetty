# testNetty

test netty

---

NOTE following code will not work.
```
@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.writeAndFlush("Server response: " + msg);
}
```

Reason:
```
java.lang.UnsupportedOperationException: unsupported message type: String (expected: ByteBuf, FileRegion)
```

The supported  type reside in AbstractChannel::filterOutboundMessage():
```
 @Override
    protected final Object filterOutboundMessage(Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (buf.isDirect()) {
                return msg;
            }

            return newDirectBuffer(buf);
        }

        if (msg instanceof FileRegion) {
            return msg;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }
```


---

NettyServer:
```
ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new NettyPrintServerHandler());
                            pipeline.addLast(new NettyEchoServerHandler());
                            //                            pipeline.addLast(new NettyTimeServerHandler());

                        }
                    }).option(ChannelOption.SO_BACKLOG, 128) // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
```

NettyPrintServerHandler::channelRead
```
@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.write(Unpooled.copiedBuffer("Print Server echo : ", CharsetUtil.UTF_8));
        ctx.write(msg);
        ctx.write(Unpooled.copiedBuffer("\n", CharsetUtil.UTF_8));
        ctx.flush();
        
        ctx.fireChannelRead(msg); // go to next Handler.
    }
```

NettyEchoServerHandler::channelRead
```
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ctx.write(Unpooled.copiedBuffer("Echo Server received: ", CharsetUtil.UTF_8));
        ctx.write(msg);
        ctx.flush();
        //        ctx.fireChannelRead(msg);
    }
```


Above code will throw following exception since the msg is consumed by first handler which is:NettyPrintServerHandler.

```
二月 15, 2017 11:39:55 上午 io.netty.util.ReferenceCountUtil safeRelease
警告: Failed to release a message: PooledUnsafeDirectByteBuf(freed)
io.netty.util.IllegalReferenceCountException: refCnt: 0, decrement: 1
	at io.netty.buffer.AbstractReferenceCountedByteBuf.release0(AbstractReferenceCountedByteBuf.java:101)
	at io.netty.buffer.AbstractReferenceCountedByteBuf.release(AbstractReferenceCountedByteBuf.java:89)
	at io.netty.util.ReferenceCountUtil.release(ReferenceCountUtil.java:84)
	at io.netty.util.ReferenceCountUtil.safeRelease(ReferenceCountUtil.java:109)
	at io.netty.channel.ChannelOutboundBuffer.remove0(ChannelOutboundBuffer.java:292)
	at io.netty.channel.ChannelOutboundBuffer.failFlushed(ChannelOutboundBuffer.java:617)
	at io.netty.channel.AbstractChannel$AbstractUnsafe.flush0(AbstractChannel.java:869)
	at io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.flush0(AbstractNioChannel.java:362)
	at io.netty.channel.AbstractChannel$AbstractUnsafe.flush(AbstractChannel.java:823)
	at io.netty.channel.DefaultChannelPipeline$HeadContext.flush(DefaultChannelPipeline.java:1296)
	at io.netty.channel.AbstractChannelHandlerContext.invokeFlush0(AbstractChannelHandlerContext.java:777)
	at io.netty.channel.AbstractChannelHandlerContext.invokeFlush(AbstractChannelHandlerContext.java:769)
	at io.netty.channel.AbstractChannelHandlerContext.flush(AbstractChannelHandlerContext.java:750)
	at com.test.handler.inbound.NettyEchoServerHandler.channelRead(NettyEchoServerHandler.java:69)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:363)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:349)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:341)
	at com.test.handler.inbound.NettyPrintServerHandler.channelRead(NettyPrintServerHandler.java:62)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:363)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:349)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:341)
	at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1334)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:363)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:349)
	at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:926)
	at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:129)
	at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:642)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:565)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:479)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:441)
	at io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:858)
	at io.netty.util.concurrent.DefaultThreadFactory$DefaultRunnableDecorator.run(DefaultThreadFactory.java:144)
	at java.lang.Thread.run(Thread.java:745)

print server completed.

```

 How to resolve:
 copy the msg in the first handler before flush and pass it to next handler, which is NettyEchoServerHandler
  
NettyPrintServerHandler::channelRead

```
@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf param = Unpooled.copiedBuffer((ByteBuf) msg);
        ctx.write(Unpooled.copiedBuffer("Print Server echo : ", CharsetUtil.UTF_8));
        ctx.write(msg);
        ctx.write(Unpooled.copiedBuffer("\n", CharsetUtil.UTF_8));
        ctx.flush();

        ctx.fireChannelRead(param);
    }
```
 
 