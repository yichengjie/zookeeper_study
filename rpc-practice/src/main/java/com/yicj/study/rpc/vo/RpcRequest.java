package com.yicj.study.rpc.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	// 接口名称，用于反射
	private String interfaceName;
	// 调用方法
	private String method;
	// 参数类型
	private Class<?>[] parameterTypes;
	// 参数
	private Object[] params;
}
