package com.blin.btrack;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Style;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.baidu.frontia.api.FrontiaPushMessageReceiver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Push消息?理receiver。???您需要的回?函?， 一般??： onBind是必?的，用??理startWork返回值；
 * onMessage用?接收透?消息； onSetTags、onDelTags、onListTags是tag相?操作的回?；
 * onNotificationClicked在通知被???回?； onUnbind是stopWork接口的返回值回?
 *
 * 返回值中的errorCode，解?如下：
 *  0 - Success
 *  10001 - Network Problem
 *  30600 - Internal Server Error
 *  30601 - Method Not Allowed
 *  30602 - Request Params Not Valid
 *  30603 - Authentication Failed
 *  30604 - Quota Use Up Payment Required
 *  30605 - Data Required Not Found
 *  30606 - Request Time Expires Timeout
 *  30607 - Channel Token Timeout
 *  30608 - Bind Relation Not Found
 *  30609 - Bind Number Too Many
 *
 * ?您遇到以上返回???，如果解?不了您的??，?用同一?求的返回值requestId和errorCode?系我?追查??。
 *
 */
public class MessageReceiver extends FrontiaPushMessageReceiver{

	public static final String ACTION_COMMUNICATION = "ACTION_COMMUNICATION";
	
	private List<Map<String, String>> msgList = new ArrayList<Map<String, String>>();
	
	Gson gson;
	int i;
	
	/**
	 * ?用PushManager.startWork后，sdk??pushserver?起?定?求，
	 * ???程是异步的。?定?求的?果通?onBind返回。 如果您需要用?播推送，
	 * 需要把?里?取的channelid和user id上?到?用server中，
	 * 再?用server接口用channel id和user id???手机或者用?推送。
	 */
	@Override
	public void onBind(Context context, int errorCode, String appid, String userId, String channelId, String requestId) {
		 String responseString = "onBind errorCode=" + errorCode + " appid="
	                + appid + " userId=" + userId + " channelId=" + channelId
	                + " requestId=" + requestId;
		PushApplication app =  PushApplication.getInstance();
		gson = app.getGson();
		app.setUserId(userId);
		app.setChannelId(channelId);
		Log.i("MessageReceiver#onBind", responseString);
		//sendData(context, "onBind","用户id："+ userId+"；通道Id:"+channelId);
		sendOnBind(context,errorCode, userId, channelId);
	}

	private void sendOnBind(Context context, int errorCode,String userId, String channelId) {
		Intent intent = new Intent(ACTION_COMMUNICATION);
		Bundle bindData = new Bundle();
		bindData.putInt("errorCode", errorCode);
		bindData.putString("userId", userId);
		bindData.putString("channelId", channelId);
		intent.putExtra("onBind", bindData);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	@Override
	public void onMessage(Context arg0, String message, String customContentString) {
		String messageString = "传透消息 message=\"" + message
                + "\" customContentString=" + customContentString;
		Log.i("MessageReceiver", messageString);
     /*   Toast.makeText(arg0, message.toString(),
                Toast.LENGTH_SHORT).show();*/
        Gson mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
				.create();
		try {
			Message msg = mGson.fromJson(message, Message.class);
			if (TextUtils.isEmpty(msg.getUser_id())) {
				sendSimpleMessage(arg0, message);
			}else {
				Log.i("MessageReceiver", msg.getUser_id()+"<=>"+PushApplication.getInstance().getUserId());
				if (!msg.getUser_id().equals(PushApplication.getInstance().getUserId())) {
					deliverMessage(arg0, "onMessage",msg);
				
					notif(arg0, msg);
				}
			}
			
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			sendSimpleMessage(arg0, message);
		}
		
	}
	
	private void sendSimpleMessage(Context arg0, String message) {
		deliverSimpleMessage(arg0, "onMessage", message);
		Toast.makeText(arg0, "百度后台消息："+message, Toast.LENGTH_SHORT).show();
	}
	
	Style newInboxStyle(String userNumber,List<Map<String, String>> msgList) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(userNumber);
        List<String> peopleMsgs = new ArrayList<String>();
        for (Map<String, String> map : msgList) {
        	for (Map.Entry<String, String> entry: map.entrySet()) {
				if (entry.getKey().equals(userNumber)) {
					peopleMsgs.add(entry.getValue());
					inboxStyle.addLine(entry.getValue());
					break;
				}
			}
		}
        inboxStyle.setSummaryText(peopleMsgs.get(peopleMsgs.size()-1));
        return inboxStyle;
    }


	private void notif(Context context,String ticker,String text) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setTicker(ticker);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle(ticker);
		mBuilder.setContentText(text);
		NotificationManager mNotifyMgr =  
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(i++, mBuilder.build(
				));                                                                     
		//Toast.makeText(arg0, msg.getMessage(), Toast.LENGTH_SHORT).show();
	}
	
	private void notif(Context context,Message msg ) {
		long uid = Long.parseLong(msg.getUser_id());
		String userNumber = "No."+msg.getUser_id().substring(msg.getUser_id().length()-4);
		Map<String, String> map = new HashMap<String, String>();
		map.put(msg.getUser_id(), msg.getMessage());
		msgList.add(map);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
		mBuilder.setTicker("您有新的消息哦！");
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle(userNumber+"??消息");
		if (msgList.size()>1) {
			mBuilder.setStyle(newInboxStyle(userNumber, msgList));
		}else {
			mBuilder.setContentText(msg.getMessage());
		}
		NotificationManager mNotifyMgr = 
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify((int) uid, mBuilder.build());
		//Toast.makeText(arg0, msg.getMessage(), Toast.LENGTH_SHORT).show();
	}
	
	private void sendData(Context context,String key,String value) {
		Intent intent = new Intent(ACTION_COMMUNICATION);
		intent.putExtra(key, value);
		context.getApplicationContext().sendBroadcast(intent);
	}
	
	private void deliverMessage(Context context,String key,Message msg) {
		Intent intent = new Intent(ACTION_COMMUNICATION);
		intent.putExtra(key, msg);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	private void deliverSimpleMessage(Context context,String key,String msg) {
		Intent intent = new Intent(ACTION_COMMUNICATION);
		intent.putExtra(key, msg);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	
	@Override
	public void onDelTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
		
	}

	@Override
	public void onListTags(Context arg0, int arg1, List<String> arg2, String arg3) {
		
	}

	@Override
	public void onNotificationClicked(Context arg0, String arg1, String arg2, String arg3) {
		
	}

	@Override
	public void onSetTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
		StringBuilder sb = new StringBuilder();
		if (arg1==0) {
			sb.append("设置成功的tag:");
			for (String string : arg2) {
				sb.append(string).append(";");
			}
		} else {
			sb.append("设置失败的tag:");
			for (String string : arg3) {
				sb.append(string).append(";");
			}
		}
		sendData(arg0, "onSetTags",sb.toString());
	}

	@Override
	public void onUnbind(Context arg0, int arg1, String arg2) {
		
	}

}
