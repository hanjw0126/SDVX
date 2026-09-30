package com.voltexmate.app.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.voltexmate.app.data.Assets
import com.voltexmate.app.data.ChartScore
import com.voltexmate.app.data.Vf
import com.voltexmate.app.data.VfInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** VF 대상곡 1080px PNG */
object VfExporter {
    private val INK = 0xFF2D2A4A.toInt()
    private val SUB = 0xFF8A86A8.toInt()

    private suspend fun loadBitmap(ctx: Context, url: String): Bitmap? = try {
        val req = ImageRequest.Builder(ctx).data(url).allowHardware(false).build()
        (ctx.imageLoader.execute(req) as? SuccessResult)?.drawable?.toBitmap()
    } catch (e: Exception) {
        null
    }

    suspend fun render(ctx: Context, name: String, playerId: String, official: VfInfo?, top: List<ChartScore>): Bitmap {
        val total = Vf.total(top)
        val vf = official ?: VfInfo.of(total, null, null)
        val badge = loadBitmap(ctx, Assets.vfIcon(vf.cls.id))
        return withContext(Dispatchers.Default) { draw(name, playerId, vf, total, top, badge) }
    }

    private fun draw(name: String, playerId: String, vf: VfInfo, total: Double, top: List<ChartScore>, badge: Bitmap?): Bitmap {
        val w = 1080
        val pad = 48f
        val headerBottom = 420f
        val cardH = 156f
        val gap = 20f
        val cw = (w - pad * 2 - gap) / 2f
        val rows = (top.size + 1) / 2
        val h = (headerBottom + pad + rows * (cardH + gap) + 110f).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        p.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), 0xFFF7F4FF.toInt(), 0xFFFFEEF7.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = LinearGradient(pad, pad, w - pad, headerBottom, 0xFF8C7BFF.toInt(), 0xFFFF8FC7.toInt(), Shader.TileMode.CLAMP)
        c.drawRoundRect(RectF(pad, pad, w - pad, headerBottom), 56f, 56f, p)
        p.shader = null

        val b = RectF(pad + 40f, pad + 56f, pad + 40f + 256f, pad + 56f + 256f)
        if (badge != null) {
            c.drawBitmap(badge, null, b, p)
        } else {
            p.shader = LinearGradient(b.left, b.top, b.right, b.bottom, vf.cls.c1.toInt(), vf.cls.c2.toInt(), Shader.TileMode.CLAMP)
            c.drawCircle(b.centerX(), b.centerY(), b.width() / 2f, p)
            p.shader = null
            text(c, p, vf.cls.label, b.centerX(), b.centerY() + 14f, 40f, Color.WHITE, bold, Paint.Align.CENTER)
        }
        text(c, p, "★".repeat(vf.tier), b.centerX(), b.bottom + 44f, 36f, 0xFFFFE27A.toInt(), bold, Paint.Align.CENTER)

        val tx = b.right + 44f
        val maxW = w - pad - 40f - tx
        text(c, p, "VOLFORCE", tx, pad + 100f, 30f, 0xDDFFFFFF.toInt(), bold)
        text(c, p, String.format(Locale.US, "%.3f", vf.value), tx, pad + 200f, 104f, Color.WHITE, bold)
        text(c, p, "${vf.cls.label} ${vf.roman}", tx, pad + 250f, 36f, Color.WHITE, bold)
        text(c, p, fit(p, name, 48f, bold, maxW), tx, pad + 318f, 48f, Color.WHITE, bold)
        val sub = listOf(playerId, String.format(Locale.US, "TOP50 %.3f", total)).filter { it.isNotBlank() }.joinToString("  ·  ")
        text(c, p, fit(p, sub, 26f, bold, maxW), tx, pad + 356f, 26f, 0xDDFFFFFF.toInt(), bold)

        top.forEachIndexed { i, s ->
            val x = pad + (i % 2) * (cw + gap)
            val y = headerBottom + pad + (i / 2) * (cardH + gap)
            p.color = Color.WHITE
            c.drawRoundRect(RectF(x, y, x + cw, y + cardH), 36f, 36f, p)

            p.color = if (i < 3) 0xFFFF8FC7.toInt() else 0x228C7BFF
            c.drawCircle(x + 50f, y + cardH / 2f, 30f, p)
            text(c, p, "${i + 1}", x + 50f, y + cardH / 2f + 11f, 30f, if (i < 3) Color.WHITE else INK, bold, Paint.Align.CENTER)

            val lx = x + 96f
            val right = x + cw - 24f
            text(c, p, "${s.force}", right, y + 72f, 44f, INK, bold, Paint.Align.RIGHT)
            text(c, p, "FORCE", right, y + 106f, 20f, SUB, bold, Paint.Align.RIGHT)

            text(c, p, fit(p, s.title, 30f, bold, right - 110f - lx), lx, y + 52f, 30f, INK, bold)
            val nx = pill(c, p, "${s.difficulty.short} ${s.levelText}", lx, y + 70f, s.difficulty.color.toInt(), bold)
            pill(c, p, s.clear.short, nx, y + 70f, s.clear.color.toInt(), bold)
            text(c, p, String.format(Locale.US, "%,d  ·  %s", s.score, s.grade.label), lx, y + 136f, 24f, SUB, bold)
        }

        val date = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())
        text(c, p, "VOLTEX MATE · unofficial fan-made · $date", w / 2f, h - 44f, 24f, SUB, bold, Paint.Align.CENTER)
        return bmp
    }

    private fun text(
        c: Canvas, p: Paint, s: String, x: Float, y: Float, size: Float, color: Int, tf: Typeface,
        align: Paint.Align = Paint.Align.LEFT,
    ) {
        p.shader = null
        p.textSize = size
        p.typeface = tf
        p.color = color
        p.textAlign = align
        c.drawText(s, x, y, p)
        p.textAlign = Paint.Align.LEFT
    }

    private fun fit(p: Paint, s: String, size: Float, tf: Typeface, max: Float): String {
        p.textSize = size
        p.typeface = tf
        if (p.measureText(s) <= max) return s
        var t = s
        while (t.isNotEmpty() && p.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }

    private fun pill(c: Canvas, p: Paint, label: String, x: Float, top: Float, color: Int, tf: Typeface): Float {
        p.shader = null
        p.textSize = 22f
        p.typeface = tf
        p.textAlign = Paint.Align.LEFT
        val tw = p.measureText(label)
        p.color = (color and 0x00FFFFFF) or 0x30000000
        c.drawRoundRect(RectF(x, top, x + tw + 28f, top + 34f), 17f, 17f, p)
        p.color = color
        c.drawText(label, x + 14f, top + 25f, p)
        return x + tw + 36f
    }

    fun save(ctx: Context, bmp: Bitmap): String {
        val name = "VoltexMate_VF_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VoltexMate")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("갤러리에 저장하지 못했어요")
            val out = ctx.contentResolver.openOutputStream(uri) ?: throw IOException("갤러리에 저장하지 못했어요")
            out.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            return "Pictures/VoltexMate"
        }
        val dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "VoltexMate").apply { mkdirs() }
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return dir.absolutePath
    }

    fun shareUri(ctx: Context, bmp: Bitmap): Uri {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "voltexmate_vf.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    }
}
