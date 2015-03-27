package com.blin.btrack;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class SetTagTask {

	private BaiduPush mBaiduPush;
	private String mMessage;
	private Handler mHandler;
	private SetTagsTask mTask;
	private String mUserId;
	private OnSetTagsListener mListener;
	public String tag;
	public String userid;

	public interface OnSetTagsListener {
		void sendSetTagsScuess();
	}

	public void setOnSetTagsListener(OnSetTagsListener listener) {
		this.mListener = listener;
	}
	
	
	
	public SetTagTask(String tag, String userid) {
		super();
		this.tag = tag;
		this.userid = userid;
		mBaiduPush = PushApplication.getInstance().getBaiduPush();
	}



	// ?送
	public void setTags() {
		//TODO 需判?有?有网?
		mTask = new SetTagsTask();
		mTask.execute();
	}

	// 停止
	public void stop() {
		if (mTask != null)
			mTask.cancel(true);
	}
	
	class SetTagsTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... message) {
			String result = "";
				result = mBaiduPush.SetTag(tag, userid);
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			Log.i("SetTagsTask",result);
				if (mListener != null)
					mListener.sendSetTagsScuess();
		}
	}
}
