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
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import android.widget.TextView
import android.widget.Toast
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.URLEncoder

private const val HOME_BASE = "https://surfer.home/"
private const val HOME_HTML = """<html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{background:#0a1020;color:#e6edf3;font-family:sans-serif;text-align:center;padding-top:28vh}
h1{font-weight:300;letter-spacing:3px}input{width:80%;padding:14px;border-radius:24px;border:0;font-size:16px}</style></head>
<body><h1>Surfer</h1><form action="https://www.google.com/search"><input name="q" placeholder="Google'da qidirish..."></form></body></html>"""

private val BLOCKED_HOSTS = listOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
    "adnxs.com", "taboola.com", "outbrain.com", "scorecardresearch.com", "criteo.com", "criteo.net",
    "popads.net", "propellerads.com", "adsterra.com", "moatads.com", "pubmatic.com",
    "rubiconproject.com", "openx.net", "adsrvr.org", "amazon-adsystem.com", "advertising.com",
    "2mdn.net", "adform.net", "smartadserver.com", "yieldmo.com", "casalemedia.com",
    "indexww.com", "sharethrough.com", "teads.tv", "mgid.com", "revcontent.com", "zedo.com",
    "adcolony.com", "applovin.com", "unityads.unity3d.com", "mopub.com", "inmobi.com",
    "chartboost.com", "exoclick.com", "juicyads.com", "trafficjunky.net", "adskeeper.com",
    "ad.gt", "bidswitch.net", "contextweb.com", "lijit.com", "33across.com", "quantserve.com"
)

private const val DARK_JS = """(function(){if(document.getElementById('surfer-dark'))return;var s=document.createElement('style');s.id='surfer-dark';s.textContent='html{background:#fff!important;filter:invert(1) hue-rotate(180deg)!important}img,video,picture,canvas,iframe[src*="youtube"]{filter:invert(1) hue-rotate(180deg)!important}';(document.head||document.documentElement).appendChild(s);})();"""

private const val ADHIDE_JS = """(function(){if(document.getElementById('surfer-ads'))return;var s=document.createElement('style');s.id='surfer-ads';s.textContent='ins.adsbygoogle,[id^="div-gpt-ad"],[id^="google_ads_iframe"],iframe[src*="doubleclick.net"],iframe[src*="googlesyndication.com"],.ytp-ad-overlay-container,.ytp-ad-image-overlay,ytd-ad-slot-renderer,ytd-promoted-sparkles-web-renderer,#masthead-ad{display:none!important}';(document.head||document.documentElement).appendChild(s);})();"""

private const val YT_JS = """(function(){if(window.__surferYT)return;window.__surferYT=1;setInterval(function(){try{var p=document.querySelector('.html5-video-player');var v=document.querySelector('video');if(p&&v&&p.classList.contains('ad-showing')){if(isFinite(v.duration)&&v.duration>0){v.muted=true;v.currentTime=v.duration;}}var b=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-skip-button-container button');if(b)b.click();}catch(e){}},400);})();"""

private const val DESKTOP_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

private const val INCOGNITO_PROFILE = "incognito"

class MainActivity : Activity() {

    private class Tab(val web: WebView, val incognito: Boolean, var title: String = "Yangi tab")
    private class UserScript(val name: String, val matches: List<String>, val code: String, var enabled: Boolean)

    private val tabs = mutableListOf<Tab>()
    private var cur = 0
    private var adblock = true
    private var dark = false
    private var desktop = false

    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var content: FrameLayout
    private lateinit var tabBtn: Button
    private lateinit var findBar: LinearLayout
    private lateinit var findInput: EditText
    private lateinit var findCount: TextView

    private val prefs by lazy { getSharedPreferences("surfer", Context.MODE_PRIVATE) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun cw() = tabs[cur]
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun onOff(b: Boolean) = if (b) "yoqilgan" else "o'chiq"

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
        adblock = prefs.getBoolean("adblock", true)
        dark = prefs.getBoolean("dark", false)
        wipeIncognito()

        window.statusBarColor = Color.parseColor("#0a1020")
        window.navigationBarColor = Color.parseColor("#0a1020")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a1020"))
        }

        // Yuqori panel: manzil + tablar + menyu
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        address = EditText(this).apply {
            setSingleLine()
            hint = "Google'da qidiring yoki manzil kiriting"
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

        // Sahifada qidirish paneli
        findCount = TextView(this).apply {
            setTextColor(Color.GRAY)
            textSize = 12f
            setPadding(dp(6), 0, dp(6), 0)
        }
        findInput = EditText(this).apply {
            setSingleLine()
            hint = "Sahifada qidirish"
            textSize = 14f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.parseColor("#1e262e"))
            setPadding(dp(12), dp(8), dp(12), dp(8))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val q = s?.toString() ?: ""
                    if (q.isEmpty()) {
                        cw().web.clearMatches()
                        findCount.text = ""
                    } else {
                        cw().web.findAllAsync(q)
                    }
                }
            })
        }
        findBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), dp(4))
            visibility = View.GONE
        }
        findBar.addView(findInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        findBar.addView(findCount)
        findBar.addView(btn("\u25B2") { cw().web.findNext(false) })
        findBar.addView(btn("\u25BC") { cw().web.findNext(true) })
        findBar.addView(btn("\u2715") { hideFind() })

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
        root.addView(findBar)
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottom)
        setContentView(root)

        newTab(intent?.data?.toString())
    }

    private fun loadHome(w: WebView) = w.loadDataWithBaseURL(HOME_BASE, HOME_HTML, "text/html", "utf-8", null)

    @SuppressLint("SetJavaScriptEnabled")
    private fun newTab(url: String? = null, incognito: Boolean = false) {
        val w = WebView(this)
        if (incognito) {
            var isolated = false
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                try {
                    WebViewCompat.setProfile(w, INCOGNITO_PROFILE)
                    isolated = true
                } catch (_: Exception) { }
            }
            if (!isolated) toast("Diqqat: bu telefonda cookie'lar to'liq ajratilmagan")
            w.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            w.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        } else {
            w.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        }
        val tab = Tab(w, incognito)
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

            override fun onPageCommitVisible(v: WebView?, u: String?) {
                if (v != null && u != null) applyStyles(v, u)
            }

            override fun onPageFinished(v: WebView?, u: String?) {
                if (v == null || u == null) return
                if (v === cw().web) showUrl(u)
                applyStyles(v, u)
                runUserScripts(v, u)
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
                if (!adblock || r == null || r.isForMainFrame) return null
                val h = r.url.host ?: return null
                val p = r.url.path ?: ""
                if (BLOCKED_HOSTS.any { h == it || h.endsWith(".$it") } ||
                    p.contains("/pagead/") || p.startsWith("/api/stats/ads")
                ) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
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
        w.setFindListener { active, total, _ ->
            if (w === cw().web) findCount.text = if (total == 0) "0" else "${active + 1}/$total"
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
        address.setBackgroundColor(Color.parseColor(if (tabs[i].incognito) "#3a1e4a" else "#1e262e"))
        findCount.text = ""
        showUrl(cw().web.url ?: "")
    }

    private fun closeTab() {
        val t = tabs.removeAt(cur)
        content.removeView(t.web)
        t.web.destroy()
        if (t.incognito && tabs.none { it.incognito }) wipeIncognito()
        if (tabs.isEmpty()) newTab() else switchTo(minOf(cur, tabs.size - 1))
    }

    private fun wipeIncognito() {
        try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                ProfileStore.getInstance().deleteProfile(INCOGNITO_PROFILE)
            }
        } catch (_: Exception) { }
    }

    private fun showUrl(u: String) {
        val hidden = u.startsWith(HOME_BASE) || u.startsWith("data:") || u == "about:blank"
        if (!address.hasFocus()) address.setText(if (hidden) "" else u)
    }

    private fun hideKeyboard(v: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun go(raw: String) {
        val s = raw.trim()
        if (s.isEmpty()) return
        val url = when {
            s.startsWith("http://") || s.startsWith("https://") -> s
            !s.contains(' ') && (s.contains('.') || s.startsWith("localhost")) -> "https://$s"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(s, "UTF-8")
        }
        hideKeyboard(address)
        address.clearFocus()
        cw().web.loadUrl(url)
        cw().web.requestFocus()
    }

    // ---------- Tungi rejim, reklama, skriptlar ----------

    private fun applyStyles(w: WebView, url: String) {
        if (dark && !url.startsWith(HOME_BASE)) w.evaluateJavascript(DARK_JS, null)
        if (adblock) {
            w.evaluateJavascript(ADHIDE_JS, null)
            val host = Uri.parse(url).host ?: ""
            if (host.endsWith("youtube.com")) w.evaluateJavascript(YT_JS, null)
        }
    }

    private fun runUserScripts(w: WebView, url: String) {
        for (s in loadScripts()) {
            if (s.enabled && s.matches.any { globMatch(it, url) }) w.evaluateJavascript(s.code, null)
        }
    }

    private fun globMatch(pattern: String, url: String): Boolean =
        Regex(pattern.split("*").joinToString(".*") { Regex.escape(it) }).matches(url)

    private fun loadScripts(): MutableList<UserScript> {
        val out = mutableListOf<UserScript>()
        try {
            val arr = JSONArray(prefs.getString("scripts", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val m = o.getJSONArray("m")
                out.add(UserScript(o.getString("n"), (0 until m.length()).map { m.getString(it) }, o.getString("c"), o.getBoolean("e")))
            }
        } catch (_: Exception) { }
        return out
    }

    private fun saveScripts(list: List<UserScript>) {
        val arr = JSONArray()
        for (s in list) {
            arr.put(JSONObject().put("n", s.name).put("m", JSONArray(s.matches)).put("c", s.code).put("e", s.enabled))
        }
        prefs.edit().putString("scripts", arr.toString()).apply()
    }

    private fun parseScript(src: String): UserScript {
        var name = "Skript"
        val m = mutableListOf<String>()
        Regex("""//\s*@(\w+)\s+(.+)""").findAll(src).forEach { r ->
            val k = r.groupValues[1]
            val v = r.groupValues[2].trim()
            if (k == "name") name = v
            if (k == "match" || k == "include") m.add(v)
        }
        if (m.isEmpty()) m.add("*")
        return UserScript(name, m, src, true)
    }

    private fun showScripts() {
        val list = loadScripts()
        val titles = list.map { (if (it.enabled) "\u2713 " else "\u2717 ") + it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Skriptlar" + (if (list.isEmpty()) " (hali yo'q)" else ""))
            .setItems(titles) { _, i -> scriptActions(list, i) }
            .setPositiveButton("+ Qo'shish") { _, _ -> addScriptDialog() }
            .setNegativeButton("Yopish", null)
            .show()
    }

    private fun scriptActions(list: MutableList<UserScript>, i: Int) {
        val s = list[i]
        AlertDialog.Builder(this)
            .setTitle(s.name)
            .setItems(arrayOf(if (s.enabled) "O'chirib qo'yish" else "Yoqish", "O'chirish")) { _, k ->
                if (k == 0) {
                    s.enabled = !s.enabled
                } else {
                    list.removeAt(i)
                }
                saveScripts(list)
                toast("Saqlandi")
            }
            .show()
    }

    private fun addScriptDialog() {
        val et = EditText(this).apply {
            hint = "// ==UserScript==\n// @name ...\n// @match https://site.com/*\n// ==/UserScript==\nkod..."
            minLines = 8
            gravity = Gravity.TOP
        }
        AlertDialog.Builder(this)
            .setTitle("Yangi skript")
            .setView(et)
            .setPositiveButton("Saqlash") { _, _ ->
                val src = et.text.toString().trim()
                if (src.isNotEmpty()) {
                    val list = loadScripts()
                    list.add(parseScript(src))
                    saveScripts(list)
                    toast("Skript saqlandi")
                }
            }
            .setNegativeButton("Bekor", null)
            .show()
    }

    // ---------- Sahifada qidirish ----------

    private fun showFind() {
        findBar.visibility = View.VISIBLE
        findInput.requestFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(findInput, 0)
    }

    private fun hideFind() {
        findBar.visibility = View.GONE
        cw().web.clearMatches()
        findInput.setText("")
        hideKeyboard(findInput)
    }

    // ---------- Dialoglar ----------

    private fun showTabs() {
        val titles = tabs.mapIndexed { i, t ->
            (if (i == cur) "\u25CF " else "   ") + (i + 1) + ". " + (if (t.incognito) "(maxfiy) " else "") + t.title
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Tablar")
            .setItems(titles) { _, i -> switchTo(i) }
            .setNeutralButton("+ Yangi tab") { _, _ -> newTab() }
            .show()
    }

    private fun showMenu() {
        val items = arrayOf(
            "Yangi tab",
            "Maxfiy tab",
            "Sahifada qidirish",
            "Xatcho'pga qo'shish",
            "Xatcho'plar",
            "Tungi rejim: " + onOff(dark),
            "Kompyuter versiyasi: " + onOff(desktop),
            "Reklama bloklash: " + onOff(adblock),
            "Skriptlar",
            "Tabni yopish",
            "Tarixni tozalash"
        )
        AlertDialog.Builder(this).setItems(items) { _, i ->
            when (i) {
                0 -> { newTab() }
                1 -> { newTab(null, true) }
                2 -> { showFind() }
                3 -> { addBookmark() }
                4 -> { showBookmarks() }
                5 -> {
                    dark = !dark
                    prefs.edit().putBoolean("dark", dark).apply()
                    cw().web.reload()
                }
                6 -> { toggleDesktop() }
                7 -> {
                    adblock = !adblock
                    prefs.edit().putBoolean("adblock", adblock).apply()
                    cw().web.reload()
                }
                8 -> { showScripts() }
                9 -> { closeTab() }
                10 -> { clearData() }
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
            findBar.visibility == View.VISIBLE -> hideFind()
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
