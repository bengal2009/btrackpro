package com.blin.btrack;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Style;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.baidu.android.pushservice.PushSettings;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
/**
 * 
 * @ClassName: BackgoundService 
 * @Description: 后台服?，一旦??知道系??机才?onDestroy
 * @author: Bvin
 * @date: 2015年2月27日 上午11:46:58
 */
public class BackgoundService extends Service {
	
	private static final int BACK_GROUND_NOTIFICATION_ID = 1610;
	
	public static final String APP_KEY = "HpXTcldcWrMfUSMmQjCz5g6o";
	public static final String SECRIT_KEY = "D91PCywgL6002tgS4ucsRTGd5gPH1uB1";
	
	private boolean isBaiduPushStarting;//??始???true，?定后?false
	private boolean isBaiduPushStarted;
	private String userId;
	private String channelId;
	private String userNumber;


    BroadcastReceiver commReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("onBind")) {
				Bundle bindData = intent.getBundleExtra("onBind");
				int errorCode =  bindData.getInt("errorCode");
				isBaiduPushStarted = errorCode==0;
				isBaiduPushStarting = !isBaiduPushStarted;
				if (isBaiduPushStarted) {
					String userId = bindData.getString("userId");
					String channelId = bindData.getString("channelId");
                    /**
                     * @ClassName:commReceiver
                     * @Descriptio: 綁定成
                     * @author: Blin
                     */
//                    綁定成功通知
//					issueNotificationWithBind(context, userId, channelId);
				}else {
					issueNotificationWithBindFaild(errorCode);
				}
				
			}else if (intent.hasExtra("onMessage")) {
				String msgLine = "";
				try {
					Message msg = (Message) intent.getSerializableExtra("onMessage");
					String userNumber = "(No."+msg.getUser_id().substring(msg.getUser_id().length()-4)+")";
					Timestamp tt = new Timestamp(msg.getTime_samp());
					msgLine = "收到消息"+tt.getHours()+":"+tt.getMinutes()
							+"："+userNumber+msg.getMessage()+"\n";
					Log.i("onReceive", msgLine);
				} catch (Exception e) {
					Log.i("onReceive", msgLine);
					msgLine = "收到消息"+intent.getStringExtra("onMessage")+"\n";
					ShortcutMeta shortcut = new Gson().fromJson(intent.getStringExtra("onMessage"), ShortcutMeta.class);
					new CreateNetIconShortcutTask().execute(shortcut);
				}
			}
		}
		
	};
	
	BroadcastReceiver networkReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			boolean noNetworkAvailable = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
			Log.i("网路", noNetworkAvailable ? "没有网" : "有网");
			if (noNetworkAvailable) {//离?
				issueNotificationWithNoConnective();
			}else if (!isBaiduPushStarted) {//尚未??推?并且有网了
				if (!isBaiduPushStarting) {//正在??
					launchBaiduPushService();//再?定一次
				}
			}else {
				//不知道?网后在?网，要不要再重新startWork。。。
			}
		}
		
	};

	private int i;
	

	private void createShortcut(Context context,String name,String url,Bitmap bitmap) {  
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");  
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);  
        shortcut.putExtra("duplicate", false);//位置是否重复建
        Intent intent = new Intent();        
        intent.setAction("android.intent.action.VIEW");    
        Uri content_url = Uri.parse(url);   
        intent.setData(content_url);  
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);  
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);  
        context.sendBroadcast(shortcut);  
    }  
	
	/**
	 * 
	 * @ClassName: CreateNetIconShortcutTask 
	 * @Description: ?建快捷方式??是?网??取的异步任?
	 * @author: Bvin
	 * @date: 2015年3月11日 下午1:57:13
	 */
	class CreateNetIconShortcutTask extends AsyncTask<ShortcutMeta, Integer, Bitmap>{

		ShortcutMeta shortcut;
		
		@Override
		protected Bitmap doInBackground(ShortcutMeta... params) {
			shortcut = params[0];
			try {//?取?片
				return getImage(shortcut.iconUrl);
			} catch (Exception e) {
				Log.i("doInBackground", e.getLocalizedMessage());
				return null;
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (result!=null) {
				Log.i("onPostExecute", result.getWidth() + "");
				if (shortcut.sendAsNotification) {
					notifMessage(getApplicationContext(),  shortcut.name, shortcut.openUrl, result);
				}
				createShortcut(getApplicationContext(), shortcut.name,shortcut.openUrl, result);
			}
		}
		
	}
	
	public static Bitmap getImage(String Url) throws Exception {

		try {

			URL url = new URL(Url);
			url.openConnection();

			/*String responseCode = url.openConnection().getHeaderField(0);

			if (responseCode.indexOf("200") < 0)

				throw new Exception("?片文件不存在或路???，??代?：" + responseCode);
*/
			return BitmapFactory.decodeStream(url.openStream());

		} catch (IOException e) {

			throw new Exception(e.getMessage());

		}

	}
	
	/**离??通知*/
	private void issueNotificationWithNoConnective() {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		.setTicker("推送服务离开")
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("后台服务")
		.setContentText(userNumber+"(离开)");
		mBuilder.setStyle(newInboxStyle(userId, channelId));
		Intent intent = new Intent(this, BackgoundService.class);
		intent.putExtra("rebound", true);
		mBuilder.addAction(0, "重送", PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		NotificationManager mNotifyMgr =  
		        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));
	}
	
	/**?用startWork后回?onBind()方法后，?定失??送的通知*/
	private void issueNotificationWithBindFaild(int errorCode) {
		//icon、title、text三要素不可缺少？
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		.setTicker("推送服??定失?")
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("后台服务")
		.setContentText("???"+errorCode);
		Intent intent = new Intent(this, BackgoundService.class);
		intent.putExtra("rebound", true);
		mBuilder.addAction(0, "重发", PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
		NotificationManager mNotifyMgr =  
		        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));
	}
	
	/**?用startWork后回?onBind()方法后，?定成功?送的通知*/
	private void issueNotificationWithBind(Context context,String userId,String channelId) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setContentTitle("后台服务");
		String userNumber = "No."+userId.substring(userId.length()-4);
		this.userId = userId;
		this.userNumber = userNumber;
		this.channelId = channelId;
		mBuilder.setContentText(userNumber);
		mBuilder.setTicker("后台服务已确定");
		mBuilder.setAutoCancel(false);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setOngoing(true);
		mBuilder.setStyle(newInboxStyle(userId, channelId));
		mBuilder.addAction(0, "打开", null);
		mBuilder.addAction(0, "??", null);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.huli));
		NotificationManager mNotifyMgr =  
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));
	}
	
	Style newInboxStyle(String userId,String channelId) {
		
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle("后台服务");
        inboxStyle.setSummaryText("推送服务已确定");
        inboxStyle.addLine("userId: " + userId);
        inboxStyle.addLine("channelId: " + channelId);
        return inboxStyle;
    }
	
	public static void luanch(Context context) {
		Intent service = new Intent(context, BackgoundService.class);
		context.startService(service);
	}
	
	public BackgoundService() {}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("BackgoundService", "onCreate");
		registerMessageCommReceiver();
		launchBaiduPushService();
		registerNetworkReceiver();
		Log.i("PushManager", "startWork");
		notif(getApplicationContext(), "后台服务","推送后台正在绑定！...");
	}
	
	private void registerMessageCommReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MessageReceiver.ACTION_COMMUNICATION);
		LocalBroadcastManager.getInstance(this).registerReceiver(commReceiver, intentFilter);
	}

	private void registerNetworkReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(networkReceiver, intentFilter);
	}
	
	private void launchBaiduPushService() {
		/**?定只?行一次，只有后台服?被?掉才?重新?定*/
		PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY, APP_KEY);
		isBaiduPushStarting = true;
		//打???模式
		PushSettings.enableDebugMode(getApplicationContext(), true);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//notif(getApplicationContext(), "?始?行","后台服?正在?行");
		Log.i("BackgoundService", "onStartCommand" + startId);
		if(intent!=null&&intent.hasExtra("rebound")) {
			if (intent.getBooleanExtra("rebound", false)) {
				launchBaiduPushService();//重新?定
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i("BackgoundService", "onDestroy");
		LocalBroadcastManager.getInstance(this).unregisterReceiver(commReceiver);
		unregisterReceiver(networkReceiver);
		notif(getApplicationContext(), "后台服务结束","后台服务已停止");
	}
	
	private void notif(Context context,String ticker,String text) {
		notif(context, ticker, text, null, null);
	}
	
	private void notif(Context context,String ticker,String text,String userId,String channelId) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setAutoCancel(true);
		mBuilder.setOngoing(true);
		mBuilder.setTicker(ticker);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle("后台服务");
		mBuilder.setContentText(text);
		NotificationManager mNotifyMgr =  
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(BACK_GROUND_NOTIFICATION_ID, mBuilder.build(
				));                                                                     
		//Toast.makeText(arg0, msg.getMessage(), Toast.LENGTH_SHORT).show();
	}
	
	private void notifMessage(Context context,CharSequence ticker, CharSequence text, Bitmap bitmap) {
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
		mBuilder.setTicker(ticker);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setLargeIcon(bitmap);
		mBuilder.setContentTitle(ticker);
		mBuilder.setContentText(text);
		NotificationManager mNotifyMgr =  
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(i++, mBuilder.build(
				));    
	}
}
