package com.cyf.bigdata.zk;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

/**
 * @todo  关于zookeeper的增删改查数据api使用
 * @author cyfIverson
 * @date 2018年1月24日
 */
public class SimpleZkClient {
	private static final String connectString = "shizhan01:2181,shizhan02:2181,shizhan03:2181";
	private static final int sessionTimeout = 2000;
	ZooKeeper zkClient = null;
	private static CountDownLatch connectedSemaphore = new CountDownLatch( 1 );  
	
	@Before
	public void init() throws IOException, InterruptedException {
		zkClient = new ZooKeeper(connectString, sessionTimeout, new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				//收到事件通知后的回调函数(应该是我们自己的事件处理)
				System.out.println(event.getType()+"-----"+event.getPath());
				connectedSemaphore.countDown();
				
				//循环注册监听
				try {
					zkClient.getChildren("/", true);
				} catch (KeeperException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
		if(States.NOT_CONNECTED==zkClient.getState()) {
			connectedSemaphore.await();
		}
	}
	
	/**
	 * 创建znode
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	@Test
	public void create() throws KeeperException, InterruptedException {
		// param1:要创建节点的路径    param2:节点的数据   param3：节点的权限   param4：节点的类型
		// param2：上传的数据可以是任何类型，但都要转换成byte类型
		zkClient.create("/eclipse", "helloZk".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	}
	
	/**
	 * 判断znode是否存在
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	@Test
	public void testExist() throws KeeperException, InterruptedException {
		Stat stat = zkClient.exists("/eclipse", true);
		System.out.println(stat==null?"not exist":"exist");
	}
	
	/**
	 * 获取znode数据
	 * @throws Exception
	 */
	@Test
	public void getChildren() throws Exception {
		List<String> childern = zkClient.getChildren("/", true);
		for(Object child:childern ) {
			System.out.println(child);
		}
		//主要为了测试监听
		Thread.sleep(Long.MAX_VALUE);
	}
	
	/**
	 * 获取znode的数据
	 * @throws KeeperException
	 * @throws InterruptedException
	 */
	@Test
	public void getData() throws KeeperException, InterruptedException {
		byte[] data = zkClient.getData("/eclipse", false, null);
		System.out.println(new String(data));
	}
	
   /**
    * 删除znode
    * @throws InterruptedException
    * @throws KeeperException
    */
	@Test
	public void deleteNode() throws InterruptedException, KeeperException {
		//param2:是指定当前删除的版本，-1表示所有的版本
		zkClient.delete("/uu", -1);
	}
	
	/**
	 * 修改znode数据
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	@Test
	public void setData() throws InterruptedException, KeeperException {
		zkClient.setData("/eclipse", "zookerper api learning".getBytes(), -1);
		byte[] data = zkClient.getData("/eclipse", true, null);
		System.out.println(new String(data));
   }	
}
