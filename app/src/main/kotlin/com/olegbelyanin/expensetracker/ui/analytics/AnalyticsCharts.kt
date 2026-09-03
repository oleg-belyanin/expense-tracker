package com.olegbelyanin.expensetracker.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.olegbelyanin.expensetracker.domain.expense.AnalyticsCategoryRow
import com.olegbelyanin.expensetracker.ui.format.ExpenseFormat
import com.olegbelyanin.expensetracker.ui.theme.ExpenseTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

data class AnalyticsVisualRow(val row: AnalyticsCategoryRow, val color: Color)

@Composable
fun AnalyticsDonut(
    rows: List<AnalyticsVisualRow>,
    totalLabel: String,
    onRowClick: (AnalyticsCategoryRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    Box(
        modifier = modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
            Modifier
                .size(188.dp)
                .pointerInput(rows) {
                    detectTapGestures { offset ->
                        val hit = hitRow(offset, size.width.toFloat(), size.height.toFloat(), rows)
                        if (hit != null) onRowClick(hit)
                    }
                },
        ) {
            val stroke = 28.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var start = -90f
            rows.forEach { item ->
                val sweep = (item.row.share * 360.0).toFloat()
                if (sweep > 0f) {
                    drawArc(
                        color = item.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = totalLabel,
                style = typography.titleSection,
                color = colors.textPrimary,
            )
            Text(
                text = "100%",
                style = typography.labelSmall,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
fun AnalyticsBars(
    rows: List<AnalyticsVisualRow>,
    onRowClick: (AnalyticsCategoryRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExpenseTheme.colors
    val typography = ExpenseTheme.typography
    val spacing = ExpenseTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        rows.forEach { item ->
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onRowClick(item.row) },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.row.name,
                        style = typography.bodySecondary,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = ExpenseFormat.shareLine(item.row.amountMinor, item.row.sharePercent),
                        style = typography.bodySecondary,
                        color = colors.textSecondary,
                    )
                }
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(colors.surfaceSubtle, ExpenseTheme.radii.full),
                ) {
                    Box(
                        modifier =
                        Modifier
                            .fillMaxWidth(item.row.share.toFloat().coerceIn(0f, 1f))
                            .height(10.dp)
                            .background(item.color, ExpenseTheme.radii.full),
                    )
                }
            }
        }
    }
}

private fun hitRow(
    offset: Offset,
    width: Float,
    height: Float,
    rows: List<AnalyticsVisualRow>,
): AnalyticsCategoryRow? {
    val center = Offset(width / 2f, height / 2f)
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = hypot(dx.toDouble(), dy.toDouble())
    val outer = minOf(width, height) / 2.0
    val inner = outer - outer * 0.35
    if (distance < inner || distance > outer) return null
    val degrees = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI + 90.0 + 360.0) % 360.0
    var start = 0.0
    rows.forEach { item ->
        val sweep = item.row.share * 360.0
        if (degrees >= start && degrees < start + sweep) {
            return item.row
        }
        start += sweep
    }
    return rows.lastOrNull()?.row
}
