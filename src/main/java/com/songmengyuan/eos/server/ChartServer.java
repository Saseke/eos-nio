package com.songmengyuan.eos.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Set;

public class ChartServer {

	private Selector selector;

	private ServerSocketChannel serverSocketChannel;

	private static final int DEFAULT_PORT = 9000;

	private static final String LOGIN_SUCCEEDED = "登陆成功";

	private static final String SEND_SUCCEEDED = "发送成功";

	public static void setUp() throws IOException {
		ChartServer server = new ChartServer();
		System.out.println("服务端开始启动");
		server.listen();
	}

	// 监听
	private void listen() {
		try {
			while (true) {
				// 得到selector
				if (selector.select(10000) != 0) {
					// 得到当前所有有操作的keys
					Set<SelectionKey> keys = selector.selectedKeys();
					keys.forEach(key -> {
						try {
							if (key.isAcceptable()) { // 若key是第一次请求建立连接
								SocketChannel channel = serverSocketChannel.accept();
								// 配置非阻塞
								channel.configureBlocking(false);
								// 将channel注册到selector上
								channel.register(selector, SelectionKey.OP_READ);
								// 第一次客户端进行连接
								System.out.println(channel.getRemoteAddress().toString().substring(1) + "上线");
								sendSuccess(LOGIN_SUCCEEDED, channel);
							}
							if (key.isReadable()) {
								// 如果是要从客户端读取数据的操作
								read(key);
							}
							keys.remove(key);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					});
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从通道中读取相应的数据
	 */
	private void read(SelectionKey key) {
		SocketChannel channel = null;
		try {
			channel = (SocketChannel) key.channel();
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			// 将channel中的数据读取到buffer中，并输出，若buffer中没有数据，read返回0
			while (channel.read(buffer) != 0) {
				String msg = new String(buffer.array());
				System.out.println("客户端 " + msg);
				// 给其他客户端群发信息
				send(msg, channel);
				// 给用户本人发送成功消息
				sendSuccess(SEND_SUCCEEDED, channel);
			}
		}
		catch (Exception e) {
			try {
				if (channel != null) {
					System.out.println(channel.getRemoteAddress() + "离线");
					key.cancel();
					channel.close();
				}
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	/**
	 * 将msg转发给其他的用户
	 */
	private void send(String msg, SocketChannel cur) {
		System.out.println("服务器开始转发消息:" + msg);
		// 获取当前所有在线的用户
		Set<SelectionKey> keys = selector.keys();
		keys.forEach(key -> {
			try {
				// 根据key 获取channel,buffer
				Channel channel = key.channel();
				// 排除ServerSocketChannel
				if (channel instanceof SocketChannel && channel != cur) {
					// 如果是中文字符的话,在utf-8编码中占3个字节,所以保险起见乘了个三
					ByteBuffer buffer = ByteBuffer.allocate(msg.toCharArray().length * 3);
					buffer.put(msg.getBytes());
					buffer.flip();
					((SocketChannel) channel).write(buffer);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void sendSuccess(String msg, SocketChannel cur) {
		try {
			ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
			cur.write(buffer);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ChartServer() throws IOException {
		selector = Selector.open();
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.socket().bind(new InetSocketAddress(DEFAULT_PORT));
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

}
