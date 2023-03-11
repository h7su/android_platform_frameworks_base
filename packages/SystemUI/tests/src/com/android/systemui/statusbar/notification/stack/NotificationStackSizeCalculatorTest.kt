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

package com.android.systemui.statusbar.notification.stack

import android.annotation.DimenRes
import android.service.notification.StatusBarNotification
import android.testing.AndroidTestingRunner
import android.view.View.VISIBLE
import androidx.test.filters.SmallTest
import com.android.systemui.R
import com.android.systemui.SysuiTestCase
import com.android.systemui.statusbar.LockscreenShadeTransitionController
import com.android.systemui.statusbar.StatusBarState
import com.android.systemui.statusbar.SysuiStatusBarStateController
import com.android.systemui.statusbar.notification.collection.NotificationEntry
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow
import com.android.systemui.statusbar.notification.row.ExpandableView
import com.android.systemui.util.mockito.any
import com.android.systemui.util.mockito.eq
import com.android.systemui.util.mockito.nullable
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations

@SmallTest
@RunWith(AndroidTestingRunner::class)
class NotificationStackSizeCalculatorTest : SysuiTestCase() {

    @Mock private lateinit var sysuiStatusBarStateController: SysuiStatusBarStateController
    @Mock
    private lateinit var lockscreenShadeTransitionController: LockscreenShadeTransitionController
    @Mock private lateinit var stackLayout: NotificationStackScrollLayout

    private val testableResources = mContext.orCreateTestableResources

    private lateinit var sizeCalculator: NotificationStackSizeCalculator

    private val gapHeight = px(R.dimen.notification_section_divider_height)
    private val dividerHeight = px(R.dimen.notification_divider_height)
    private val shelfHeight = px(R.dimen.notification_shelf_height)
    private val rowHeight = px(R.dimen.notification_max_height)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        sizeCalculator =
            NotificationStackSizeCalculator(
                statusBarStateController = sysuiStatusBarStateController,
                lockscreenShadeTransitionController = lockscreenShadeTransitionController,
                testableResources.resources)
    }

    @Test
    fun computeMaxKeyguardNotifications_zeroSpace_returnZero() {
        val rows = listOf(createMockRow(height = rowHeight))

        val maxNotifications =
            computeMaxKeyguardNotifications(
                rows, spaceForNotifications = 0f, spaceForShelf = 0f, shelfHeight = 0f)

        assertThat(maxNotifications).isEqualTo(0)
    }

    @Test
    fun computeMaxKeyguardNotifications_infiniteSpace_returnsAll() {
        val numberOfRows = 30
        val rows = createLockscreenRows(numberOfRows)

        val maxNotifications =
            computeMaxKeyguardNotifications(
                rows,
                spaceForNotifications = Float.MAX_VALUE,
                spaceForShelf = Float.MAX_VALUE,
                shelfHeight)

        assertThat(maxNotifications).isEqualTo(numberOfRows)
    }

    @Test
    fun computeMaxKeyguardNotifications_spaceForOneAndShelf_returnsOne() {
        setGapHeight(gapHeight)
        val shelfHeight = rowHeight / 2 // Shelf absence won't leave room for another row.
        val spaceForNotifications = rowHeight + dividerHeight
        val spaceForShelf = gapHeight + dividerHeight + shelfHeight
        val rows = listOf(createMockRow(rowHeight), createMockRow(rowHeight))

        val maxNotifications =
            computeMaxKeyguardNotifications(rows, spaceForNotifications, spaceForShelf, shelfHeight)

        assertThat(maxNotifications).isEqualTo(1)
    }

    @Test
    fun computeMaxKeyguardNotifications_spaceForTwo_returnsTwo() {
        setGapHeight(gapHeight)
        val shelfHeight = shelfHeight + dividerHeight
        val spaceForNotifications =
            listOf(
                    rowHeight + dividerHeight,
                    gapHeight + rowHeight + dividerHeight,
                )
                .sum()
        val spaceForShelf = gapHeight + dividerHeight + shelfHeight
        val rows =
            listOf(createMockRow(rowHeight), createMockRow(rowHeight), createMockRow(rowHeight))

        val maxNotifications =
            computeMaxKeyguardNotifications(
                rows, spaceForNotifications + 1, spaceForShelf, shelfHeight)

        assertThat(maxNotifications).isEqualTo(2)
    }

    @Test
    fun computeHeight_gapBeforeShelf_returnsSpaceUsed() {
        // Each row in separate section.
        setGapHeight(gapHeight)

        val spaceForNotifications =
            listOf(
                    rowHeight,
                    dividerHeight + gapHeight + rowHeight,
                )
                .sum()

        val spaceForShelf = dividerHeight + gapHeight + shelfHeight
        val spaceUsed = spaceForNotifications + spaceForShelf
        val rows =
            listOf(createMockRow(rowHeight), createMockRow(rowHeight), createMockRow(rowHeight))

        val maxNotifications =
            computeMaxKeyguardNotifications(rows, spaceForNotifications, spaceForShelf, shelfHeight)
        assertThat(maxNotifications).isEqualTo(2)

        val height =
            sizeCalculator.computeHeight(stackLayout, maxNotifications, this.shelfHeight)
        assertThat(height).isEqualTo(spaceUsed)
    }

    @Test
    fun computeHeight_noGapBeforeShelf_returnsSpaceUsed() {
        // Both rows are in the same section.
        setGapHeight(0f)

        val spaceForNotifications = rowHeight
        val spaceForShelf = dividerHeight + shelfHeight
        val spaceUsed = spaceForNotifications + spaceForShelf
        val rows = listOf(createMockRow(rowHeight), createMockRow(rowHeight))

        // test that we only use space required
        val maxNotifications =
            computeMaxKeyguardNotifications(
                rows, spaceForNotifications + 1, spaceForShelf, shelfHeight)
        assertThat(maxNotifications).isEqualTo(1)

        val height =
            sizeCalculator.computeHeight(stackLayout, maxNotifications, this.shelfHeight)
        assertThat(height).isEqualTo(spaceUsed)
    }

    @Test
    fun onLockscreen_onKeyguard_AndNotGoingToShade_returnsTrue() {
        whenever(sysuiStatusBarStateController.state).thenReturn(StatusBarState.KEYGUARD)
        whenever(lockscreenShadeTransitionController.fractionToShade).thenReturn(0f)
        assertThat(sizeCalculator.onLockscreen()).isTrue()
    }

    @Test
    fun onLockscreen_goingToShade_returnsFalse() {
        whenever(sysuiStatusBarStateController.state).thenReturn(StatusBarState.KEYGUARD)
        whenever(lockscreenShadeTransitionController.fractionToShade).thenReturn(0.5f)
        assertThat(sizeCalculator.onLockscreen()).isFalse()
    }

    @Test
    fun onLockscreen_notOnLockscreen_returnsFalse() {
        whenever(sysuiStatusBarStateController.state).thenReturn(StatusBarState.SHADE)
        whenever(lockscreenShadeTransitionController.fractionToShade).thenReturn(1f)
        assertThat(sizeCalculator.onLockscreen()).isFalse()
    }

    @Test
    fun spaceNeeded_onLockscreen_usesMinHeight() {
        setGapHeight(0f)
        // No divider height since we're testing one element where index = 0

        val expandableView = createMockRow(rowHeight)
        whenever(expandableView.getMinHeight(any())).thenReturn(5)
        whenever(expandableView.intrinsicHeight).thenReturn(10)

        val space =
            sizeCalculator.spaceNeeded(
                expandableView,
                visibleIndex = 0,
                previousView = null,
                stack = stackLayout,
                onLockscreen = true)
        assertThat(space).isEqualTo(5)
    }

    @Test
    fun spaceNeeded_fsiHunOnLockscreen_usesIntrinsicHeight() {
        setGapHeight(0f)
        // No divider height since we're testing one element where index = 0

        val expandableView = createMockStickyRow(rowHeight)
        whenever(expandableView.getMinHeight(any())).thenReturn(5)
        whenever(expandableView.intrinsicHeight).thenReturn(10)

        val space =
                sizeCalculator.spaceNeeded(
                        expandableView,
                        visibleIndex = 0,
                        previousView = null,
                        stack = stackLayout,
                        onLockscreen = true)
        assertThat(space).isEqualTo(10)
    }

    @Test
    fun spaceNeeded_notOnLockscreen_usesIntrinsicHeight() {
        setGapHeight(0f)
        // No divider height since we're testing one element where index = 0

        val expandableView = createMockRow(rowHeight)
        whenever(expandableView.getMinHeight(any())).thenReturn(5)
        whenever(expandableView.intrinsicHeight).thenReturn(10)

        val space =
            sizeCalculator.spaceNeeded(
                expandableView,
                visibleIndex = 0,
                previousView = null,
                stack = stackLayout,
                onLockscreen = false)
        assertThat(space).isEqualTo(10)
    }

    private fun computeMaxKeyguardNotifications(
        rows: List<ExpandableView>,
        spaceForNotifications: Float,
        spaceForShelf: Float,
        shelfHeight: Float = this.shelfHeight
    ): Int {
        setupChildren(rows)
        return sizeCalculator.computeMaxKeyguardNotifications(
            stackLayout, spaceForNotifications, spaceForShelf, shelfHeight)
    }

    private fun setupChildren(children: List<ExpandableView>) {
        whenever(stackLayout.getChildAt(any())).thenAnswer { invocation ->
            val inx = invocation.getArgument<Int>(0)
            return@thenAnswer children[inx]
        }
        whenever(stackLayout.childCount).thenReturn(children.size)
    }

    private fun createLockscreenRows(number: Int): List<ExpandableNotificationRow> =
        (1..number).map { createMockRow() }.toList()

    private fun createMockRow(
        height: Float = rowHeight,
        isRemoved: Boolean = false,
        visibility: Int = VISIBLE
    ): ExpandableNotificationRow {
        val row = mock(ExpandableNotificationRow::class.java)
        val entry = mock(NotificationEntry::class.java)
        val sbn = mock(StatusBarNotification::class.java)
        whenever(entry.sbn).thenReturn(sbn)
        whenever(row.entry).thenReturn(entry)
        whenever(row.isRemoved).thenReturn(isRemoved)
        whenever(row.visibility).thenReturn(visibility)
        whenever(row.getMinHeight(any())).thenReturn(height.toInt())
        whenever(row.intrinsicHeight).thenReturn(height.toInt())
        return row
    }

    private fun createMockStickyRow(
            height: Float = rowHeight,
            isRemoved: Boolean = false,
            visibility: Int = VISIBLE
    ): ExpandableNotificationRow {
        val row = mock(ExpandableNotificationRow::class.java)
        val entry = mock(NotificationEntry::class.java)
        whenever(entry.isStickyAndNotDemoted).thenReturn(true)

        val sbn = mock(StatusBarNotification::class.java)
        whenever(entry.sbn).thenReturn(sbn)
        whenever(row.entry).thenReturn(entry)
        whenever(row.isRemoved).thenReturn(isRemoved)
        whenever(row.visibility).thenReturn(visibility)
        whenever(row.getMinHeight(any())).thenReturn(height.toInt())
        whenever(row.intrinsicHeight).thenReturn(height.toInt())
        return row
    }

    private fun setGapHeight(height: Float) {
        whenever(stackLayout.calculateGapHeight(nullable(), nullable(), any())).thenReturn(height)
        whenever(stackLayout.calculateGapHeight(nullable(), nullable(), /* visibleIndex= */ eq(0)))
            .thenReturn(0f)
    }

    private fun px(@DimenRes id: Int): Float =
        testableResources.resources.getDimensionPixelSize(id).toFloat()
}
