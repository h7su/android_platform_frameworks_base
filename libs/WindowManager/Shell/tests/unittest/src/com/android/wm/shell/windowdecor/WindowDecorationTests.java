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

package com.android.wm.shell.windowdecor;

import static com.android.wm.shell.MockSurfaceControlHelper.createMockSurfaceControlBuilder;
import static com.android.wm.shell.MockSurfaceControlHelper.createMockSurfaceControlTransaction;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.testing.AndroidTestingRunner;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.InsetsState;
import android.view.SurfaceControl;
import android.view.SurfaceControlViewHost;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.WindowManager.LayoutParams;
import android.window.WindowContainerTransaction;

import androidx.test.filters.SmallTest;

import com.android.wm.shell.ShellTaskOrganizer;
import com.android.wm.shell.ShellTestCase;
import com.android.wm.shell.TestRunningTaskInfoBuilder;
import com.android.wm.shell.common.DisplayController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tests for {@link WindowDecoration}.
 *
 * Build/Install/Run:
 * atest WMShellUnitTests:WindowDecorationTests
 */
@SmallTest
@RunWith(AndroidTestingRunner.class)
public class WindowDecorationTests extends ShellTestCase {
    private static final int CAPTION_HEIGHT_DP = 32;
    private static final int SHADOW_RADIUS_DP = 5;
    private static final Rect TASK_BOUNDS = new Rect(100, 300, 400, 400);
    private static final Point TASK_POSITION_IN_PARENT = new Point(40, 60);

    private final Rect mOutsetsDp = new Rect();
    private final WindowDecoration.RelayoutResult<TestView> mRelayoutResult =
            new WindowDecoration.RelayoutResult<>();

    @Mock
    private DisplayController mMockDisplayController;
    @Mock
    private ShellTaskOrganizer mMockShellTaskOrganizer;
    @Mock
    private WindowDecoration.SurfaceControlViewHostFactory mMockSurfaceControlViewHostFactory;
    @Mock
    private SurfaceControlViewHost mMockSurfaceControlViewHost;
    @Mock
    private TestView mMockView;
    @Mock
    private WindowContainerTransaction mMockWindowContainerTransaction;

    private final List<SurfaceControl.Builder> mMockSurfaceControlBuilders = new ArrayList<>();
    private SurfaceControl.Transaction mMockSurfaceControlStartT;
    private SurfaceControl.Transaction mMockSurfaceControlFinishT;

    @Before
    public void setUp() {
        mMockSurfaceControlStartT = createMockSurfaceControlTransaction();
        mMockSurfaceControlFinishT = createMockSurfaceControlTransaction();

        doReturn(mMockSurfaceControlViewHost).when(mMockSurfaceControlViewHostFactory)
                .create(any(), any(), any(), anyBoolean());
    }

    @Test
    public void testLayoutResultCalculation_invisibleTask() {
        final Display defaultDisplay = mock(Display.class);
        doReturn(defaultDisplay).when(mMockDisplayController)
                .getDisplay(Display.DEFAULT_DISPLAY);

        final SurfaceControl decorContainerSurface = mock(SurfaceControl.class);
        final SurfaceControl.Builder decorContainerSurfaceBuilder =
                createMockSurfaceControlBuilder(decorContainerSurface);
        mMockSurfaceControlBuilders.add(decorContainerSurfaceBuilder);
        final SurfaceControl taskBackgroundSurface = mock(SurfaceControl.class);
        final SurfaceControl.Builder taskBackgroundSurfaceBuilder =
                createMockSurfaceControlBuilder(taskBackgroundSurface);
        mMockSurfaceControlBuilders.add(taskBackgroundSurfaceBuilder);

        final ActivityManager.TaskDescription.Builder taskDescriptionBuilder =
                new ActivityManager.TaskDescription.Builder()
                        .setBackgroundColor(Color.YELLOW);
        final ActivityManager.RunningTaskInfo taskInfo = new TestRunningTaskInfoBuilder()
                .setDisplayId(Display.DEFAULT_DISPLAY)
                .setTaskDescriptionBuilder(taskDescriptionBuilder)
                .setBounds(TASK_BOUNDS)
                .setPositionInParent(TASK_POSITION_IN_PARENT.x, TASK_POSITION_IN_PARENT.y)
                .setVisible(false)
                .build();
        taskInfo.isFocused = false;
        // Density is 2. Outsets are (20, 40, 60, 80) px. Shadow radius is 10px. Caption height is
        // 64px.
        taskInfo.configuration.densityDpi = DisplayMetrics.DENSITY_DEFAULT * 2;
        mOutsetsDp.set(10, 20, 30, 40);

        final SurfaceControl taskSurface = mock(SurfaceControl.class);
        final TestWindowDecoration windowDecor = createWindowDecoration(taskInfo, taskSurface);

        windowDecor.relayout(taskInfo);

        verify(decorContainerSurfaceBuilder, never()).build();
        verify(taskBackgroundSurfaceBuilder, never()).build();
        verify(mMockSurfaceControlViewHostFactory, never())
                .create(any(), any(), any(), anyBoolean());

        verify(mMockSurfaceControlFinishT).hide(taskSurface);

        assertNull(mRelayoutResult.mRootView);
    }

    @Test
    public void testLayoutResultCalculation_visibleFocusedTask() {
        final Display defaultDisplay = mock(Display.class);
        doReturn(defaultDisplay).when(mMockDisplayController)
                .getDisplay(Display.DEFAULT_DISPLAY);

        final SurfaceControl decorContainerSurface = mock(SurfaceControl.class);
        final SurfaceControl.Builder decorContainerSurfaceBuilder =
                createMockSurfaceControlBuilder(decorContainerSurface);
        mMockSurfaceControlBuilders.add(decorContainerSurfaceBuilder);
        final SurfaceControl taskBackgroundSurface = mock(SurfaceControl.class);
        final SurfaceControl.Builder taskBackgroundSurfaceBuilder =
                createMockSurfaceControlBuilder(taskBackgroundSurface);
        mMockSurfaceControlBuilders.add(taskBackgroundSurfaceBuilder);

        final ActivityManager.TaskDescription.Builder taskDescriptionBuilder =
                new ActivityManager.TaskDescription.Builder()
                        .setBackgroundColor(Color.YELLOW);
        final ActivityManager.RunningTaskInfo taskInfo = new TestRunningTaskInfoBuilder()
                .setDisplayId(Display.DEFAULT_DISPLAY)
                .setTaskDescriptionBuilder(taskDescriptionBuilder)
                .setBounds(TASK_BOUNDS)
                .setPositionInParent(TASK_POSITION_IN_PARENT.x, TASK_POSITION_IN_PARENT.y)
                .setVisible(true)
                .build();
        taskInfo.isFocused = true;
        // Density is 2. Outsets are (20, 40, 60, 80) px. Shadow radius is 10px. Caption height is
        // 64px.
        taskInfo.configuration.densityDpi = DisplayMetrics.DENSITY_DEFAULT * 2;
        mOutsetsDp.set(10, 20, 30, 40);

        final SurfaceControl taskSurface = mock(SurfaceControl.class);
        final TestWindowDecoration windowDecor = createWindowDecoration(taskInfo, taskSurface);

        windowDecor.relayout(taskInfo);

        verify(decorContainerSurfaceBuilder).setParent(taskSurface);
        verify(decorContainerSurfaceBuilder).setContainerLayer();
        verify(mMockSurfaceControlStartT).setTrustedOverlay(decorContainerSurface, true);
        verify(mMockSurfaceControlStartT).setPosition(decorContainerSurface, -20, -40);
        verify(mMockSurfaceControlStartT).setWindowCrop(decorContainerSurface, 380, 220);

        verify(taskBackgroundSurfaceBuilder).setParent(taskSurface);
        verify(taskBackgroundSurfaceBuilder).setEffectLayer();
        verify(mMockSurfaceControlStartT).setWindowCrop(taskBackgroundSurface, 300, 100);
        verify(mMockSurfaceControlStartT)
                .setColor(taskBackgroundSurface, new float[] {1.f, 1.f, 0.f});
        verify(mMockSurfaceControlStartT).setShadowRadius(taskBackgroundSurface, 10);

        verify(mMockSurfaceControlViewHostFactory)
                .create(any(), eq(defaultDisplay), any(), anyBoolean());
        verify(mMockSurfaceControlViewHost)
                .setView(same(mMockView),
                        argThat(lp -> lp.height == 64
                                && lp.width == 300
                                && (lp.flags & LayoutParams.FLAG_NOT_FOCUSABLE) != 0));
        if (ViewRootImpl.CAPTION_ON_SHELL) {
            verify(mMockView).setTaskFocusState(true);
            verify(mMockWindowContainerTransaction)
                    .addRectInsetsProvider(taskInfo.token,
                            new Rect(100, 300, 400, 364),
                            new int[] { InsetsState.ITYPE_CAPTION_BAR });
        }

        verify(mMockSurfaceControlFinishT)
                .setPosition(taskSurface, TASK_POSITION_IN_PARENT.x, TASK_POSITION_IN_PARENT.y);
        verify(mMockSurfaceControlFinishT)
                .setCrop(taskSurface, new Rect(-20, -40, 360, 180));
        verify(mMockSurfaceControlStartT)
                .show(taskSurface);

        assertEquals(380, mRelayoutResult.mWidth);
        assertEquals(220, mRelayoutResult.mHeight);
        assertEquals(2, mRelayoutResult.mDensity, 0.f);
    }

    @Test
    public void testNotCrashWhenDisplayAppearsAfterTask() {
        doReturn(mock(Display.class)).when(mMockDisplayController)
                .getDisplay(Display.DEFAULT_DISPLAY);

        final int displayId = Display.DEFAULT_DISPLAY + 1;
        final ActivityManager.TaskDescription.Builder taskDescriptionBuilder =
                new ActivityManager.TaskDescription.Builder()
                        .setBackgroundColor(Color.BLACK);
        final ActivityManager.RunningTaskInfo taskInfo = new TestRunningTaskInfoBuilder()
                .setDisplayId(displayId)
                .setTaskDescriptionBuilder(taskDescriptionBuilder)
                .setVisible(true)
                .build();

        final TestWindowDecoration windowDecor =
                createWindowDecoration(taskInfo, new SurfaceControl());
        windowDecor.relayout(taskInfo);

        // It shouldn't show the window decoration when it can't obtain the display instance.
        assertThat(mRelayoutResult.mRootView).isNull();

        final ArgumentCaptor<DisplayController.OnDisplaysChangedListener> listenerArgumentCaptor =
                ArgumentCaptor.forClass(DisplayController.OnDisplaysChangedListener.class);
        verify(mMockDisplayController).addDisplayWindowListener(listenerArgumentCaptor.capture());
        final DisplayController.OnDisplaysChangedListener listener =
                listenerArgumentCaptor.getValue();

        // Adding an irrelevant display shouldn't change the result.
        listener.onDisplayAdded(Display.DEFAULT_DISPLAY);
        assertThat(mRelayoutResult.mRootView).isNull();

        final Display mockDisplay = mock(Display.class);
        doReturn(mockDisplay).when(mMockDisplayController).getDisplay(displayId);

        listener.onDisplayAdded(displayId);

        // The listener should be removed when the display shows up.
        verify(mMockDisplayController).removeDisplayWindowListener(same(listener));

        assertThat(mRelayoutResult.mRootView).isSameInstanceAs(mMockView);
        verify(mMockSurfaceControlViewHostFactory)
                .create(any(), eq(mockDisplay), any(), anyBoolean());
        verify(mMockSurfaceControlViewHost).setView(same(mMockView), any());
    }

    private TestWindowDecoration createWindowDecoration(
            ActivityManager.RunningTaskInfo taskInfo, SurfaceControl testSurface) {
        return new TestWindowDecoration(mContext, mMockDisplayController, mMockShellTaskOrganizer,
                taskInfo, testSurface, new MockSurfaceControlBuilderSupplier(),
                mMockSurfaceControlViewHostFactory);
    }

    private class MockSurfaceControlBuilderSupplier implements Supplier<SurfaceControl.Builder> {
        private int mNumOfCalls = 0;

        @Override
        public SurfaceControl.Builder get() {
            final SurfaceControl.Builder builder =
                    mNumOfCalls < mMockSurfaceControlBuilders.size()
                            ? mMockSurfaceControlBuilders.get(mNumOfCalls)
                            : createMockSurfaceControlBuilder(mock(SurfaceControl.class));
            ++mNumOfCalls;
            return builder;
        }
    }

    private static class TestView extends View implements TaskFocusStateConsumer {
        private TestView(Context context) {
            super(context);
        }

        @Override
        public void setTaskFocusState(boolean focused) {}
    }

    private class TestWindowDecoration extends WindowDecoration<TestView> {
        TestWindowDecoration(Context context, DisplayController displayController,
                ShellTaskOrganizer taskOrganizer, ActivityManager.RunningTaskInfo taskInfo,
                SurfaceControl taskSurface,
                Supplier<SurfaceControl.Builder> surfaceControlBuilderSupplier,
                SurfaceControlViewHostFactory surfaceControlViewHostFactory) {
            super(context, displayController, taskOrganizer, taskInfo, taskSurface,
                    surfaceControlBuilderSupplier, surfaceControlViewHostFactory);
        }

        @Override
        void relayout(ActivityManager.RunningTaskInfo taskInfo) {
            relayout(null /* taskInfo */, 0 /* layoutResId */, mMockView, CAPTION_HEIGHT_DP,
                    mOutsetsDp, SHADOW_RADIUS_DP, mMockSurfaceControlStartT,
                    mMockSurfaceControlFinishT, mMockWindowContainerTransaction, mRelayoutResult);
        }
    }
}
