package com.yicj.study.rpc.vo;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcResponse implements Serializable{
	private static final long serialVersionUID = 1L;
	//返回状态，当然这里可以，加入对应的异常等（这里简化）
    private String status;
    //返回的数据
    private Object data;
}
