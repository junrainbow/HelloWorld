package test;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;


/**
 * @author junrainbow
 * @Description
 * @Date:Create in 2018-07-10 15:23
 */
public class NettyTimeClient {

        public static void main(String[] args) throws Exception {
            new NettyTimeClient().connect(8080,"127.0.0.1");
        }

        public void connect(int port,String host) throws InterruptedException {
            //配置客户端NIO线程组
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap b = new Bootstrap();
                b.group(group);
                b.channel(NioSocketChannel.class);
                b.option(ChannelOption.TCP_NODELAY, true);
                b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel sc) throws Exception {
                        sc.pipeline().addLast(new LineBasedFrameDecoder(1024));
                        sc.pipeline().addLast(new StringDecoder());
                        sc.pipeline().addLast(new TimeClientHandler());
                    }
                });

                //发起异步连接操作
                ChannelFuture f = b.connect(host,port).sync();

                //等待客户都按链路关闭
                f.channel().closeFuture().sync();
            }finally {
                group.shutdownGracefully();
            }
        }


    public class TimeClientHandler extends ChannelHandlerAdapter{

            private int counter;

            private ByteBuf firstMessage;

            private byte[] req;

            public TimeClientHandler(){
                req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
                firstMessage = Unpooled.buffer(req.length);
                firstMessage.writeBytes(req);
            }

            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                ByteBuf message = null;
                for (int i = 0; i < 100; i++) {
                    message = Unpooled.buffer(req.length);
                    message.writeBytes(req);
                    ctx.writeAndFlush(message);
                }
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //            ByteBuf buf = (ByteBuf) msg;
        //            byte[] req = new byte[buf.readableBytes()];
        //            buf.readBytes(req);
                String body = (String)msg;
                System.out.println("Now is : " + body +" "+ ++counter);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                ctx.close();
            }
    }
}
