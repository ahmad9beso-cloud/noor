package app.noor.islamic.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Small line-icon set drawn in code, so the app needs no icon library. All icons live on a 24x24 grid. */
enum class Ic {
    Home, Quran, Adhkar, Compass, Tune, Bookmark, BookmarkFill, ChevronR, ChevronL,
    Search, Bell, BellOff, Close, Check, Pin, Pages, Scroll,
}

@Composable
fun NIcon(ic: Ic, modifier: Modifier = Modifier, tint: Color = Noor.colors.text, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        scale(s, Offset.Zero) { drawIc(ic, tint) }
    }
}

private fun DrawScope.stroke(p: Path, c: Color, w: Float = 1.8f) {
    drawPath(p, c, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.line(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float = 1.8f) {
    drawLine(c, Offset(x1, y1), Offset(x2, y2), strokeWidth = w, cap = StrokeCap.Round)
}

private fun DrawScope.drawIc(ic: Ic, c: Color) {
    when (ic) {
        Ic.Home -> {
            val p = Path()
            p.moveTo(3.5f, 11f); p.lineTo(12f, 3.8f); p.lineTo(20.5f, 11f)
            p.moveTo(5.5f, 9.6f); p.lineTo(5.5f, 20f); p.lineTo(18.5f, 20f); p.lineTo(18.5f, 9.6f)
            p.moveTo(10f, 20f); p.lineTo(10f, 14f); p.lineTo(14f, 14f); p.lineTo(14f, 20f)
            stroke(p, c)
        }
        Ic.Quran -> {
            val p = Path()
            p.moveTo(12f, 6.2f)
            p.cubicTo(9.8f, 4.7f, 6.6f, 4.4f, 3.6f, 5.2f)
            p.lineTo(3.6f, 18.8f)
            p.cubicTo(6.6f, 18f, 9.8f, 18.3f, 12f, 19.8f)
            p.cubicTo(14.2f, 18.3f, 17.4f, 18f, 20.4f, 18.8f)
            p.lineTo(20.4f, 5.2f)
            p.cubicTo(17.4f, 4.4f, 14.2f, 4.7f, 12f, 6.2f)
            p.moveTo(12f, 6.2f); p.lineTo(12f, 19.8f)
            stroke(p, c)
        }
        Ic.Adhkar -> {
            for (k in 0 until 8) {
                val a = Math.toRadians(k * 45.0 - 90.0)
                drawCircle(c, radius = 1.35f, center = Offset(12f + 5.8f * cos(a).toFloat(), 9.5f + 5.8f * sin(a).toFloat()))
            }
            line(c, 12f, 16.9f, 12f, 19.2f)
            drawCircle(c, radius = 1.5f, center = Offset(12f, 20.6f))
        }
        Ic.Compass -> {
            drawCircle(c, radius = 9f, center = Offset(12f, 12f), style = Stroke(width = 1.8f))
            val north = Path()
            north.moveTo(12f, 5.6f); north.lineTo(14.3f, 12f); north.lineTo(9.7f, 12f); north.close()
            drawPath(north, c)
            val south = Path()
            south.moveTo(14.3f, 12f); south.lineTo(12f, 18.4f); south.lineTo(9.7f, 12f)
            stroke(south, c, 1.5f)
        }
        Ic.Tune -> {
            line(c, 4f, 6f, 6.6f, 6f); line(c, 11.4f, 6f, 20f, 6f)
            drawCircle(c, radius = 2.4f, center = Offset(9f, 6f), style = Stroke(width = 1.8f))
            line(c, 4f, 12f, 12.6f, 12f); line(c, 17.4f, 12f, 20f, 12f)
            drawCircle(c, radius = 2.4f, center = Offset(15f, 12f), style = Stroke(width = 1.8f))
            line(c, 4f, 18f, 5.6f, 18f); line(c, 10.4f, 18f, 20f, 18f)
            drawCircle(c, radius = 2.4f, center = Offset(8f, 18f), style = Stroke(width = 1.8f))
        }
        Ic.Bookmark, Ic.BookmarkFill -> {
            val p = Path()
            p.moveTo(7f, 4f); p.lineTo(17f, 4f); p.lineTo(17f, 20.5f); p.lineTo(12f, 16.5f); p.lineTo(7f, 20.5f); p.close()
            if (ic == Ic.BookmarkFill) drawPath(p, c)
            stroke(p, c)
        }
        Ic.ChevronR -> {
            val p = Path()
            p.moveTo(9f, 5.5f); p.lineTo(15.5f, 12f); p.lineTo(9f, 18.5f)
            stroke(p, c, 2f)
        }
        Ic.ChevronL -> {
            val p = Path()
            p.moveTo(15f, 5.5f); p.lineTo(8.5f, 12f); p.lineTo(15f, 18.5f)
            stroke(p, c, 2f)
        }
        Ic.Search -> {
            drawCircle(c, radius = 6f, center = Offset(10.5f, 10.5f), style = Stroke(width = 1.8f))
            line(c, 15f, 15f, 20f, 20f)
        }
        Ic.Bell, Ic.BellOff -> {
            val p = Path()
            p.moveTo(6f, 17f)
            p.cubicTo(6f, 17f, 7f, 15.5f, 7f, 12f)
            p.cubicTo(7f, 9f, 9f, 7f, 12f, 7f)
            p.cubicTo(15f, 7f, 17f, 9f, 17f, 12f)
            p.cubicTo(17f, 15.5f, 18f, 17f, 18f, 17f)
            p.close()
            stroke(p, c)
            line(c, 12f, 4.6f, 12f, 7f)
            val clapper = Path()
            clapper.moveTo(10.2f, 19.4f)
            clapper.cubicTo(10.8f, 20.6f, 13.2f, 20.6f, 13.8f, 19.4f)
            stroke(clapper, c)
            if (ic == Ic.BellOff) line(c, 4.5f, 4.5f, 19.5f, 19.5f, 2f)
        }
        Ic.Close -> {
            line(c, 6f, 6f, 18f, 18f, 2f)
            line(c, 18f, 6f, 6f, 18f, 2f)
        }
        Ic.Check -> {
            val p = Path()
            p.moveTo(5f, 12.5f); p.lineTo(10f, 17.5f); p.lineTo(19f, 7f)
            stroke(p, c, 2.2f)
        }
        Ic.Pin -> {
            val p = Path()
            p.moveTo(12f, 21.2f)
            p.cubicTo(8f, 17.2f, 5.5f, 13.8f, 5.5f, 10.2f)
            p.cubicTo(5.5f, 6.6f, 8.4f, 3.8f, 12f, 3.8f)
            p.cubicTo(15.6f, 3.8f, 18.5f, 6.6f, 18.5f, 10.2f)
            p.cubicTo(18.5f, 13.8f, 16f, 17.2f, 12f, 21.2f)
            p.close()
            stroke(p, c)
            drawCircle(c, radius = 2.4f, center = Offset(12f, 10.2f), style = Stroke(width = 1.8f))
        }
        Ic.Pages -> {
            val p = Path()
            p.moveTo(4f, 5.5f); p.lineTo(20f, 5.5f); p.lineTo(20f, 18.5f); p.lineTo(4f, 18.5f); p.close()
            p.moveTo(12f, 5.5f); p.lineTo(12f, 18.5f)
            stroke(p, c)
        }
        Ic.Scroll -> {
            line(c, 12f, 4.5f, 12f, 19.5f)
            val up = Path()
            up.moveTo(7.5f, 9f); up.lineTo(12f, 4.5f); up.lineTo(16.5f, 9f)
            stroke(up, c)
            val down = Path()
            down.moveTo(7.5f, 15f); down.lineTo(12f, 19.5f); down.lineTo(16.5f, 15f)
            stroke(down, c)
        }
    }
}
