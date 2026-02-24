package com.finoria.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.finoria.app.data.model.Transaction
import com.finoria.app.ui.theme.IncomeGreen
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Transaction row with swipe actions.
 * The card slides to reveal colored action areas underneath:
 * - Swipe right → green/blue area with edit/validate icon
 * - Swipe left → red area with delete icon
 */
@Composable
fun SwipeableTransactionRow(
    transaction: Transaction,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onValidate: ((Transaction) -> Unit)? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val triggerThresholdPx = with(density) { 100.dp.toPx() }

    var rawOffset by remember { mutableFloatStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = rawOffset,
        animationSpec = tween(durationMillis = if (rawOffset == 0f) 250 else 0),
        label = "swipeOffset"
    )

    val isPotential = transaction.potentiel && onValidate != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // ─── Background actions revealed when card slides ────────
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Left side (revealed on swipe right) — Edit / Validate
            if (animatedOffset > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isPotential) IncomeGreen else MaterialTheme.colorScheme.primary)
                        .padding(start = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        imageVector = if (isPotential) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isPotential) "Valider" else "Modifier",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Right side (revealed on swipe left) — Delete
            if (animatedOffset < 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ─── Foreground card that slides ─────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(transaction.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (rawOffset > triggerThresholdPx) {
                                // Swipe right → validate or edit
                                if (isPotential) {
                                    onValidate?.invoke(transaction)
                                } else {
                                    onEdit(transaction)
                                }
                            } else if (rawOffset < -triggerThresholdPx) {
                                // Swipe left → delete
                                onDelete(transaction)
                            }
                            rawOffset = 0f
                        },
                        onDragCancel = { rawOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            rawOffset = (rawOffset + dragAmount)
                                .coerceIn(-triggerThresholdPx * 1.5f, triggerThresholdPx * 1.5f)
                        }
                    )
                },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = if (abs(animatedOffset) > 0.5f) 2.dp else 0.dp
        ) {
            TransactionRow(
                transaction = transaction,
                onClick = onClick
            )
        }
    }
}
