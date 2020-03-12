package com.songmengyuan.eos.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChartClient {

	private String host;

	private Integer port;

	private SocketChannel socketChannel;

	private String username;

	private ChartClient() throws IOException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("请输入服务器IP地址: ");
		String tmpHost = scanner.next();
		Pattern pattern = Pattern
				.compile("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}");
		while (verify(tmpHost, pattern)) {
			System.out.println("请输入正确的服务器IP地址: ");
			tmpHost = scanner.next();
		}
		host = tmpHost;
		System.out.println("请输入服务器端口: ");
		String tmpPort = scanner.next();
		pattern = Pattern.compile("([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{4}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])");
		while (verify(tmpPort, pattern)) {
			System.out.println("请输入正确的port ");
			tmpPort = scanner.next();
		}
		port = Integer.valueOf(tmpPort);
		socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
		socketChannel.configureBlocking(false);
		username = socketChannel.getLocalAddress().toString().substring(1);
	}

	private boolean verify(String str, Pattern pattern) {
		Matcher matcher = pattern.matcher(str);
		return !matcher.matches();
	}

	private void send(String msg) {
		msg = username + "   :  " + msg;
		try {
			socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
		}
		catch (IOException e) {
			System.out.println("发送失败,请重试");
			e.printStackTrace();
		}
	}

	private void read() {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			while (socketChannel.read(buffer) != 0) {
				System.out.println(new String(buffer.array()));
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void setUp() throws IOException {
		ChartClient client = new ChartClient();
		new Thread(() -> {
			while (true) {
				client.read();
				try {
					Thread.sleep(3000);
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		Scanner scanner = new Scanner(System.in);
		while (scanner.hasNext()) {
			client.send(scanner.next());
		}
	}

}
