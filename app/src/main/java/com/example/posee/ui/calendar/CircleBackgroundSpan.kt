import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import androidx.annotation.ColorInt

class CircleBackgroundSpan(
    context: Context,
    @ColorInt private val backgroundColor: Int,
    dpRadiusPadding: Float = 4f,    // dp 단위로 전달
    dpMinimumRadius: Float = 24f     // dp 단위로 전달
) : LineBackgroundSpan {

    // 생성자에서 dp → px로 변환
    private val density = context.resources.displayMetrics.density
    private val radiusPaddingPx: Float = dpRadiusPadding * density
    private val minimumRadiusPx: Float = dpMinimumRadius * density

    override fun drawBackground(
        canvas: Canvas, paint: Paint,
        left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNum: Int
    ) {
        // (1) 색상 교체
        val oldColor = paint.color
        paint.color = backgroundColor

        // (2) 텍스트 너비 기준 반지름 후보
        val textWidth = paint.measureText(text, start, end)
        val candidateRadius = textWidth / 2f + radiusPaddingPx

        // (3) dp → px로 변환된 최소 반지름과 비교
        val radius = maxOf(candidateRadius, minimumRadiusPx)

        // (4) 셀 중앙 계산
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f

        // (5) 원 그리기
        canvas.drawCircle(cx, cy, radius, paint)

        // (6) 색상 복원
        paint.color = oldColor
    }
}
