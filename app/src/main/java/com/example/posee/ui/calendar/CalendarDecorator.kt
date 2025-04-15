package com.example.posee.ui.calendar

import android.graphics.Canvas
import android.graphics.Paint
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class CalendarDecorator(
    private val color: Int,
                        private val dates: Collection<CalendarDay>
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        // 지정한 날짜들과 일치하는지 확인합니다.
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // 커스텀 원형 배경 Span을 추가합니다.
        view.addSpan(CircleBackgroundSpan(color))
    }
}