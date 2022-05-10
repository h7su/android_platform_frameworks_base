/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.backup.transport;

import android.annotation.Nullable;

import com.android.server.backup.transport.BackupTransportClient;

/**
 * Listener to be called by {@link TransportConnection#connectAsync(TransportConnectionListener,
 * String)}.
 */
public interface TransportConnectionListener {
    /**
     * Called when {@link TransportConnection} has a transport client available or that it decided
     * it couldn't obtain one, in which case {@param transport} is null.
     *
     * @param transportClient A {@link BackupTransportClient} transport or null.
     * @param transportConnection The {@link TransportConnection} used to retrieve this transport
     *                            client.
     */
    void onTransportConnectionResult(
            @Nullable BackupTransportClient transportClient,
            TransportConnection transportConnection);
}
