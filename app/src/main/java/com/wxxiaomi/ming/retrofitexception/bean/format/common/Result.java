package com.wxxiaomi.ming.retrofitexception.bean.format.common;

/**
 * 公共数据格式
 * @author Mr.W
 *
 * @param <T>
 */
public class Result<T> {
	public int state;
	public String error;
	public T infos;

}
