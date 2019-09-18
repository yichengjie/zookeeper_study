package com.yicj.study.zoo;

import java.util.concurrent.CountDownLatch;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

//分布式配置中心demo
public class ZookeeperProSync implements Watcher {

	private static CountDownLatch connectedSemaphore = new CountDownLatch(1) ;
	private static ZooKeeper zk = null ;
	private static Stat stat = new Stat() ;
	
	public static void main(String[] args) throws Exception {
		//zookeeper配置数据库存放路径
		String path ="/username" ;
		//连接zookeeper并且注册一个默认的监听器
		zk = new ZooKeeper("127.0.0.1:2181", 5000, new ZookeeperProSync()) ;
		//等待zk连接成功的通知
		connectedSemaphore.await(); 
		//获取path目录节点的配置数据，并注册默认监听器
		System.out.println(new String(zk.getData(path, true, stat)));
		Thread.sleep(Integer.MAX_VALUE);
	}
	
	@Override
	public void process(WatchedEvent event) {
		//zk连接成功
		if(KeeperState.SyncConnected == event.getState()) {
			if(EventType.None == event.getType() && null == event.getPath()) {
				connectedSemaphore.countDown(); 
			}else if(event.getType() == EventType.NodeDataChanged) {
				try {
					String info = new String(zk.getData(event.getPath(), true, stat)) ;
					System.out.println(info);
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
		
	}

}
