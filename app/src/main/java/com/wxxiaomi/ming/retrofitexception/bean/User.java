package com.wxxiaomi.ming.retrofitexception.bean;

import java.io.Serializable;


public class User implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int id;
	public String username;
	public String password;
	public UserCommonInfo userCommonInfo;
	
	public static class UserCommonInfo implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public int userid;
		public String name;
		public String head;
		public String emname;
		@Override
		public String toString() {
			return "UserCommonInfo [userid=" + userid + ", name=" + name
					+ ", head=" + head + ", emname=" + emname + "]";
		}
		
		
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", username=" + username + ", password="
				+ password + ", userCommonInfo=" + userCommonInfo + "]";
	}
	
	
}
