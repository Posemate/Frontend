package com.example.posee.ui.calendar

import CircleBackgroundSpan
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class CalendarDecorator(
    private val context: Context,
    private val color: Int,
    private val dates: Collection<CalendarDay>
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // Context와 color를 함께 넘겨줍니다.
        val span = CircleBackgroundSpan(
            context         = context,
            backgroundColor = color,
            dpRadiusPadding = 8f,   // 필요에 따라 조절
            dpMinimumRadius = 24f   // 필요에 따라 조절
        )
        view.addSpan(span)
    }
}
