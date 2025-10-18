package com.example.frigateviewer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.max

/**
 * WallPanelGrid: simple fill strategy using a near-uniform grid (rows x cols).
 * - Enumerates candidate (rows, cols) with rows*cols >= n.
 * - Chooses the grid that minimizes the maximum per-tile crop fraction, with
 *   crop limited by [maxCropFraction]. If none fits under the cap, picks the
 *   one with the smallest max crop.
 * - Tiles fill the screen; pass cell aspect to children so they can crop.
 */
@Composable
fun <T> WallPanelGrid(
    items: List<T>,
    aspectRatio: (T) -> Float, // width / height
    maxCropFraction: Float = 0.10f,
    modifier: Modifier = Modifier,
    content: @Composable (item: T, targetAspect: Float?) -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val W = constraints.maxWidth
        val H = constraints.maxHeight
        val bounded = constraints.hasBoundedHeight
        val n = items.size
        if (n == 0 || W == 0 || !bounded) {
            return@SubcomposeLayout layout(W, if (bounded) H else 0) {}
        }

        val ars = items.map { a -> max(0.1f, aspectRatio(a)) }

        data class Candidate(val rows: Int, val cols: Int, val cellAspect: Float, val maxCrop: Float)

        var best: Candidate? = null
        for (rows in 1..n) {
            val cols = ceil(n / rows.toFloat()).toInt()
            if (cols <= 0) continue
            val cellAspect = (W.toFloat() / cols) / (H.toFloat() / rows)
            var maxCrop = 0f
            for (ar in ars) {
                val kept = minOf(ar / cellAspect, cellAspect / ar)
                val crop = 1f - kept
                if (crop > maxCrop) maxCrop = crop
            }
            val cand = Candidate(rows, cols, cellAspect, maxCrop)
            best = when {
                best == null -> cand
                cand.maxCrop <= maxCropFraction && (best!!.maxCrop > maxCropFraction || cand.maxCrop < best!!.maxCrop) -> cand
                cand.maxCrop > maxCropFraction && best!!.maxCrop > maxCropFraction && cand.maxCrop < best!!.maxCrop -> cand
                cand.maxCrop == best!!.maxCrop && rows * cols < best!!.rows * best!!.cols -> cand
                else -> best
            }
        }

        val chosen = best ?: Candidate(1, n, (W.toFloat() / n) / H.toFloat(), 0f)
        val rows = chosen.rows
        val cols = chosen.cols
        val cellW = W / cols
        val cellH = H / rows

        val placeables = mutableListOf<androidx.compose.ui.layout.Placeable>()

        var index = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (index >= n) break
                val cellAspect = (cellW.toFloat() / cellH.toFloat()).coerceAtLeast(0.1f)
                val itemAr = ars[index]
                val kept = minOf(itemAr / cellAspect, cellAspect / itemAr)
                val cropFraction = 1f - kept
                val targetAspect: Float? = if (cropFraction > maxCropFraction) null else cellAspect

                val slotId = index // stable id per position
                val meas = subcompose(slotId) {
                    content(items[index], targetAspect)
                }.first().measure(Constraints.fixed(cellW, cellH))
                placeables.add(meas)
                index++
            }
        }

        layout(W, H) {
            var i = 0
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (i >= placeables.size) break
                    val x = c * cellW
                    val y = r * cellH
                    placeables[i].placeRelative(x, y)
                    i++
                }
            }
        }
    }
}
