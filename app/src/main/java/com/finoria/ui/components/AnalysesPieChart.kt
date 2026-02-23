package com.finoria.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import com.finoria.ui.screens.analyses.CategoryData

@Composable
fun AnalysesPieChart(
    data: List<CategoryData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val total = data.sumOf { it.percentage.toDouble() }.toFloat()
        var startAngle = -90f

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
        ) {
            data.forEach { categoryData ->
                val sweepAngle = (categoryData.percentage / total) * 360f
                drawArc(
                    color = categoryData.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
    }
}