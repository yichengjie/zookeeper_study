package com.yicj.study.rpc.server.service.impl;

import com.yicj.study.rpc.server.service.IHelloService;


public class HelloServiceImpl implements IHelloService {
	
	@Override
	public String hello(String name) {
		System.out.println("hello ["+name+"]");
		return "return ["+name+"]";
	}

}
