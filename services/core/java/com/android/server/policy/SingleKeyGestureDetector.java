/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.policy;

import android.annotation.IntDef;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Detect single key gesture: press, long press, very long press and multi press.
 *
 * Call {@link #reset} if current {@link KeyEvent} has been handled by another policy
 */

public final class SingleKeyGestureDetector {
    private static final String TAG = "SingleKeyGesture";
    private static final boolean DEBUG = false;

    private static final int MSG_KEY_LONG_PRESS = 0;
    private static final int MSG_KEY_VERY_LONG_PRESS = 1;
    private static final int MSG_KEY_DELAYED_PRESS = 2;

    private final long mLongPressTimeout;
    private final long mVeryLongPressTimeout;

    private volatile int mKeyPressCounter;

    private final ArrayList<SingleKeyRule> mRules = new ArrayList();
    private SingleKeyRule mActiveRule = null;

    // Key code of current key down event, reset when key up.
    private int mDownKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private volatile boolean mHandledByLongPress = false;
    private final Handler mHandler;
    private static final long MULTI_PRESS_TIMEOUT = ViewConfiguration.getMultiPressTimeout();


    /** Supported gesture flags */
    public static final int KEY_LONGPRESS = 1 << 1;
    public static final int KEY_VERYLONGPRESS = 1 << 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "KEY_" }, value = {
            KEY_LONGPRESS,
            KEY_VERYLONGPRESS,
    })
    public @interface KeyGestureFlag {}

    /**
     *  Rule definition for single keys gesture.
     *  E.g : define power key.
     *  <pre class="prettyprint">
     *  SingleKeyRule rule =
     *      new SingleKeyRule(KEYCODE_POWER, KEY_LONGPRESS|KEY_VERYLONGPRESS) {
     *           int getMaxMultiPressCount() { // maximum multi press count. }
     *           void onPress(long downTime) { // short press behavior. }
     *           void onLongPress(long eventTime) { // long press behavior. }
     *           void onVeryLongPress(long eventTime) { // very long press behavior. }
     *           void onMultiPress(long downTime, int count) { // multi press behavior.  }
     *       };
     *  </pre>
     */
    abstract static class SingleKeyRule {
        private final int mKeyCode;
        private final int mSupportedGestures;

        SingleKeyRule(int keyCode, @KeyGestureFlag int supportedGestures) {
            mKeyCode = keyCode;
            mSupportedGestures = supportedGestures;
        }

        /**
         *  True if the rule could intercept the key.
         */
        private boolean shouldInterceptKey(int keyCode) {
            return keyCode == mKeyCode;
        }

        /**
         *  True if the rule support long press.
         */
        private boolean supportLongPress() {
            return (mSupportedGestures & KEY_LONGPRESS) != 0;
        }

        /**
         *  True if the rule support very long press.
         */
        private boolean supportVeryLongPress() {
            return (mSupportedGestures & KEY_VERYLONGPRESS) != 0;
        }

        /**
         *  Maximum count of multi presses.
         *  Return 1 will trigger onPress immediately when {@link KeyEvent.ACTION_UP}.
         *  Otherwise trigger onMultiPress immediately when reach max count when
         *  {@link KeyEvent.ACTION_DOWN}.
         */
        int getMaxMultiPressCount() {
            return 1;
        }

        /**
         *  Called when short press has been detected.
         */
        abstract void onPress(long downTime);
        /**
         *  Callback when multi press (>= 2) has been detected.
         */
        void onMultiPress(long downTime, int count) {}
        /**
         *  Callback when long press has been detected.
         */
        void onLongPress(long eventTime) {}
        /**
         *  Callback when very long press has been detected.
         */
        void onVeryLongPress(long eventTime) {}

        @Override
        public String toString() {
            return "KeyCode = " + KeyEvent.keyCodeToString(mKeyCode)
                    + ", long press : " + supportLongPress()
                    + ", very Long press : " + supportVeryLongPress()
                    + ", max multi press count : " + getMaxMultiPressCount();
        }
    }

    public SingleKeyGestureDetector(Context context) {
        mLongPressTimeout = ViewConfiguration.get(context).getDeviceGlobalActionKeyTimeout();
        mVeryLongPressTimeout = context.getResources().getInteger(
                com.android.internal.R.integer.config_veryLongPressTimeout);
        mHandler = new KeyHandler();
    }

    void addRule(SingleKeyRule rule) {
        mRules.add(rule);
    }

    void interceptKey(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            interceptKeyDown(event);
        } else {
            interceptKeyUp(event);
        }
    }

    private void interceptKeyDown(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        // same key down.
        if (mDownKeyCode == keyCode) {
            if (mActiveRule != null && (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0
                    && mActiveRule.supportLongPress() && !mHandledByLongPress) {
                if (DEBUG) {
                    Log.i(TAG, "Long press key " + KeyEvent.keyCodeToString(keyCode));
                }
                mHandledByLongPress = true;
                mHandler.removeMessages(MSG_KEY_LONG_PRESS);
                mHandler.removeMessages(MSG_KEY_VERY_LONG_PRESS);
                mActiveRule.onLongPress(event.getEventTime());
            }
            return;
        }

        // When a different key is pressed, stop processing gestures for the currently active key.
        if (mDownKeyCode != KeyEvent.KEYCODE_UNKNOWN
                || (mActiveRule != null && !mActiveRule.shouldInterceptKey(keyCode))) {
            if (DEBUG) {
                Log.i(TAG, "Press another key " + KeyEvent.keyCodeToString(keyCode));
            }
            reset();
        }
        mDownKeyCode = keyCode;

        // Picks a new rule, return if no rule picked.
        if (mActiveRule == null) {
            final int count = mRules.size();
            for (int index = 0; index < count; index++) {
                final SingleKeyRule rule = mRules.get(index);
                if (rule.shouldInterceptKey(keyCode)) {
                    if (DEBUG) {
                        Log.i(TAG, "Intercept key by rule " + rule);
                    }
                    mActiveRule = rule;
                    break;
                }
            }
        }
        if (mActiveRule == null) {
            return;
        }

        final long eventTime = event.getEventTime();
        if (mKeyPressCounter == 0) {
            if (mActiveRule.supportLongPress()) {
                final Message msg = mHandler.obtainMessage(MSG_KEY_LONG_PRESS, keyCode, 0,
                        eventTime);
                msg.setAsynchronous(true);
                mHandler.sendMessageDelayed(msg, mLongPressTimeout);
            }

            if (mActiveRule.supportVeryLongPress()) {
                final Message msg = mHandler.obtainMessage(MSG_KEY_VERY_LONG_PRESS, keyCode, 0,
                        eventTime);
                msg.setAsynchronous(true);
                mHandler.sendMessageDelayed(msg, mVeryLongPressTimeout);
            }
        } else {
            mHandler.removeMessages(MSG_KEY_LONG_PRESS);
            mHandler.removeMessages(MSG_KEY_VERY_LONG_PRESS);
            mHandler.removeMessages(MSG_KEY_DELAYED_PRESS);

            // Trigger multi press immediately when reach max count.( > 1)
            if (mKeyPressCounter == mActiveRule.getMaxMultiPressCount() - 1) {
                if (DEBUG) {
                    Log.i(TAG, "Trigger multi press " + mActiveRule.toString() + " for it"
                            + " reach the max count " + mKeyPressCounter);
                }
                mActiveRule.onMultiPress(eventTime, mKeyPressCounter + 1);
                mKeyPressCounter = 0;
            }
        }
    }

    private boolean interceptKeyUp(KeyEvent event) {
        mHandler.removeMessages(MSG_KEY_LONG_PRESS);
        mHandler.removeMessages(MSG_KEY_VERY_LONG_PRESS);
        mDownKeyCode = KeyEvent.KEYCODE_UNKNOWN;
        if (mActiveRule == null) {
            return false;
        }

        if (mHandledByLongPress) {
            mHandledByLongPress = false;
            mKeyPressCounter = 0;
            return true;
        }

        final long downTime = event.getDownTime();
        if (event.getKeyCode() == mActiveRule.mKeyCode) {
            // Directly trigger short press when max count is 1.
            if (mActiveRule.getMaxMultiPressCount() == 1) {
                if (DEBUG) {
                    Log.i(TAG, "press key " + KeyEvent.keyCodeToString(event.getKeyCode()));
                }
                mActiveRule.onPress(downTime);
                return true;
            }

            // This could be a multi-press.  Wait a little bit longer to confirm.
            mKeyPressCounter++;
            Message msg = mHandler.obtainMessage(MSG_KEY_DELAYED_PRESS, mActiveRule.mKeyCode,
                    mKeyPressCounter, downTime);
            msg.setAsynchronous(true);
            mHandler.sendMessageDelayed(msg, MULTI_PRESS_TIMEOUT);
            return true;
        }
        reset();
        return false;
    }

    int getKeyPressCounter(int keyCode) {
        if (mActiveRule != null && mActiveRule.mKeyCode == keyCode) {
            return mKeyPressCounter;
        } else {
            return 0;
        }
    }

    void reset() {
        if (mActiveRule != null) {
            if (mDownKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
                mHandler.removeMessages(MSG_KEY_LONG_PRESS);
                mHandler.removeMessages(MSG_KEY_VERY_LONG_PRESS);
            }

            if (mKeyPressCounter > 0) {
                mHandler.removeMessages(MSG_KEY_DELAYED_PRESS);
                mKeyPressCounter = 0;
            }
            mActiveRule = null;
        }

        mHandledByLongPress = false;
        mDownKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    }

    boolean isKeyIntercepted(int keyCode) {
        if (mActiveRule != null && mActiveRule.shouldInterceptKey(keyCode)) {
            return mHandledByLongPress;
        }
        return false;
    }

    private class KeyHandler extends Handler {
        KeyHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActiveRule == null) {
                return;
            }
            final int keyCode = msg.arg1;
            final long eventTime = (long) msg.obj;
            switch(msg.what) {
                case MSG_KEY_LONG_PRESS:
                    if (DEBUG) {
                        Log.i(TAG, "Detect long press " + KeyEvent.keyCodeToString(keyCode));
                    }
                    mHandledByLongPress = true;
                    mActiveRule.onLongPress(eventTime);
                    break;
                case MSG_KEY_VERY_LONG_PRESS:
                    if (DEBUG) {
                        Log.i(TAG, "Detect very long press "
                                + KeyEvent.keyCodeToString(keyCode));
                    }
                    mHandledByLongPress = true;
                    mActiveRule.onVeryLongPress(eventTime);
                    break;
                case MSG_KEY_DELAYED_PRESS:
                    if (DEBUG) {
                        Log.i(TAG, "Detect press " + KeyEvent.keyCodeToString(keyCode)
                                + ", count " + mKeyPressCounter);
                    }
                    if (mKeyPressCounter == 1) {
                        mActiveRule.onPress(eventTime);
                    } else {
                        mActiveRule.onMultiPress(eventTime, mKeyPressCounter);
                    }
                    mKeyPressCounter = 0;
                    break;
            }
        }
    }
}
