package com.cyf.bigdata.zkdist;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.Stat;

public class DistributedServer {
	private static final String connectString = "shizhan01:2181,shizhan02:2181,shizhan03:2181";
	private static final int sessionTimeout = 2000;
	private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
	private static final String parentNode = "/Servers";
	ZooKeeper zk = null;

	/**
	 * 获取连接
	 * 
	 * @throws Exception
	 */
	public void getConnect() throws Exception {
		zk = new ZooKeeper(connectString, sessionTimeout, new Watcher() {

			@Override
			public void process(WatchedEvent event) {
				
				//收到事件通知后的回调函数(应该是我们自己的事件处理逻辑)
				System.out.println(event.getType() + "-----" + event.getPath());
				connectedSemaphore.countDown();
			}
		});
		if (States.NOT_CONNECTED == zk.getState()) {
			connectedSemaphore.wait();
		}
	}

	/**
	 * 服务器启动时向zookeeper注册监听
	 * 
	 * @param hostName
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void registerServer(String hostName) throws KeeperException, InterruptedException {
		// 先判断 parentNode节点是否存在
		Stat state = zk.exists(parentNode, true);
		if (state == null) {
			zk.create(parentNode, "server parent node".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
		}
		String create = zk.create(parentNode + "/server", hostName.getBytes(), Ids.OPEN_ACL_UNSAFE,
				CreateMode.EPHEMERAL_SEQUENTIAL);
		System.out.println(hostName + " is online...." + create);
	}

	/**
	 * 处理业务功能
	 * 
	 * @param hostName
	 * @throws InterruptedException
	 */
	public void handleBussiness(String hostName) throws InterruptedException {
		System.out.println(hostName + " is start working");
		Thread.sleep(Long.MAX_VALUE);
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		DistributedServer server = new DistributedServer();
		// 获取连接
		server.getConnect();
		// 注册服务
		server.registerServer(args[0]);
		// 处理业务
		server.handleBussiness(args[0]);

	}

}
