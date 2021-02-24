/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.wm.shell.flicker

import android.graphics.Region
import android.view.Surface
import com.android.server.wm.flicker.APP_PAIR_SPLIT_DIVIDER
import com.android.server.wm.flicker.DOCKED_STACK_DIVIDER
import com.android.server.wm.flicker.FlickerTestParameter
import com.android.server.wm.flicker.helpers.WindowUtils
import com.android.server.wm.flicker.traces.layers.getVisibleBounds

fun FlickerTestParameter.appPairsDividerIsVisible() {
    assertLayersEnd {
        this.isVisible(APP_PAIR_SPLIT_DIVIDER)
    }
}

fun FlickerTestParameter.appPairsDividerIsInvisible() {
    assertLayersEnd {
        this.notExists(APP_PAIR_SPLIT_DIVIDER)
    }
}

fun FlickerTestParameter.appPairsDividerBecomesVisible() {
    assertLayers {
        this.hidesLayer(DOCKED_STACK_DIVIDER)
            .then()
            .showsLayer(DOCKED_STACK_DIVIDER)
    }
}

fun FlickerTestParameter.dockedStackDividerIsVisible() {
    assertLayersEnd {
        this.isVisible(DOCKED_STACK_DIVIDER)
    }
}

fun FlickerTestParameter.dockedStackDividerBecomesVisible() {
    assertLayers {
        this.hidesLayer(DOCKED_STACK_DIVIDER)
            .then()
            .showsLayer(DOCKED_STACK_DIVIDER)
    }
}

fun FlickerTestParameter.dockedStackDividerBecomesInvisible() {
    assertLayers {
        this.showsLayer(DOCKED_STACK_DIVIDER)
            .then()
            .hidesLayer(DOCKED_STACK_DIVIDER)
    }
}

fun FlickerTestParameter.dockedStackDividerIsInvisible() {
    assertLayersEnd {
        this.notExists(DOCKED_STACK_DIVIDER)
    }
}

fun FlickerTestParameter.appPairsPrimaryBoundsIsVisible(rotation: Int, primaryLayerName: String) {
    assertLayersEnd {
        val dividerRegion = entry.getVisibleBounds(APP_PAIR_SPLIT_DIVIDER)
        this.hasVisibleRegion(primaryLayerName, getPrimaryRegion(dividerRegion, rotation))
    }
}

fun FlickerTestParameter.dockedStackPrimaryBoundsIsVisible(
    rotation: Int,
    primaryLayerName: String
) {
    assertLayersEnd {
        val dividerRegion = entry.getVisibleBounds(DOCKED_STACK_DIVIDER)
        this.hasVisibleRegion(primaryLayerName, getPrimaryRegion(dividerRegion, rotation))
    }
}

fun FlickerTestParameter.appPairsSecondaryBoundsIsVisible(
    rotation: Int,
    secondaryLayerName: String
) {
    assertLayersEnd {
        val dividerRegion = entry.getVisibleBounds(APP_PAIR_SPLIT_DIVIDER)
        this.hasVisibleRegion(secondaryLayerName, getSecondaryRegion(dividerRegion, rotation))
    }
}

fun FlickerTestParameter.dockedStackSecondaryBoundsIsVisible(
    rotation: Int,
    secondaryLayerName: String
) {
    assertLayersEnd {
        val dividerRegion = entry.getVisibleBounds(DOCKED_STACK_DIVIDER)
        this.hasVisibleRegion(secondaryLayerName, getSecondaryRegion(dividerRegion, rotation))
    }
}

fun getPrimaryRegion(dividerRegion: Region, rotation: Int): Region {
    val displayBounds = WindowUtils.getDisplayBounds(rotation)
    return if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
        Region(0, 0, displayBounds.bounds.right,
            dividerRegion.bounds.bottom - WindowUtils.dockedStackDividerInset)
    } else {
        Region(0, 0, dividerRegion.bounds.left,
            dividerRegion.bounds.right - WindowUtils.dockedStackDividerInset)
    }
}

fun getSecondaryRegion(dividerRegion: Region, rotation: Int): Region {
    val displayBounds = WindowUtils.getDisplayBounds(rotation)
    return if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
        Region(0,
            dividerRegion.bounds.bottom - WindowUtils.dockedStackDividerInset,
            displayBounds.bounds.right,
            displayBounds.bounds.bottom - WindowUtils.dockedStackDividerInset)
    } else {
        Region(dividerRegion.bounds.right, 0,
            displayBounds.bounds.right,
            displayBounds.bounds.bottom - WindowUtils.dockedStackDividerInset)
    }
}