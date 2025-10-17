package com.example.frigateviewer.data.model

/**
 * Different layout options for viewing multiple cameras
 */
enum class ViewLayout(val columns: Int, val maxCameras: Int, val displayName: String) {
    SINGLE(1, 1, "Single View"),
    GRID_2X2(2, 4, "2x2 Grid"),
    GRID_3X3(3, 9, "3x3 Grid");

    companion object {
        fun fromColumns(columns: Int): ViewLayout {
            return entries.find { it.columns == columns } ?: SINGLE
        }
    }
}
