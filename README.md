# testNetty

test netty

---
chinese refer:
http://wiki.jikexueyuan.com/project/netty-4-user-guide/implement-websocket-chat-function.html

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
 
---

###启动服务 代码片段详解
 
```
	public static void server() throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap();

		bootstrap
				.group(BOSSGROUP, WORKERGROUP)
				// 指定NIO传输channel
				.channel(NioServerSocketChannel.class)
				// 在channelPipeline中加入我们自定义的handler 在生成每一个新的channel时调用
				.childHandler(new ChannelInitializer<Channel>() { 

					@Override
					protected void initChannel(Channel channel)
							throws Exception {
						ChannelPipeline pipeline = channel.pipeline();
						pipeline.addLast(new MessageDecoder());
						pipeline.addLast(new MessageEncoder());
						// 配置 心跳机制的间隔时间 读事件60秒，写事件40秒，全部事件30秒
						pipeline.addLast("ping", new IdleStateHandler(60, 40,
								30, TimeUnit.SECONDS));
						// serverHandler如果同时使用一个handler时，一定要注意并发
						pipeline.addLast(new ServerHandler());
					}
				}).option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
		// 异步的绑定服务器 sync一直等到绑定完成
		ChannelFuture f = bootstrap.bind(PORT).sync();
		// 获得这个channel的closeFuture，阻塞当前线程直到关闭操作完成
		f.channel().closeFuture().sync();

		System.out.println("TCP服务器启动完成");

	} 
```

---
### Code snippet 1:  新增一个Channel时，添加ChannelInitializer 监听
```
.childHandler(new ChannelInitializer<Channel>() { // 1
    ...
}
```
这个代码 会在 新建一个 Channel的时候被调用

---
### Code snippet 2: 添加 ChannelHandler
```
ChannelPipeline pipeline = channel.pipeline();
pipeline.addLast(new MessageDecoder()); //inbound
pipeline.addLast(new MessageEncoder()); //outbound
// 配置 心跳机制的间隔时间 读事件60秒，写事件40秒，全部事件30秒
pipeline.addLast("ping", new IdleStateHandler(60, 40,
        30, TimeUnit.SECONDS)); //inbound and outbound
// serverHandler如果同时使用一个handler时，一定要注意并发
pipeline.addLast(new ServerHandler());
```

这段代码的意思是：

    1. 获取 Channel的 pipeline
    2. 在 pipeline 添加 ChannelHandler。 在 DefaultChannelPipeline 中可以看到，其实是会生成一个 AbstractChannelHandlerContext
    3. 这样，最后会形成一个 AbstractChannelHandlerContext 的链。 该链是从 head 开始，tail 结束
    4. 比如上面的code，形成的链如下图：
![此处输入图片的描述][1]



如何处理message( 具体实现可以参考 DefaultChannelPipeline::fireChannelRead() 或者 DefaultChannelPipeline::write() ):

> 
1. read： pipeline会它的head中开始读取它的next，获取所有是 inbound的     AbstractChannelHandlerContext，然后一个一个往下走，直到tail才结束
![此处输入图片的描述][2]

<br>
> 
2. write：pipeline 会它的tail中开始读取它的prev，获取 outbound的 AbstractChannelHandlerContext，然后一个一个往前走，直到head结束。 
![此处输入图片的描述][3]
    


  [1]: http://oksd56xj3.bkt.clouddn.com/netty_whole.png
  [2]: http://oksd56xj3.bkt.clouddn.com/netty_read.png
  [3]: http://oksd56xj3.bkt.clouddn.com/netty_write.png