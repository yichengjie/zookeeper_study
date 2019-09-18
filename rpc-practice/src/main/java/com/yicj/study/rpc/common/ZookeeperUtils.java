package com.yicj.study.rpc.common;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class ZookeeperUtils {
	private static ZooKeeper zooKeeper = null;

	public static ZooKeeper connect() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		// 连接zk
		zooKeeper = new ZooKeeper("localhost:2181", 60000, watchedEvent -> {
			if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
				latch.countDown();
			}
		});
		// 无连接阻塞
		latch.await();
		return zooKeeper;
	}
}
