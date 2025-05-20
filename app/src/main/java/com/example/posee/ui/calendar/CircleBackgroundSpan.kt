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
        val oldColor = paint.color
        paint.color = backgroundColor
        paint.isAntiAlias = true

        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f

        val cellWidth = right - left
        val cellHeight = bottom - top

        // 원 크기 수동 조절
        val radius = (minOf(cellWidth, cellHeight) / 2f) * 2.5f

        val ovalRect = android.graphics.RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawOval(ovalRect, paint)

        paint.color = oldColor
    }
}
