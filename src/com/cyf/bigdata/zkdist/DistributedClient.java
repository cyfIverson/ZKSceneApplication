package com.cyf.bigdata.zkdist;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;

public class DistributedClient {
	private static final String connectString = "shizhan01:2181,shizhan02:2181,shizhan03:2181";
	private static final int sessionTimeout = 2000;
	private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
	private static final String parentNode = "/Servers";
	private volatile List<String> serverList;
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
				
				try {
					//重新更新服务器列表，并且注册监听
					getServerList();
				} catch (Exception e) {
					e.printStackTrace();
				}
				connectedSemaphore.countDown();
			}
		});
		if (States.NOT_CONNECTED == zk.getState()) {
			connectedSemaphore.wait();
		}
	}


	public void getServerList() throws KeeperException, InterruptedException {
		//获取Servers的子节点信息，并监听父节点
		List<String> children = zk.getChildren(parentNode, true);
		
		//先创建一个局部的list来放服务器信息
		ArrayList<String> servers = new ArrayList<String>();
		for(String child : children) {
			
			//遍历得到子节点，获取子节点的数据信息
			byte[] data = zk.getData(parentNode+"/"+child, false, null);
			servers.add(new String(data));
		}
		//把servers赋值给成员变量，以提供各业务线程使用
		serverList = servers;
		//打印服务器列表信息
		System.out.println(serverList);
	}
	
	
	/**
	 * 处理业务功能
	 * @param hostName
	 * @throws InterruptedException
	 */
	public void handleBussiness() throws InterruptedException {
		System.out.println("client is start working");
		Thread.sleep(Long.MAX_VALUE);
	}
	
	public static void main(String[] args) throws Exception {
		DistributedClient client = new DistributedClient();
		//获取连接
		client.getConnect();
		//获取服务器列表，并对其进行监听
		client.getServerList();
		//业务功能
		client.handleBussiness();
	}

}
