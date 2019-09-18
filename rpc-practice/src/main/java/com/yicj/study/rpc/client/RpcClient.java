package com.yicj.study.rpc.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.zookeeper.ZooKeeper;

import com.yicj.study.rpc.common.RpcBeanFactory;
import com.yicj.study.rpc.common.ZookeeperUtils;
import com.yicj.study.rpc.server.service.IHelloService;
import com.yicj.study.rpc.vo.RpcRequest;
import com.yicj.study.rpc.vo.RpcResponse;

// rpc客户端
public class RpcClient {
	//缓存地址
    private Map<String, List<String>> adressMap = new ConcurrentHashMap<>();
    //链接zk
    public ZooKeeper connect() throws Exception {
        return ZookeeperUtils.connect();
    }
    /**
     * 这里主要是创建代理，利用代理接口实现来达到调用的目的
     * @param interfaceName
     * @return
     * @throws ClassNotFoundException
     */
    public Object createProxy(final String interfaceName) throws ClassNotFoundException {
        //使用线程实例化，主要考虑处理性
        final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(interfaceName);
        //创建代理
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new InvocationHandler() {
            //用重连计数
            private int num = 5;
            @Override
            public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
                //发送请求需要的请求参数
                RpcRequest rpcRequest = new RpcRequest();
                //接口名称
                rpcRequest.setInterfaceName(interfaceName);
                //调用方法名
                rpcRequest.setMethod(method.getName());
                //对应参数类型
                rpcRequest.setParameterTypes(method.getParameterTypes());
                //对应参数
                rpcRequest.setParams(params);
                //返回响应结果
                return sendData(rpcRequest);
            }

            //tcp方式调用
            private Object sendData(RpcRequest rpcRequest) throws Exception {
                //当访问地址存在时
                if (adressMap.containsKey(interfaceName)) {
                    List<String> adresses = adressMap.get(interfaceName);
                    if (adresses != null && !adresses.isEmpty()) {
                        //如果存在多个地址，使用可以调通的一个
                        for (String adress:adresses) {
                            //这个是注册zk的时候设定的数据
                            String[] strs = adress.split(":");
                            //这里简易版的实现，所以直接使用的socket.
                            //实际上可以采用netty框架编写，保存channel访问就可以了，也可以对数据进行加解密
                            Socket socket = new Socket();
                            try {
                                //连接可以访问zk，如果连接失败直接抛出异常，循环下一个地址
                                socket.connect(new InetSocketAddress(strs[0], Integer.valueOf(strs[1])));
                                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                                objectOutputStream.writeObject(rpcRequest);
                                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                                //这里是响应数据，也就是执行过后的数据。远程执行结果
                                RpcResponse rpcResponse = (RpcResponse) objectInputStream.readObject();
                                return rpcResponse.getData();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        //设置重连机制，跳出循环
                        if (num == 0) {
                            throw new RuntimeException("server connect fail");
                        }
                        num--;
                        //如果多个地址还是不能访问，则从zk上面更新地址
                        getAddress(interfaceName);
                        //如果多个地址还是不能访问，则重新访问
                        sendData(rpcRequest);
                    }
                    throw new RuntimeException("not found service");
                }else {
                    //如果没有地址存储时的访问，主要是请求链接问题
                    if (num == 0) {
                        throw new RuntimeException("not found server");
                    }
                    num--;
                    getAddress(interfaceName);
                    return sendData(rpcRequest);
                }
            }
        });
    }

    //从zk获取最新的地址
    private void getAddress(String interfaceName) throws Exception {
        //链接zk
        ZooKeeper zooKeeper = connect();
        //设定的地址目录
        String interfacePath = "/registry/" + interfaceName;
        //获取地址目录
        List<String> addresses = zooKeeper.getChildren(interfacePath, false);
        
        if (addresses != null && !addresses.isEmpty()) {
            List<String> datas = new ArrayList<>();
            for (String address:addresses) {
                //获取数据，也就是配置对应的访问地址
                byte[] bytes = zooKeeper.getData(interfacePath + "/" + address, false, null);
                if (bytes.length > 0) {
                    //放入数组
                    datas.add(new String(bytes));
                }
            }
            //加入缓存
            adressMap.put(interfaceName, datas);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException, InterruptedException {
    	RpcClient client = new RpcClient();
        //这一步是模拟spring容器放入bean的过程，实际用spring容器可自定义标签实现
        RpcBeanFactory.putBean("helloService", client.createProxy(IHelloService.class.getName()));
        //获取bean的过程，实际上实现是代理接口的实现
        IHelloService testService = (IHelloService) RpcBeanFactory.getBean("helloService");
        //调用方法，也就是调用代理的过程
        String test = testService.hello("yicj");
        System.out.println(test);
    }
}
