package com.yicj.study.rpc.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import com.yicj.study.rpc.common.RpcBeanFactory;
import com.yicj.study.rpc.common.ZookeeperUtils;
import com.yicj.study.rpc.server.service.IHelloService;
import com.yicj.study.rpc.server.service.impl.HelloServiceImpl;
import com.yicj.study.rpc.vo.RpcRequest;
import com.yicj.study.rpc.vo.RpcResponse;

//rpc服务端
public class RpcServer {
	 //服务器设定的目录
    private String registryPath = "/registry";
    //接口，这里方便测试用
    private String serviceName = IHelloService.class.getName();
    //地址目录
    private static String addressName = "address";
    //本地地址
    private static String ip = "localhost";
    //监听接口
    public static Integer port = 8000;

    //链接zk
    public ZooKeeper connect() throws Exception {
        ZooKeeper zooKeeper = ZookeeperUtils.connect();
        return zooKeeper;
    }

    //创建节点，也就是访问的，目录
    public void createNode(ZooKeeper zooKeeper) throws Exception {
        if (zooKeeper.exists(registryPath, false) == null) {
            //创建永久目录，接口服务，可以创建永久目录
            zooKeeper.create(registryPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        //----- /registry/com.yicj.demo.rpc.server.service.IHelloService/
        String servicePath = registryPath + "/" +serviceName;
        if (zooKeeper.exists(servicePath, false) == null) {//接口目录
            zooKeeper.create(servicePath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }

        //----- /registry/com.yicj.demo.rpc.server.service.IHelloService/address
        //localhost:8000
        String addressPath = servicePath + "/" +addressName;
        //地址目录，这里ip就是本地的地址，用于tcp链接使用
        //这里创建的是临时目录，当zk服务断连过后，自动删除临时节点
        zooKeeper.create(addressPath, (ip + ":"+ port).getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    //监听过程
    private void accept() throws Exception {
        //当然这里也可以使用netty来进行监听和其他过程
        //这里简化
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            System.out.println("监听中。。。。。。。");
            Socket socket = serverSocket.accept();
            resultData(socket);
        }
    }

    //执行并返回数据
    private void resultData(Socket socket) 
    		throws IOException, ClassNotFoundException, NoSuchMethodException, 
    		IllegalAccessException, InvocationTargetException {
        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        //读取请求的参数
        RpcRequest rpcRequest = (RpcRequest) objectInputStream.readObject();
        //这里从容器中获取bean，当然这里的bean可以自己缓存，独立spring容器之外
        Object bean = RpcBeanFactory.getBean(rpcRequest.getInterfaceName());
        //方法调用
        Method method = bean.getClass().getMethod(rpcRequest.getMethod(), rpcRequest.getParameterTypes());
        Object data = method.invoke(bean, rpcRequest.getParams());
        //返回数据
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setStatus("success");
        rpcResponse.setData(data);
        objectOutputStream.writeObject(rpcResponse);
    }

    public static void main(String[] args) throws Exception {
        //模拟spring容器的加载过程
        RpcBeanFactory.putBean(IHelloService.class.getName(), new HelloServiceImpl());
        RpcServer server = new RpcServer();
        ZooKeeper zooKeeper = server.connect();
        //创建节点，用于地址访问
        server.createNode(zooKeeper);
        //监听，当然多线程更加理想，这里只显示效果
        server.accept();
    }
	
}
