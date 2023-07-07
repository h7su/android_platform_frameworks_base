/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.retail.domain.interactor

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.retail.data.repository.RetailModeRepository
import javax.inject.Inject

/** Interactor to determine if the device is currently in retail mode */
interface RetailModeInteractor {
    /** Whether the device is currently in retail mode */
    val isInRetailMode: Boolean
}

@SysUISingleton
class RetailModeInteractorImpl
@Inject
constructor(
    private val repository: RetailModeRepository,
) : RetailModeInteractor {
    override val isInRetailMode: Boolean
        get() = repository.inRetailMode
}
