package com.cyf.bigdata.dist.shareLock;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.data.Stat;

public class ShareLockClient {

	private static final String connectString = "shizhan01:2181,shizhan02:2181,shizhan03:2181";
	private static final int sessionTimeout = 2000;
	private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
	private String groupNode = "/locks";
	private String subNode = "sub";
	private ZooKeeper zkClient = null;

	// 当前client创建的子节点
	private volatile String thisPath;

	public void getConnect() throws IOException, Exception {
		zkClient = new ZooKeeper(connectString, sessionTimeout, new Watcher() {

			@Override
			public void process(WatchedEvent event) {
				// 获取连接时变化监听
				System.out.println(event.getType() + "----" + event.getPath());

				// 如果监听的父节点的节点发生变化
				if (event.getType() == EventType.NodeChildrenChanged && event.getPath().equals(groupNode)) {
					// 获取节点数据,并监听父节点
					try {
						List<String> zodeList = zkClient.getChildren(groupNode, true);
						Collections.sort(zodeList);
						if (thisPath == zodeList.get(0)) {
							// 访问锁资源
							accessResources();
							// 重新注册节点
							thisPath = zkClient.create(groupNode + "/" + subNode, "nodeData".getBytes(),
									Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		});

		if (States.NOT_CONNECTED == zkClient.getState()) {
			connectedSemaphore.wait();
		}
	}

	public void registerLock() throws Exception {
		// 判断groupNode节点是否存在
		Stat stat = zkClient.exists(groupNode, false);
		if (stat == null) {
			zkClient.create(groupNode, "groupNode".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		}
		// 注册本节点
		String thisPath = zkClient.create(groupNode + "/" + subNode, "nodeData".getBytes(), Ids.OPEN_ACL_UNSAFE,
				CreateMode.EPHEMERAL_SEQUENTIAL);

		// 获取节点数据,并监听父节点
		List<String> zodeList = zkClient.getChildren(groupNode, true);
		Collections.sort(zodeList);
		if (thisPath == zodeList.get(0)) {
			// 访问资源
			accessResources();

			// 重新注册节点
			thisPath = zkClient.create(groupNode + "/" + subNode, "nodeData".getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL_SEQUENTIAL);
		}
	}

	/**
	 * 访问资源，并删除本节点
	 * 
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	public void accessResources() throws Exception {
		System.out.println("gain lock....");
		// doSomething
		try {
			// 睡眠一会，便于观察
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			zkClient.delete(thisPath, -1);
		}
	}

	public static void main(String[] args) throws Exception {
		ShareLockClient slc = new ShareLockClient();
		// 获取连接
		slc.getConnect();
		// 注册锁 获取子节点，比较自己的序号是否是最小的 访问资源
		slc.registerLock();
		// 保持主线程
		Thread.sleep(Long.MAX_VALUE);
	}
}
