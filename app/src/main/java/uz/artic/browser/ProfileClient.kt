package uz.artic.browser   // <- Surfer paketi

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.Gravity
import android.widget.TextView

/** SurfeTheNet'dagi profilni o'qiydi. SurfeTheNet o'rnatilmagan bo'lsa null qaytaradi. */
object ProfileClient {
    data class Profile(val name: String, val emoji: String, val color: Int) {
        val initial: String get() = emoji.ifEmpty { name.trim().take(1).uppercase().ifEmpty { "?" } }
    }

    private val URI = Uri.parse("content://uz.artic.surfethenet.profile/profile")

    fun read(ctx: Context): Profile? = try {
        ctx.contentResolver.query(URI, null, null, null, null)?.use { c ->
            if (!c.moveToFirst()) null else Profile(
                name = c.getString(c.getColumnIndexOrThrow("name")) ?: "",
                emoji = c.getString(c.getColumnIndexOrThrow("emoji")) ?: "",
                color = try { Color.parseColor(c.getString(c.getColumnIndexOrThrow("color"))) }
                        catch (e: Exception) { Color.parseColor("#8ab4f8") }
            )
        }
    } catch (e: Exception) { null }

    /** Tayyor dumaloq avatar: toolbar yoki menyuga qo'shing. */
    fun avatarView(ctx: Context, sizePx: Int): TextView? {
        val p = read(ctx) ?: return null
        return TextView(ctx).apply {
            text = p.initial
            textSize = sizePx / ctx.resources.displayMetrics.density * 0.45f
            setTextColor(Color.parseColor("#202124"))
            gravity = Gravity.CENTER
            width = sizePx; height = sizePx
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(p.color) }
        }
    }
}
