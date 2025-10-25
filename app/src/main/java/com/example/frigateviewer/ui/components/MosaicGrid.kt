package com.example.frigateviewer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
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
    content: @Composable (item: T, cellAspect: Float) -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val width = constraints.maxWidth
        val boundedHeight = constraints.hasBoundedHeight
        val maxHeight = constraints.maxHeight
        val n = items.size
        if (n == 0 || width == 0) {
            return@SubcomposeLayout layout(width, if (boundedHeight) maxHeight else 0) {}
        }

        val ratios = items.map { r -> max(0.1f, aspectRatio(r)) }

        // Given a target row height, greedily pack items into rows so
        // each row's width meets/exceeds container width, then scale
        // row height to exactly fill width.
        fun buildRows(targetH: Int): Pair<List<List<Float>>, Int> {
            val rows = mutableListOf<List<Float>>()
            var i = 0
            var totalH = 0
            while (i < ratios.size) {
                var sum = 0f
                val row = mutableListOf<Float>()
                while (i < ratios.size && sum < width) {
                    val r = ratios[i]
                    row.add(r)
                    sum += r * targetH
                    i++
                }
                if (row.isEmpty()) {
                    row.add(ratios[i])
                    i++
                    sum = row[0] * targetH
                }
                val rowH = max(1, (width / row.sum().coerceAtLeast(0.1f)).toInt())
                rows.add(row)
                totalH += rowH
            }
            return rows to totalH
        }

        // Choose rows via binary search on target row height to match container height
        val (rowsRatios, baseTotalH) = if (boundedHeight) {
            var low = 1
            var high = max(2, maxHeight * 2)
            var bestRows: List<List<Float>> = emptyList()
            var bestH = Int.MAX_VALUE
            repeat(12) {
                val mid = (low + high) / 2
                val (rows, totalH) = buildRows(mid)
                // Track the closest total height to target
                if (kotlin.math.abs(maxHeight - totalH) < kotlin.math.abs(maxHeight - bestH)) {
                    bestH = totalH
                    bestRows = rows
                }
                if (totalH < maxHeight) {
                    // Too short: increase row height to reduce items per row (more rows)
                    low = mid + 1
                } else {
                    // Too tall: decrease row height (fewer rows)
                    high = mid - 1
                }
            }
            val resultRows = if (bestRows.isEmpty()) buildRows(maxHeight).first else bestRows
            val resultH = if (bestRows.isEmpty()) maxHeight else bestH
            resultRows to resultH
        } else {
            buildRows(200)
        }

        // Measure and place with exact sizes; ensure each row fills width exactly
        val layoutH = if (boundedHeight) maxHeight else baseTotalH

        val placeables = mutableListOf<androidx.compose.ui.layout.Placeable>()
        val positions = mutableListOf<Triple<Int, Int, Int>>() // x, y, index

        var y = 0
        var childIndex = 0
        rowsRatios.forEachIndexed { rowIndex, row ->
            val sumR = row.sum().coerceAtLeast(0.1f)
            val baseRowHf = (width / sumR).coerceAtLeast(1f)
            val isLastRow = rowIndex == rowsRatios.lastIndex
            val rowH = if (boundedHeight && isLastRow) {
                (maxHeight - y).coerceAtLeast(1)
            } else {
                baseRowHf.toInt().coerceAtLeast(1)
            }

            // Integer widths that sum exactly to width
            val rawWidths = row.map { r -> (width * (r / sumR)) }
            val floors = rawWidths.map { it.toInt() }.toMutableList()
            var allocated = floors.sum()
            var remaining = (width - allocated).coerceAtLeast(0)
            if (remaining > 0) {
                val remaindersIdx = rawWidths.mapIndexed { idx, v -> idx to (v - floors[idx]) }
                    .sortedByDescending { it.second }
                var k = 0
                while (remaining > 0 && k < remaindersIdx.size) {
                    val idx = remaindersIdx[k].first
                    floors[idx] = floors[idx] + 1
                    remaining--
                    k++
                    if (k >= remaindersIdx.size) k = 0
                }
            }

            var x = 0
            floors.forEach { wInt ->
                val w = wInt.coerceAtLeast(1)
                val cellAspect = w.toFloat() / rowH.toFloat()
                val meas = subcompose(childIndex) {
                    content(items[childIndex], cellAspect)
                }.first().measure(Constraints.fixed(w, rowH))
                placeables.add(meas)
                positions.add(Triple(x, y, childIndex))
                x += w
                childIndex++
            }
            y += rowH
        }

        layout(width, layoutH) {
            positions.forEachIndexed { idx, (x, yy, _) ->
                placeables[idx].placeRelative(x, yy)
            }
        }
    }
}
