package com.finoria.app.ui.analyses

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.AnalysisType
import com.finoria.app.data.model.CategoryData
import com.finoria.app.util.formattedCurrency
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Graphique camembert (donut) dessiné via Canvas.
 * Remplace Swift Charts SectorMark.
 *
 * La sélection est identifiée par [CategoryData.selectionKey] (nom d'enum ou id
 * de catégorie personnalisée) : chaque catégorie perso a sa propre part.
 */
@Composable
fun AnalysesPieChart(
    data: List<CategoryData>,
    total: Double,
    analysisType: AnalysisType,
    selectedKey: String?,
    onCategorySelected: (CategoryData) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation progress
    var animationProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 800),
        label = "pieAnimation"
    )
    LaunchedEffect(data) { animationProgress = 1f }

    val strokeWidth = 48f
    val selectedStrokeWidth = 58f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) / 2f
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)

                        // Only detect taps on the donut ring
                        if (distance < radius - strokeWidth || distance > radius + strokeWidth / 2) return@detectTapGestures

                        var angle = Math
                            .toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                            .toFloat()
                        // Normalize to 0-360 with -90 start offset
                        angle = (angle + 90f + 360f) % 360f

                        var startAngle = 0f
                        for (item in data) {
                            val sweepAngle = item.percentage * 360f
                            if (angle in startAngle..(startAngle + sweepAngle)) {
                                onCategorySelected(item)
                                return@detectTapGestures
                            }
                            startAngle += sweepAngle
                        }
                    }
                }
        ) {
            val canvasSize = min(size.width, size.height)
            val arcSize = Size(canvasSize, canvasSize)
            val topLeft = Offset(
                (size.width - canvasSize) / 2f,
                (size.height - canvasSize) / 2f
            )

            var startAngle = -90f // Start from top
            for (item in data) {
                val sweepAngle = item.percentage * 360f * animatedProgress
                val isSelected = selectedKey == item.selectionKey
                val currentStroke = if (isSelected) selectedStrokeWidth else strokeWidth
                val alpha = if (selectedKey != null && !isSelected) 0.3f else 1f

                drawArc(
                    color = item.color.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = currentStroke)
                )
                startAngle += sweepAngle
            }
        }

        // Center text showing total or selected category
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (selectedKey != null) {
                val selectedData = data.firstOrNull { it.selectionKey == selectedKey }
                selectedData?.let {
                    Text(
                        text = it.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = it.amount.formattedCurrency(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${String.format(java.util.Locale.FRANCE, "%.0f", it.percentage * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = analysisType.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = total.formattedCurrency(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
