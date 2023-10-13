package com.android.systemui.communal.ui.view.layout.blueprints

import android.testing.TestableLooper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.systemui.SysuiTestCase
import com.android.systemui.communal.ui.view.layout.sections.DefaultCommunalHubSection
import com.android.systemui.communal.ui.view.layout.sections.DefaultCommunalWidgetSection
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
@TestableLooper.RunWithLooper(setAsMainLooper = true)
@SmallTest
class DefaultCommunalBlueprintTest : SysuiTestCase() {
    @Mock private lateinit var hubSection: DefaultCommunalHubSection
    @Mock private lateinit var widgetSection: DefaultCommunalWidgetSection

    private lateinit var blueprint: DefaultCommunalBlueprint

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        blueprint = DefaultCommunalBlueprint(hubSection, widgetSection)
    }

    @Test
    fun addView() {
        val constraintLayout = ConstraintLayout(context, null)
        blueprint.replaceViews(null, constraintLayout)
        verify(hubSection).addViews(constraintLayout)
        verify(widgetSection).addViews(constraintLayout)
    }

    @Test
    fun applyConstraints() {
        val cs = ConstraintSet()
        blueprint.applyConstraints(cs)
        verify(hubSection).applyConstraints(cs)
        verify(widgetSection).applyConstraints(cs)
    }
}
