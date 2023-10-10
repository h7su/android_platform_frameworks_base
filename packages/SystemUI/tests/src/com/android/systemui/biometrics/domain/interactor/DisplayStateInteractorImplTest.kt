package com.android.systemui.biometrics.domain.interactor

import androidx.test.filters.SmallTest
import com.android.systemui.SysuiTestCase
import com.android.systemui.biometrics.data.repository.FakeRearDisplayStateRepository
import com.android.systemui.coroutines.collectLastValue
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider
import com.android.systemui.unfold.updates.FoldProvider
import com.android.systemui.util.concurrency.FakeExecutor
import com.android.systemui.util.mockito.any
import com.android.systemui.util.mockito.withArgCaptor
import com.android.systemui.util.time.FakeSystemClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(JUnit4::class)
class DisplayStateInteractorImplTest : SysuiTestCase() {

    @JvmField @Rule var mockitoRule = MockitoJUnit.rule()

    private val fakeExecutor = FakeExecutor(FakeSystemClock())
    private val testScope = TestScope(StandardTestDispatcher())
    private val rearDisplayStateRepository = FakeRearDisplayStateRepository()

    @Mock private lateinit var screenSizeFoldProvider: ScreenSizeFoldProvider
    private lateinit var interactor: DisplayStateInteractorImpl

    @Before
    fun setup() {
        interactor =
            DisplayStateInteractorImpl(
                testScope.backgroundScope,
                mContext,
                fakeExecutor,
                rearDisplayStateRepository
            )
        interactor.setScreenSizeFoldProvider(screenSizeFoldProvider)
    }

    @Test
    fun isInRearDisplayModeChanges() =
        testScope.runTest {
            val isInRearDisplayMode = collectLastValue(interactor.isInRearDisplayMode)

            rearDisplayStateRepository.setIsInRearDisplayMode(false)
            assertThat(isInRearDisplayMode()).isFalse()

            rearDisplayStateRepository.setIsInRearDisplayMode(true)
            assertThat(isInRearDisplayMode()).isTrue()
        }

    @Test
    fun isFoldedChanges() =
        testScope.runTest {
            val isFolded = collectLastValue(interactor.isFolded)
            runCurrent()
            val callback = screenSizeFoldProvider.captureCallback()

            callback.onFoldUpdated(isFolded = true)
            assertThat(isFolded()).isTrue()

            callback.onFoldUpdated(isFolded = false)
            assertThat(isFolded()).isFalse()
        }
}

private fun FoldProvider.captureCallback() =
    withArgCaptor<FoldProvider.FoldCallback> {
        verify(this@captureCallback).registerCallback(capture(), any())
    }
