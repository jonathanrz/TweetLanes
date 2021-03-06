/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tweetalib.android;

import java.util.HashMap;

import org.appdotnet4j.model.AdnUser;
import org.asynctasktex.AsyncTaskEx;

import org.socialnetlib.android.AppdotnetApi;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.model.TwitterUser;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;


public class TwitterFetchUser {

	private FetchUserWorkerCallbacks mCallbacks;
	private HashMap<Long, TwitterUser> mUserIdHashMap;
	private HashMap<String, TwitterUser> mUserScreenNameHashMap;
	private Integer mFetchUserCallbackHandle;
	private HashMap<Integer, FinishedCallback> mFinishedCallbackMap;

	/*
	 * 
	 */
	public void clearCallbacks() {
		mFinishedCallbackMap.clear();
	}
	
	/*
	 * 
	 */
	public interface FetchUserWorkerCallbacks {
		
		public AppdotnetApi getAppdotnetApi();
		public Twitter getTwitterInstance();
	}
	
	/*
	 * 
	 */
	public interface FinishedCallbackInterface {
		
		public void finished(TwitterFetchResult result, TwitterUser user);
		
	}
	
	/*
	 * 
	 */
	public abstract class FinishedCallback implements FinishedCallbackInterface {
		
		static final int kInvalidHandle = -1; 
		
		public FinishedCallback() {
			mHandle = kInvalidHandle;
		}
		
		void setHandle(int handle) {
			mHandle = handle;
		}
		
		private int mHandle;
	}
	
	/*
	 * 
	 */
	public TwitterFetchUser() {
		mFinishedCallbackMap = new HashMap<Integer, FinishedCallback>();
		mFetchUserCallbackHandle = 0;
		mUserIdHashMap = new HashMap<Long, TwitterUser>();
		mUserScreenNameHashMap = new HashMap<String, TwitterUser>();
	}
	
	/*
	 * 
	 */
	public void setWorkerCallbacks(FetchUserWorkerCallbacks callbacks) {
		mCallbacks = callbacks;
	}
	
	/*
	 * 
	 */
	FinishedCallback getFetchStatusesCallback(Integer callbackHandle) {
		FinishedCallback callback = mFinishedCallbackMap.get(callbackHandle);
		return callback;
	}
	
	/*
	 * 
	 */
	void removeFetchStatusesCallback(FinishedCallback callback) {
		if (mFinishedCallbackMap.containsValue(callback)) {
			mFinishedCallbackMap.remove(callback.mHandle);
		}
	}
	
	/*
	 * 
	 */
	Twitter getTwitterInstance() {
		return mCallbacks.getTwitterInstance();
	}
	
	AppdotnetApi getAppdotnetApi() {
		return mCallbacks.getAppdotnetApi();
	}
	
	/*
	 * 
	 */
	public void setUser(TwitterUser twitterUser) {
		if (mUserIdHashMap.containsKey(twitterUser.getId()) == false) {
			mUserIdHashMap.put(twitterUser.getId(), twitterUser);
		}
		if (mUserScreenNameHashMap.containsKey(twitterUser.getScreenName()) == false) {
			mUserScreenNameHashMap.put(twitterUser.getScreenName(), twitterUser);
		}
	}
	
	public void setUser(User user, boolean forceUpdate) {
	    Long userId = Long.valueOf(user.getId());
	    TwitterUser twitterUser = new TwitterUser(user);
	    if (forceUpdate == true || mUserIdHashMap.containsKey(userId) == false) {
	    	mUserIdHashMap.put(userId, twitterUser);
	    }
	    if (forceUpdate == true || mUserIdHashMap.containsKey(twitterUser.getScreenName()) == false) {
	    	mUserScreenNameHashMap.put(twitterUser.getScreenName(), twitterUser);
	    }
	}
	
	public void setUser(AdnUser user, boolean forceUpdate) {
	    Long userId = Long.valueOf(user.mId);
	    TwitterUser twitterUser = new TwitterUser(user);
	    if (forceUpdate == true || mUserIdHashMap.containsKey(userId) == false) {
	    	mUserIdHashMap.put(userId, twitterUser);
	    }
	    if (forceUpdate == true || mUserIdHashMap.containsKey(twitterUser.getScreenName()) == false) {
	    	mUserScreenNameHashMap.put(twitterUser.getScreenName(), twitterUser);
	    }
	}
	
	/*
	 * 
	 */
	public TwitterUser getUser(Long userId, FinishedCallback callback, ConnectionStatus connectionStatus) {
		
		TwitterUser user = mUserIdHashMap.get(userId);
		
		if (callback != null) {
			trigger(userId, callback, connectionStatus);
		}
			
		return user;
	}
	
	public TwitterUser getUser(String screenName, FinishedCallback callback, ConnectionStatus connectionStatus) {
		
		TwitterUser user = mUserScreenNameHashMap.get(screenName);
		
		if (callback != null) {
			trigger(screenName, callback, connectionStatus);
		}
		
		return user;
	}
	
	/*
	 * 
	 */
	public void verifyUser(FinishedCallback callback, ConnectionStatus connectionStatus) {
		
		if (connectionStatus.isOnline() == false) {
			if (callback != null) {
				callback.finished(new TwitterFetchResult(false, connectionStatus.getErrorMessageNoConnection()), null);
			}
			return;
		}
		
		mFinishedCallbackMap.put(mFetchUserCallbackHandle, callback);
		new FetchUserTask().execute(AsyncTaskEx.PRIORITY_HIGH, "Validate User", new FetchUserTaskInput(mFetchUserCallbackHandle, connectionStatus));
		mFetchUserCallbackHandle += 1;
	}
	
	/*
	 * 
	 */
	private void trigger(Long userId, FinishedCallback callback, ConnectionStatus connectionStatus) {
		
		if (connectionStatus.isOnline() == false) {
			if (callback != null) {
				callback.finished(new TwitterFetchResult(false, connectionStatus.getErrorMessageNoConnection()), null);
			}
			return;
		}
		
		assert(mFinishedCallbackMap.containsValue(callback) == false);
		
		mFinishedCallbackMap.put(mFetchUserCallbackHandle, callback);
		new FetchUserTask().execute(AsyncTaskEx.PRIORITY_HIGH, "Fetch User", new FetchUserTaskInput(userId, mFetchUserCallbackHandle, connectionStatus));
		
		mFetchUserCallbackHandle += 1;
	}
	
	private void trigger(String screenName, FinishedCallback callback, ConnectionStatus connectionStatus) {
		
		if (connectionStatus.isOnline() == false) {
			if (callback != null) {
				callback.finished(new TwitterFetchResult(false, connectionStatus.getErrorMessageNoConnection()), null);
			}
			return;
		}
		
		assert(mFinishedCallbackMap.containsValue(callback) == false);
		
		mFinishedCallbackMap.put(mFetchUserCallbackHandle, callback);
		new FetchUserTask().execute(AsyncTaskEx.PRIORITY_HIGH, "Fetch User", new FetchUserTaskInput(screenName, mFetchUserCallbackHandle, connectionStatus));
		
		mFetchUserCallbackHandle += 1;
	}
	
	/*
	 * 
	 */
	public void cancel(FinishedCallback callback) {
		
		removeFetchStatusesCallback(callback);
	}
	
	
	
	/*
	 * 
	 */
	class FetchUserTaskInput {
		
		FetchUserTaskInput(Long userId, Integer callbackHandle, ConnectionStatus connectionStatus) {
			mCallbackHandle = callbackHandle;
			mUserId = userId;
			mConnectionStatus = connectionStatus;
		}
		
		FetchUserTaskInput(String screenName, Integer callbackHandle, ConnectionStatus connectionStatus) {
			mCallbackHandle = callbackHandle;
			mScreenName = screenName;
			mConnectionStatus = connectionStatus;
		}
		
		FetchUserTaskInput(Integer callbackHandle, ConnectionStatus connectionStatus) {
			mCallbackHandle = callbackHandle;
			mVerifyCredentials = true;
			mConnectionStatus = connectionStatus;
		}
		
		Integer mCallbackHandle;
		Boolean mVerifyCredentials;
		Long mUserId;
		String mScreenName;
		ConnectionStatus mConnectionStatus;
		
	}
	
	/*
	 * 
	 */
	class FetchUserTaskOutput {
		
		FetchUserTaskOutput(TwitterFetchResult fetchResult, Integer callbackHandle, TwitterUser user) {
			mFetchResult = fetchResult;
			mCallbackHandle = callbackHandle;
			mUser = user;
		}
		
		TwitterFetchResult mFetchResult;
		Integer mCallbackHandle;
		TwitterUser mUser;
	}
	
	/*
	 * 
	 */
	class FetchUserTask extends AsyncTaskEx<FetchUserTaskInput, Void, FetchUserTaskOutput> {

		@Override
		protected FetchUserTaskOutput doInBackground(FetchUserTaskInput... inputArray) {

			TwitterUser result = null;
			FetchUserTaskInput input = inputArray[0];
			
			if (input.mConnectionStatus.isOnline() == false) {
				return new FetchUserTaskOutput(new TwitterFetchResult(false, input.mConnectionStatus.getErrorMessageNoConnection()), input.mCallbackHandle, null);
			}
			
			String errorDescription = null;
			Twitter twitter = getTwitterInstance();
			if (twitter != null) {
				try {
					User user = null;
					if (input.mVerifyCredentials != null && input.mVerifyCredentials.booleanValue() == true) {
						user = twitter.verifyCredentials();
					} else {
						if (input.mUserId != null) {
							user = twitter.showUser(input.mUserId);
						} else if (input.mScreenName != null) {
							user = twitter.showUser(input.mScreenName);
						}
					}
					result = new TwitterUser(user);
					setUser(result);
				} catch (TwitterException e) {
					e.printStackTrace();
					errorDescription = e.getErrorMessage();
				}
			} else {
				AppdotnetApi api = getAppdotnetApi();
				if (api != null && input.mUserId != null) {
					result = api.getAdnUser(input.mUserId);
					setUser(result);
				}
			}

			return new FetchUserTaskOutput(new TwitterFetchResult(errorDescription == null ? true : false, errorDescription), input.mCallbackHandle, result);
		}

		@Override
		protected void onPostExecute(FetchUserTaskOutput output) {
			
			FinishedCallback callback = getFetchStatusesCallback(output.mCallbackHandle);
			if (callback != null) {
				callback.finished(output.mFetchResult, output.mUser);
				removeFetchStatusesCallback(callback);
			}

			super.onPostExecute(output);
		}
	}

}