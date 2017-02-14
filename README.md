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


 