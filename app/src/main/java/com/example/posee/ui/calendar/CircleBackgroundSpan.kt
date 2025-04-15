package com.example.posee.ui.calendar

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class CircleBackgroundSpan(
    private val backgroundColor: Int,
    private val radiusPadding: Float = 8f,      // 텍스트에 더할 추가 패딩
    private val minimumRadius: Float = 80f      // 최소 반지름 값 (원 크기가 너무 작아지지 않도록)
) : LineBackgroundSpan {
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNum: Int
    ) {
        // 기존 색상 저장 후 배경 색상 적용
        val oldColor = paint.color
        paint.color = backgroundColor

        // 텍스트 너비 측정 후 계산된 반지름
        val textWidth = paint.measureText(text, start, end)
        val candidateRadius = textWidth / 2f + radiusPadding

        // 최소 반지름 값과 비교해서 더 큰 값을 사용
        val radius = maxOf(candidateRadius, minimumRadius)

        // 날짜 셀 중앙의 좌표 계산
        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f

        // 캔버스에 원을 그림
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 원래 색상 복원
        paint.color = oldColor
    }
}