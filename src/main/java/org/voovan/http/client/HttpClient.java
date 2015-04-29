package org.voovan.http.client;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.message.Request.RequestType;
import org.voovan.http.message.packet.Header;
import org.voovan.http.message.packet.Part;
import org.voovan.network.aio.AioSocket;
import org.voovan.network.messageparter.HttpMessageParter;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

/**
 * HTTP 请求调用
 * @author helyho
 *
 */
public class HttpClient {
	
	private AioSocket socket;
	private HttpClientHandler clientHandler; 
	private Request request;
	private Map<String, Object> parameters;
	
	
	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 */
	public  HttpClient(String urlString) {
		try {
			URL url = new URL(urlString);
			String hostString = url.getHost();
			int port = url.getPort();
			
			request = new Request();
			//初始化请求参数,默认值
			request.protocol().setPath(url.getPath().isEmpty()?"/":url.getPath());
			request.header().put("Host", hostString);
			request.header().put("Pragma", "no-cache");
			request.header().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			request.header().put("User-Agent", "Http Client");
			request.header().put("Accept-Encoding","gzip,deflate,sdch");
			
			socket = new AioSocket(hostString, port==-1?80:port, 5000);
			parameters = new HashMap<String, Object>();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 设置请求方法
	 * @param method
	 */
	public void setMethod(String method){
		request.protocol().setMethod(method);
	}
	
	/**
	 * 获取请求头集合
	 * @return
	 */
	public Header getHeader(){
		return request.header();
	}
	
	/**
	 * 获取请求参数集合
	 * @return
	 */
	public Map<String,Object> getParameters(){
		return parameters;
	}
	
	/**
	 * 设置请求参数
	 * @param name
	 * @param value
	 * @return
	 */
	public HttpClient putParameters(String name,Object value){
		parameters.put(name, value);
		return this;
	}
	
	/**
	 * 构建QueryString
	 * 	将 Map 集合转换成 QueryString 字符串
	 * @return
	 */
	private String getQueryString(){
		String queryString = "";
		try {
			for (Entry<String, Object> parameter : parameters.entrySet()) {
				queryString += parameter.getKey()
						+ "="
						+ URLEncoder.encode(parameter.getValue().toString(), "UTF-8")
						+ "&";
			}
			queryString = queryString.length()>0?TString.removeSuffix(queryString):queryString;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queryString.isEmpty()? "" :"?"+queryString;
	}
	
	/**
	 * 构建请求
	 */
	private void buildRequest(){

		if (request.getType() == RequestType.GET) {
			String queryString = getQueryString(); 
			request.protocol().setPath(request.protocol().getPath() + queryString);
		}
		else if(request.getType() == RequestType.POST && request.parts().size()!=0){
			
			try{
				for (Entry<String, Object> parameter : parameters.entrySet()) {
					Part part = new Part();
					part.header().put("name", parameter.getKey());
					part.body().write(URLEncoder.encode(parameter.getValue().toString(),"UTF-8").getBytes());
					request.parts().add(part);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		else if(request.getType() == RequestType.POST && request.body()!=null){
			request.body().write(getQueryString().getBytes());
		}
	}
	
	/**
	 * 连接并发送请求
	 * @return
	 * @throws Exception
	 */
	public Response Connect() throws Exception{
		buildRequest();
		
		clientHandler = new HttpClientHandler(request);
		socket.handler(clientHandler);
		socket.filterChain().add(new HttpClientFilter());
		socket.messageParter(new HttpMessageParter());
		Logger.simple(request);
		socket.start();
		
		//等待获取 response并返回
		while(clientHandler.getResponse()==null){
			TEnv.sleep(100);
		}
		
		return clientHandler.getResponse();
	}
	
	public static void main(String[] args) throws Exception {
		HttpClient client = new HttpClient("http://www.sohu.com/");
		client.setMethod("GET");
		long t = System.currentTimeMillis();
		Logger.simple("start :"+System.currentTimeMillis());
		Response response = client.Connect();
		Logger.simple("====");
		Logger.simple(System.currentTimeMillis()-t);
		
		String bodyString = response.body().toString();
		Logger.debug("body length:"+bodyString.length());
	}
}