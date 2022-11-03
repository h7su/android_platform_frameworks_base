/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.content.pm;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.os.Process;
import android.os.UserHandle;
import android.util.SparseArrayMap;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;

import java.util.Objects;

/**
 * POJO to represent a package for a specific user ID.
 *
 * @hide
 */
public final class UserPackage {
    @UserIdInt
    public final int userId;
    public final String packageName;

    @GuardedBy("sCache")
    private static final SparseArrayMap<String, UserPackage> sCache = new SparseArrayMap<>();

    private static final Object sUserIdLock = new Object();
    private static final class NoPreloadHolder {
        /** Set of userIDs to cache objects for. */
        @GuardedBy("sUserIdLock")
        private static int[] sUserIds = new int[]{UserHandle.getUserId(Process.myUid())};
    }

    private UserPackage(int userId, String packageName) {
        this.userId = userId;
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return "<" + userId + ">" + packageName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UserPackage) {
            UserPackage other = (UserPackage) obj;
            return userId == other.userId && Objects.equals(packageName, other.packageName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + userId;
        result = 31 * result + packageName.hashCode();
        return result;
    }

    /** Return an instance of this class representing the given userId + packageName combination. */
    @NonNull
    public static UserPackage of(@UserIdInt int userId, @NonNull String packageName) {
        synchronized (sUserIdLock) {
            if (!ArrayUtils.contains(NoPreloadHolder.sUserIds, userId)) {
                // Don't cache objects for invalid userIds.
                return new UserPackage(userId, packageName);
            }
        }
        synchronized (sCache) {
            UserPackage up = sCache.get(userId, packageName);
            if (up == null) {
                packageName = packageName.intern();
                up = new UserPackage(userId, packageName);
                sCache.add(userId, packageName, up);
            }
            return up;
        }
    }

    /** Remove the specified app from the cache. */
    public static void removeFromCache(@UserIdInt int userId, @NonNull String packageName) {
        synchronized (sCache) {
            sCache.delete(userId, packageName);
        }
    }

    /** Indicate the list of valid user IDs on the device. */
    public static void setValidUserIds(@NonNull int[] userIds) {
        userIds = userIds.clone();
        synchronized (sUserIdLock) {
            NoPreloadHolder.sUserIds = userIds;
        }
        synchronized (sCache) {
            for (int u = sCache.numMaps() - 1; u >= 0; --u) {
                final int userId = sCache.keyAt(u);
                // Not holding sUserIdLock is intentional here. We don't modify the elements within
                // the array and so even if this method is called multiple times with different sets
                // of user IDs, we want to adjust the cache based on each new array.
                if (!ArrayUtils.contains(userIds, userId)) {
                    sCache.deleteAt(u);
                }
            }
        }
    }
}
