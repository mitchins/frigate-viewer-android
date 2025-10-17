package com.example.frigateviewer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.max

/**
 * A justified mosaic grid that preserves item aspect ratios and optimizes
 * screen usage by adjusting row heights so each row spans the full width.
 *
 * - Items are laid out in rows. For a target row height, items in a row are
 *   scaled so their total width exactly fills the available width.
 * - The target row height is adjusted to fit the container height (when bounded).
 */
@Composable
fun <T> MosaicGrid(
    items: List<T>,
    aspectRatio: (T) -> Float, // width / height
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            items.forEach { item ->
                content(item)
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val boundedHeight = constraints.hasBoundedHeight
        val maxHeight = constraints.maxHeight
        val n = items.size
        if (n == 0 || width == 0) {
            return@Layout layout(width, if (boundedHeight) maxHeight else 0) {}
        }

        val ratios = items.map { r -> max(0.1f, aspectRatio(r)) }

        fun rowsForCount(rowsCount: Int): Pair<List<List<Float>>, Int> {
            val rows = mutableListOf<List<Float>>()
            var idx = 0
            val base = n / rowsCount
            val extra = n % rowsCount
            repeat(rowsCount) { r ->
                val take = base + if (r < extra) 1 else 0
                if (take > 0) {
                    rows.add(ratios.subList(idx, idx + take))
                    idx += take
                }
            }
            // compute total height when each row is scaled to fill width
            var totalH = 0
            rows.forEach { row ->
                val sumR = row.sum()
                if (sumR <= 0f) return@forEach
                val rowH = (width / sumR).toInt()
                totalH += rowH
            }
            return rows to totalH
        }

        // Choose optimal rows count
        val (rowsRatios, totalH) = if (boundedHeight) {
            var bestRows: List<List<Float>> = listOf()
            var bestTotal = 0
            var bestDiff = Int.MAX_VALUE
            for (rowsCount in 1..n) {
                val (rows, h) = rowsForCount(rowsCount)
                val diff = kotlin.math.abs(maxHeight - h)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestRows = rows
                    bestTotal = h
                }
            }
            bestRows to bestTotal
        } else {
            val rowsCount = ceil(kotlin.math.sqrt(n.toFloat())).toInt().coerceAtLeast(1)
            rowsForCount(rowsCount)
        }

        // Measure and place with exact sizes
        val placeables = measurables
        val layoutH = if (boundedHeight) minOf(maxHeight, totalH) else totalH

        layout(width, layoutH) {
            var y = 0
            var childIndex = 0
            rowsRatios.forEach { row ->
                val sumR = row.sum().coerceAtLeast(0.1f)
                val rowH = (width / sumR).toInt().coerceAtLeast(1)
                var x = 0
                row.forEach { r ->
                    val w = (r * rowH).toInt().coerceAtLeast(1)
                    val p = placeables[childIndex].measure(Constraints.fixed(w, rowH))
                    p.placeRelative(x, y)
                    x += w
                    childIndex++
                }
                y += rowH
            }
        }
    }
}
