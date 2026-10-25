package uz.artic.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.net.URLEncoder

private const val HOME_BASE = "https://artic.home/"
private const val HOME_HTML = """<html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{background:#101418;color:#e6edf3;font-family:sans-serif;text-align:center;padding-top:28vh}
h1{font-weight:300;letter-spacing:2px}input{width:80%;padding:14px;border-radius:24px;border:0;font-size:16px}</style></head>
<body><h1>ArcBrowser</h1><form action="https://duckduckgo.com/"><input name="q" placeholder="Qidirish..."></form></body></html>"""

private val BLOCKED_HOSTS = listOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
    "adnxs.com", "taboola.com", "outbrain.com", "scorecardresearch.com", "criteo.com",
    "popads.net", "propellerads.com", "adsterra.com", "moatads.com", "pubmatic.com"
)

private const val DESKTOP_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

class MainActivity : Activity() {

    private class Tab(val web: WebView, var title: String = "Yangi tab")

    private val tabs = mutableListOf<Tab>()
    private var cur = 0
    private var adblock = true
    private var desktop = false

    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var content: FrameLayout
    private lateinit var tabBtn: Button

    private val prefs by lazy { getSharedPreferences("artic", Context.MODE_PRIVATE) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun cw() = tabs[cur]
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun btn(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        textSize = 18f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT)
        minWidth = 0
        minimumWidth = 0
        setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#101418")
        window.navigationBarColor = Color.parseColor("#101418")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101418"))
        }

        // Yuqori panel: manzil + tablar + menyu
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        address = EditText(this).apply {
            setSingleLine()
            hint = "Qidiring yoki manzil kiriting"
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.parseColor("#1e262e"))
            setPadding(dp(14), dp(10), dp(14), dp(10))
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSelectAllOnFocus(true)
            setOnEditorActionListener { _, _, _ -> go(text.toString()); true }
        }
        tabBtn = btn("1") { showTabs() }
        top.addView(address, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(tabBtn)
        top.addView(btn("\u22EE") { showMenu() })

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }

        content = FrameLayout(this)

        // Pastki panel
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun add(label: String, f: () -> Unit) =
            bottom.addView(btn(label, f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        add("\u25C0") { if (cw().web.canGoBack()) cw().web.goBack() }
        add("\u25B6") { if (cw().web.canGoForward()) cw().web.goForward() }
        add("\u2302") { loadHome(cw().web) }
        add("\u21BB") { cw().web.reload() }
        add("+") { newTab() }

        root.addView(top)
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottom)
        setContentView(root)

        newTab(intent?.data?.toString())
    }

    private fun loadHome(w: WebView) = w.loadDataWithBaseURL(HOME_BASE, HOME_HTML, "text/html", "utf-8", null)

    @SuppressLint("SetJavaScriptEnabled")
    private fun newTab(url: String? = null) {
        val w = WebView(this)
        val tab = Tab(w)
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        w.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                if (v === cw().web) showUrl(u ?: "")
            }

            override fun onPageFinished(v: WebView?, u: String?) {
                if (v === cw().web) showUrl(u ?: "")
            }

            override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                val u = r?.url ?: return false
                when (u.scheme) {
                    "http", "https", "about", "data", "blob", "file" -> return false
                }
                try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (_: Exception) { }
                return true
            }

            override fun shouldInterceptRequest(v: WebView?, r: WebResourceRequest?): WebResourceResponse? {
                if (adblock) {
                    val h = r?.url?.host ?: return null
                    if (BLOCKED_HOSTS.any { h == it || h.endsWith(".$it") }) {
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }
                return null
            }
        }
        w.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView?, p: Int) {
                if (v === cw().web) {
                    progress.progress = p
                    progress.visibility = if (p in 1..99) View.VISIBLE else View.GONE
                }
            }

            override fun onReceivedTitle(v: WebView?, title: String?) {
                tab.title = title ?: "Tab"
            }
        }
        w.setDownloadListener { u, ua, cd, mime, _ -> download(u, ua, cd, mime) }
        w.visibility = View.GONE
        content.addView(w, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        tabs.add(tab)
        switchTo(tabs.size - 1)
        if (url == null) loadHome(w) else w.loadUrl(url)
    }

    private fun switchTo(i: Int) {
        cur = i
        tabs.forEachIndexed { k, t -> t.web.visibility = if (k == i) View.VISIBLE else View.GONE }
        tabBtn.text = tabs.size.toString()
        progress.visibility = View.GONE
        showUrl(cw().web.url ?: "")
    }

    private fun closeTab() {
        val t = tabs.removeAt(cur)
        content.removeView(t.web)
        t.web.destroy()
        if (tabs.isEmpty()) newTab() else switchTo(minOf(cur, tabs.size - 1))
    }

    private fun showUrl(u: String) {
        val hidden = u.startsWith(HOME_BASE) || u.startsWith("data:") || u == "about:blank"
        if (!address.hasFocus()) address.setText(if (hidden) "" else u)
    }

    private fun go(raw: String) {
        val s = raw.trim()
        if (s.isEmpty()) return
        val url = when {
            s.startsWith("http://") || s.startsWith("https://") -> s
            !s.contains(' ') && (s.contains('.') || s.startsWith("localhost")) -> "https://$s"
            else -> "https://duckduckgo.com/?q=" + URLEncoder.encode(s, "UTF-8")
        }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
        address.clearFocus()
        cw().web.loadUrl(url)
        cw().web.requestFocus()
    }

    private fun showTabs() {
        val titles = tabs.mapIndexed { i, t -> (if (i == cur) "\u25CF " else "   ") + (i + 1) + ". " + t.title }
            .toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tablar")
            .setItems(titles) { _, i -> switchTo(i) }
            .setNeutralButton("+ Yangi tab") { _, _ -> newTab() }
            .show()
    }

    private fun showMenu() {
        val items = arrayOf(
            "Yangi tab",
            "Xatcho'pga qo'shish",
            "Xatcho'plar",
            "Kompyuter versiyasi: " + (if (desktop) "yoqilgan" else "o'chiq"),
            "Reklama bloklash: " + (if (adblock) "yoqilgan" else "o'chiq"),
            "Tabni yopish",
            "Tarix va keshni tozalash"
        )
        AlertDialog.Builder(this).setItems(items) { _, i ->
            when (i) {
                0 -> { newTab() }
                1 -> { addBookmark() }
                2 -> { showBookmarks() }
                3 -> { toggleDesktop() }
                4 -> { adblock = !adblock; toast("Reklama bloklash: " + (if (adblock) "yoqildi" else "o'chirildi")) }
                5 -> { closeTab() }
                6 -> { clearData() }
            }
        }.show()
    }

    private fun toggleDesktop() {
        desktop = !desktop
        val s = cw().web.settings
        s.userAgentString = if (desktop) DESKTOP_UA else null
        s.useWideViewPort = true
        cw().web.reload()
    }

    private fun clearData() {
        cw().web.clearHistory()
        cw().web.clearCache(true)
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        toast("Tozalandi")
    }

    private fun addBookmark() {
        val w = cw().web
        val u = w.url ?: return
        if (u.startsWith(HOME_BASE)) { toast("Bosh sahifani qo'shib bo'lmaydi"); return }
        val set = (prefs.getStringSet("bm", emptySet()) ?: emptySet()).toMutableSet()
        set.add(u + "\t" + (w.title ?: u))
        prefs.edit().putStringSet("bm", set).apply()
        toast("Xatcho'pga qo'shildi")
    }

    private fun showBookmarks() {
        val list = (prefs.getStringSet("bm", emptySet()) ?: emptySet()).toList().sorted()
        if (list.isEmpty()) { toast("Xatcho'plar yo'q"); return }
        val titles = list.map { it.substringAfter('\t') }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Xatcho'plar")
            .setItems(titles) { _, i -> cw().web.loadUrl(list[i].substringBefore('\t')) }
            .setNeutralButton("Hammasini o'chirish") { _, _ ->
                prefs.edit().remove("bm").apply()
                toast("O'chirildi")
            }
            .show()
    }

    private fun download(url: String, ua: String?, cd: String?, mime: String?) {
        try {
            val name = URLUtil.guessFileName(url, cd, mime)
            val req = DownloadManager.Request(Uri.parse(url))
                .setMimeType(mime)
                .addRequestHeader("User-Agent", ua ?: "")
                .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            toast("Yuklanmoqda: $name")
        } catch (e: Exception) {
            toast("Yuklab bo'lmadi")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val w = cw().web
        when {
            w.canGoBack() -> w.goBack()
            tabs.size > 1 -> closeTab()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        super.onPause()
    }
}
