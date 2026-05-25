package com.calorietracker.presentation.common.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {

    val ScaleOutlined: ImageVector by lazy {
        ImageVector.Builder(
            name = "ScaleOutlined",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Main body outline
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                close()
            }
            
            // Screen Outline
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 6.5f)
                curveTo(14.8f, 6.5f, 15.5f, 7.2f, 15.5f, 8f)
                curveTo(15.5f, 8.8f, 14.8f, 9.5f, 14f, 9.5f)
                horizontalLineTo(10f)
                curveTo(9.2f, 9.5f, 8.5f, 8.8f, 8.5f, 8f)
                curveTo(8.5f, 7.2f, 9.2f, 6.5f, 10f, 6.5f)
                close()
            }

            // Divider Line
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 14f)
                horizontalLineTo(21f)
            }
        }.build()
    }

    val ScaleFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "ScaleFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            ) {
                // Main body
                moveTo(19f, 2f)
                horizontalLineTo(5f)
                curveTo(3.34f, 2f, 2f, 3.34f, 2f, 5f)
                verticalLineTo(19f)
                curveTo(2f, 20.66f, 3.34f, 22f, 5f, 22f)
                horizontalLineTo(19f)
                curveTo(20.66f, 22f, 22f, 20.66f, 22f, 19f)
                verticalLineTo(5f)
                curveTo(22f, 3.34f, 20.66f, 2f, 19f, 2f)
                close()
                
                // Screen Cutout
                moveTo(14f, 5f)
                curveTo(15.66f, 5f, 17f, 6.34f, 17f, 8f)
                curveTo(17f, 9.66f, 15.66f, 11f, 14f, 11f)
                horizontalLineTo(10f)
                curveTo(8.34f, 11f, 7f, 9.66f, 7f, 8f)
                curveTo(7f, 6.34f, 8.34f, 5f, 10f, 5f)
                close()
                
                // Divider Cutout
                moveTo(2f, 15f)
                horizontalLineTo(22f)
                verticalLineTo(17f)
                horizontalLineTo(2f)
                close()
            }
        }.build()
    }
}
