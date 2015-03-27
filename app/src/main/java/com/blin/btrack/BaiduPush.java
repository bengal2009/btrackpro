package com.blin.btrack;;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Map;

import android.util.Log;


public class BaiduPush {
	
	public final static String mUrl = "http://channel.api.duapp.com/rest/2.0/channel/";// 基?url

	public final static String HTTP_METHOD_POST = "POST";
	public final static String HTTP_METHOD_GET = "GET";
	
	public static final String SEND_MSG_ERROR = "send_msg_error";
	
	private final static int HTTP_CONNECT_TIMEOUT = 10000;// ?接超???，10s
	private final static int HTTP_READ_TIMEOUT = 10000;// ?消息超???，10s
	
	public String mHttpMethod;// ?求方式，Post or Get
	public String mSecretKey;// 安全key

	/**
	 * 构造函?
	 * 
	 * @param http_mehtod
	 *            ?求方式
	 * @param secret_key
	 *            安全key
	 * @param api_key
	 *            ?用key
	 */
	public BaiduPush(String http_mehtod, String secret_key, String api_key) {
		mHttpMethod = http_mehtod;
		mSecretKey = secret_key;
		RestApi.mApiKey = api_key;
	}

	/**
	 * url??方式
	 * 
	 * @param str
	 *            指定??方式，未指定默??utf-8
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String urlencode(String str) throws UnsupportedEncodingException {
		String rc = URLEncoder.encode(str, "utf-8");
		return rc.replace("*", "%2A");
	}

	/**
	 * ?字符串???json格式
	 * 
	 * @param str
	 * @return
	 */
	public String jsonencode(String str) {
		String rc = str.replace("\\", "\\\\");
		rc = rc.replace("\"", "\\\"");
		rc = rc.replace("\'", "\\\'");
		return rc;
	}

	/**
	 * ?行Post?求前?据?理，加密之?
	 * 
	 * @param data
	 *            ?求的?据
	 * @return
	 */
	public String PostHttpRequest(RestApi data) {

		StringBuilder sb = new StringBuilder();

		String channel = data.remove(RestApi._CHANNEL_ID);
		if (channel == null)
			channel = "channel";

		try {
			data.put(RestApi._TIMESTAMP,
					Long.toString(System.currentTimeMillis() / 1000));
			data.remove(RestApi._SIGN);

			sb.append(mHttpMethod);
			sb.append(mUrl);
			sb.append(channel);
			for (Map.Entry<String, String> i : data.entrySet()) {
				sb.append(i.getKey());
				sb.append('=');
				sb.append(i.getValue());
			}
			sb.append(mSecretKey);

			// System.out.println( "PRE: " + sb.toString() );
			// System.out.println( "UEC: " + URLEncoder.encode(sb.toString(),
			// "utf-8") );

			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			// md.update( URLEncoder.encode(sb.toString(), "utf-8").getBytes()
			// );
			md.update(urlencode(sb.toString()).getBytes());
			byte[] md5 = md.digest();

			sb.setLength(0);
			for (byte b : md5)
				sb.append(String.format("%02x", b & 0xff));
			data.put(RestApi._SIGN, sb.toString());

			// System.out.println( "MD5: " + sb.toString());

			sb.setLength(0);
			for (Map.Entry<String, String> i : data.entrySet()) {
				sb.append(i.getKey());
				sb.append('=');
				// sb.append(i.getValue());
				// sb.append(URLEncoder.encode(i.getValue(), "utf-8"));
				sb.append(urlencode(i.getValue()));
				sb.append('&');
			}
			sb.setLength(sb.length() - 1);

			// System.out.println( "PST: " + sb.toString() );
			// System.out.println( mUrl + "?" + sb.toString() );

		} catch (Exception e) {
			e.printStackTrace();
			Log.i("PostHttpRequest","PostHttpRequest Exception:" + e.getMessage());
			return SEND_MSG_ERROR;//消息?送失?，返回??，?行重?
		}

		StringBuilder response = new StringBuilder();
		HttpRequest(mUrl + channel, sb.toString(), response);
		return response.toString();
	}

	/**
	 * ?行Post?求
	 * 
	 * @param url
	 *            基?url
	 * @param query
	 *            提交的?据
	 * @param out
	 *            服?器回复的字符串
	 * @return
	 */
	private int HttpRequest(String url, String query, StringBuilder out) {

		URL urlobj;
		HttpURLConnection connection = null;

		try {
			urlobj = new URL(url);
			connection = (HttpURLConnection) urlobj.openConnection();
			connection.setRequestMethod("POST");

			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			connection
					.setRequestProperty("Content-Length", "" + query.length());
			connection.setRequestProperty("charset", "utf-8");

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
			connection.setReadTimeout(HTTP_READ_TIMEOUT);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(query.toString());
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;

			while ((line = rd.readLine()) != null) {
				out.append(line);
				out.append('\r');
			}
			rd.close();

		} catch (Exception e) {
			e.printStackTrace();
			Log.i("HttpRequest","HttpRequest Exception:" + e.getMessage());
			out.append(SEND_MSG_ERROR);//消息?送失?，返回??，?行重?
		}

		if (connection != null)
			connection.disconnect();

		return 0;
	}

	//
	// REST APIs
	//
	/**
	 * 查???信息、?用、用?与百度Channel的?定?系。
	 * 
	 * @param userid
	 *            用?id
	 * @param channelid
	 * @return json形式的服?器恢复
	 */
	public String QueryBindlist(String userid, String channelid) {
		RestApi ra = new RestApi(RestApi.METHOD_QUERY_BIND_LIST);
		ra.put(RestApi._USER_ID, userid);
		// ra.put(RestApi._DEVICE_TYPE, RestApi.DEVICE_TYPE_ANDROID);
		ra.put(RestApi._CHANNEL_ID, channelid);
		// ra.put(RestApi._START, "0");
		// ra.put(RestApi._LIMIT, "10");
		return PostHttpRequest(ra);
	}

	/**
	 * 判???、?用、用?与Channel的?定?系是否存在。
	 * 
	 * @param userid
	 *            用?id
	 * @param channelid
	 * @return
	 */
	public String VerifyBind(String userid, String channelid) {
		RestApi ra = new RestApi(RestApi.METHOD_VERIFY_BIND);
		ra.put(RestApi._USER_ID, userid);
		// ra.put(RestApi._DEVICE_TYPE, RestApi.DEVICE_TYPE_ANDROID);
		ra.put(RestApi._CHANNEL_ID, channelid);
		return PostHttpRequest(ra);
	}

	/**
	 * ?指定用??置??
	 * 
	 * @param tag
	 * @param userid
	 * @return
	 */
	public String SetTag(String tag, String userid) {
		RestApi ra = new RestApi(RestApi.METHOD_SET_TAG);
		ra.put(RestApi._USER_ID, userid);
		ra.put(RestApi._TAG, tag);
		return PostHttpRequest(ra);
	}

	/**
	 * 查??用的所有??
	 * 
	 * @return
	 */
	public String FetchTag() {
		RestApi ra = new RestApi(RestApi.METHOD_FETCH_TAG);
		// ra.put(RestApi._NAME, "0");
		// ra.put(RestApi._START, "0");
		// ra.put(RestApi._LIMIT, "10");
		return PostHttpRequest(ra);
	}

	/**
	 * ?除指定用?的指定??
	 * 
	 * @param tag
	 * @param userid
	 * @return
	 */
	public String DeleteTag(String tag, String userid) {
		RestApi ra = new RestApi(RestApi.METHOD_DELETE_TAG);
		ra.put(RestApi._USER_ID, userid);
		ra.put(RestApi._TAG, tag);
		return PostHttpRequest(ra);
	}

	/**
	 * 查?指定用?的??
	 * 
	 * @param userid
	 * @return
	 */
	public String QueryUserTag(String userid) {
		RestApi ra = new RestApi(RestApi.METHOD_QUERY_USER_TAG);
		ra.put(RestApi._USER_ID, userid);
		return PostHttpRequest(ra);
	}

	/**
	 * 根据channel_id查????型： 1：??器??； 2：pc??； 3：Andriod??； 4：iOS??； 5：wp??；
	 * 
	 * @param channelid
	 * @return
	 */
	public String QueryDeviceType(String channelid) {
		RestApi ra = new RestApi(RestApi.METHOD_QUERY_DEVICE_TYPE);
		ra.put(RestApi._CHANNEL_ID, channelid);
		return PostHttpRequest(ra);
	}

	// Message Push

	private final static String MSGKEY = "msgkey";

	/**
	 * ?指定用?推送消息
	 * 
	 * @param message
	 * @param userid
	 * @return
	 */
	public String PushMessage(String message, String userid) {
		RestApi ra = new RestApi(RestApi.METHOD_PUSH_MESSAGE);
		ra.put(RestApi._MESSAGE_TYPE, RestApi.MESSAGE_TYPE_MESSAGE);
		ra.put(RestApi._MESSAGES, message);
		ra.put(RestApi._MESSAGE_KEYS, MSGKEY);
		// ra.put(RestApi._MESSAGE_EXPIRES, "86400");
		// ra.put(RestApi._CHANNEL_ID, "");
		ra.put(RestApi._PUSH_TYPE, RestApi.PUSH_TYPE_USER);
		// ra.put(RestApi._DEVICE_TYPE, RestApi.DEVICE_TYPE_ANDROID);
		ra.put(RestApi._USER_ID, userid);
		return PostHttpRequest(ra);
	}

	/**
	 * ?指定??用?推送消息
	 * 
	 * @param message
	 * @param tag
	 * @return
	 */
	public String PushTagMessage(String message, String tag) {
		RestApi ra = new RestApi(RestApi.METHOD_PUSH_MESSAGE);
		ra.put(RestApi._MESSAGE_TYPE, RestApi.MESSAGE_TYPE_MESSAGE);
		ra.put(RestApi._MESSAGES, message);
		ra.put(RestApi._MESSAGE_KEYS, MSGKEY);
		// ra.put(RestApi._MESSAGE_EXPIRES, "86400");
		ra.put(RestApi._PUSH_TYPE, RestApi.PUSH_TYPE_TAG);
		// ra.put(RestApi._DEVICE_TYPE, RestApi.DEVICE_TYPE_ANDROID);
		ra.put(RestApi._TAG, tag);
		return PostHttpRequest(ra);
	}

	/**
	 * ?所有用?推送消息
	 * 
	 * @param message
	 * @return
	 */
	public String PushMessage(String message) {
		RestApi ra = new RestApi(RestApi.METHOD_PUSH_MESSAGE);
		ra.put(RestApi._MESSAGE_TYPE, RestApi.MESSAGE_TYPE_MESSAGE);
		ra.put(RestApi._MESSAGES, message);
		ra.put(RestApi._MESSAGE_KEYS, MSGKEY);
		// ra.put(RestApi._MESSAGE_EXPIRES, "86400");
		ra.put(RestApi._PUSH_TYPE, RestApi.PUSH_TYPE_ALL);
		// ra.put(RestApi._DEVICE_TYPE, RestApi.DEVICE_TYPE_ANDROID);
		return PostHttpRequest(ra);
	}

	/**
	 * ?指定用?推送通知
	 * 
	 * @param title
	 * @param message
	 * @param userid
	 * @return
	 */
	public String PushNotify(String title, String message, String userid) {
		RestApi ra = new RestApi(RestApi.METHOD_PUSH_MESSAGE);
		ra.put(RestApi._MESSAGE_TYPE, RestApi.MESSAGE_TYPE_NOTIFY);

		// notification_builder_id : default 0

		// String msg =
		// String.format("{'title':'%s','description':'%s','notification_basic_style':7}",
		// title, jsonencode(message));
		// String msg =
		// String.format("{'title':'%s','description':'%s','notification_builder_id':0,'notification_basic_style':5,'open_type':2}",
		// title, jsonencode(message));
		// String msg =
		// String.format("{'title':'%s','description':'%s','notification_builder_id':2,'notification_basic_style':7}",
		// title, jsonencode(message));

		String msg = String
				.format("{'title':'%s','description':'%s','notification_builder_id':0,'notification_basic_style':7,'open_type':2,'custom_content':{'test':'test'}}",
						title, jsonencode(message));

		// String msg =
		// String.format("{\"title\":\"%s\",\"description\":\"%s\",\"notification_basic_style\":\"7\"}",
		// title, jsonencode(message));
		// String msg =
		// String.format("{\"title\":\"%s\",\"description\":\"%s\",\"notification_builder_id\":0,\"notification_basic_style\":1,\"open_type\":2}",
		// title, jsonencode(message));

		System.out.println(msg);

		ra.put(RestApi._MESSAGES, msg);

		ra.put(RestApi._MESSAGE_KEYS, MSGKEY);
		ra.put(RestApi._PUSH_TYPE, RestApi.PUSH_TYPE_USER);
		// ra.put(RestApi._PUSH_TYPE, RestApi.PUSH_TYPE_ALL);
		ra.put(RestApi._USER_ID, userid);
		return PostHttpRequest(ra);
	}

	/**
	 * ?所有用?推送通知
	 * 
	 * @param title
	 * @param message
	 * @return
	 */
	public String PushNotifyAll(String title, String message) {
		RestApi ra = new RestApi(RestApi.METHOD_PUSH_MESSAGE);
		ra.put(RestApi._MESSAGE_TYPE, RestApi.MESSAGE_TYPE_NOTIFY);

		String msg = String
				.format("{'title':'%s','description':'%s','notification_builder_id':0,'notification_basic_style':7,'open_type':2,'custom_content':{'test':'test'}}",
						title, jsonencode(message));

		System.out.println(msg);

		ra.put(RestApi._MESSAGES, msg);

		ra.put(RestApi._MESSAGE_KEYS, MSGKEY);
		ra.put(RestApi._PUSH_TYPE, RestApi.PUSH_TYPE_ALL);
		return PostHttpRequest(ra);
	}

	/**
	 * 查?指定用?离?消息。
	 * 
	 * @param userid
	 * @return
	 */
	public String FetchMessage(String userid) {
		RestApi ra = new RestApi(RestApi.METHOD_FETCH_MESSAGE);
		ra.put(RestApi._USER_ID, userid);
		// ra.put(RestApi._START, "0");
		// ra.put(RestApi._LIMIT, "10");
		return PostHttpRequest(ra);
	}

	/**
	 * 查?指定用?的离?消息?
	 * 
	 * @param userid
	 * @return
	 */
	public String FetchMessageCount(String userid) {
		RestApi ra = new RestApi(RestApi.METHOD_FETCH_MSG_COUNT);
		ra.put(RestApi._USER_ID, userid);
		return PostHttpRequest(ra);
	}

	/**
	 * ?除离?消息
	 * 
	 * @param userid
	 * @param msgids
	 * @return
	 */
	public String DeleteMessage(String userid, String msgids) {
		RestApi ra = new RestApi(RestApi.METHOD_DELETE_MESSAGE);
		ra.put(RestApi._USER_ID, userid);
		ra.put(RestApi._MESSAGE_IDS, msgids);
		return PostHttpRequest(ra);
	}

}