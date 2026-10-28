package uz.artic.browser

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
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
import android.widget.ImageButton
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
private const val HOME_CSS = """*{box-sizing:border-box}body{margin:0;background:#202124;color:#f1f3f4;font-family:Arial,sans-serif}.top{display:flex;justify-content:space-between;align-items:center;padding:23px 24px}.menu{font-size:25px;color:#ddd}.avatar{width:43px;height:43px;border-radius:50%;background:linear-gradient(140deg,#00d4ff,#1459ed);display:grid;place-items:center;font-weight:bold;font-size:20px;color:white;border:2px solid #46546a}.w{padding:35px 20px 70px;max-width:720px;margin:auto;text-align:center}.brand{display:flex;align-items:center;justify-content:center;gap:12px;margin:15px 0 44px}.brand img{width:76px;height:76px;border-radius:20px}.brand span{font-size:46px;font-weight:600;letter-spacing:-2px}.search{display:flex;align-items:center;border:1px solid #51545a;border-radius:38px;background:#303134;padding:9px 16px;gap:12px}.search input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:#fff;font-size:17px;padding:13px 0}.search button{background:none;border:0;color:#b6d1ff;font-size:24px}.chips{display:flex;gap:10px;overflow:auto;margin:25px 0 30px}.chip{white-space:nowrap;border-radius:40px;background:#303136;padding:15px 20px;font-size:15px;color:#e7e7e7}.g{display:grid;grid-template-columns:repeat(4,1fr);gap:18px 8px;margin:30px auto;max-width:520px}.t{text-decoration:none;color:#e4e8f0;font-size:12px}.c{display:flex;width:55px;height:55px;border-radius:18px;margin:0 auto 9px;align-items:center;justify-content:center;color:#fff;font-size:23px;font-weight:600}.section{text-align:left;margin:35px 0 14px;font-size:17px}.news{background:#2b2d32;border:1px solid #3b3d42;border-radius:20px;padding:22px;text-align:left}.news h3{margin:0 0 8px;font-size:19px}.news p{color:#adb4bf;line-height:1.5}.b{display:block;text-decoration:none;color:#e6edf3;background:#303136;border-radius:14px;padding:14px;margin:8px 0;text-align:left;font-size:14px;overflow:hidden;text-overflow:ellipsis}"""
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

    private fun ibtn(res: Int, onClick: () -> Unit) = ImageButton(this).apply {
        setImageResource(res)
        setColorFilter(Color.WHITE)
        val tv = TypedValue()
        this@MainActivity.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)
        setBackgroundResource(tv.resourceId)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setOnClickListener { onClick() }
    }

    private fun pill(color: Int, r: Int = 24) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    private fun esc(t: String) = t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun homeHtml(): String {
        val tiles = listOf(
            Triple("Google", "https://www.google.com", "#4285F4"),
            Triple("YouTube", "https://m.youtube.com", "#E53935"),
            Triple("Gmail", "https://mail.google.com", "#EA4335"),
            Triple("Maps", "https://maps.google.com", "#34A853"),
            Triple("Translate", "https://translate.google.com", "#1A73E8"),
            Triple("Drive", "https://drive.google.com", "#F9A825"),
            Triple("Wikipedia", "https://uz.wikipedia.org", "#6B7280"),
            Triple("Telegram", "https://web.telegram.org", "#229ED9")
        )
        val gEmail = prefs.getString("g_email", "") ?: ""
        val name = (prefs.getString("p_name", "") ?: "").ifBlank { gEmail.substringBefore("@") }
            .ifBlank { prefs.getString("local_account", "Mehmon") ?: "Mehmon" }
        val emoji = prefs.getString("p_emoji", "") ?: ""
        val color = prefs.getString("p_color", "") ?: ""
        val initial = esc(emoji.ifEmpty { name.take(1).uppercase() })
        val avatarStyle = (if (color.matches(Regex("^#[0-9a-fA-F]{6}$"))) "background:$color;color:#202124;" else "") +
            "text-decoration:none"
        val sb = StringBuilder()
        sb.append("<!doctype html><html lang='uz'><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>")
        sb.append(HOME_CSS)
        sb.append("</style></head><body><div class='top'><span class='menu'>☰</span><a class='avatar' href='surfer://profile' style='").append(avatarStyle).append("'>").append(initial).append("</a></div>")
        sb.append("<main class='w'><div class='brand'><img src='/logo.png'><span>Surfer</span></div>")
        sb.append("<form class='search' action='https://www.google.com/search'><button type='submit'>⌕</button><input name='q' placeholder='Surfer orqali qidiring' autocomplete='off'><button type='submit'>➜</button></form>")
        sb.append("<div class='chips'><div class='chip'>✦ Surfer qidiruv</div><div class='chip'>🌐 Internet</div><div class='chip'>★ Sevimlilar</div></div>")
        sb.append("<div class='g'>")
        for ((title, url, color) in tiles) {
            sb.append("<a class='t' href='").append(url).append("'><span class='c' style='background:")
                .append(color).append("'>").append(title.take(1)).append("</span>")
                .append(title).append("</a>")
        }
        sb.append("</div><div class='section'>Surfer yangiliklari</div><div class='news'><h3>Surfer bilan internetni kashf eting</h3><p>Qidiruv, sevimli saytlar va mahalliy profil — hammasi bir joyda. Profil: ")
            .append(esc(name)).append(".</p></div>")
        val bm = (prefs.getStringSet("bm", emptySet()) ?: emptySet()).toList().sorted().take(8)
        if (bm.isNotEmpty()) {
            sb.append("<div class='section'>Xatcho'plar</div>")
            for (item in bm) {
                sb.append("<a class='b' href='").append(esc(item.substringBefore('\t'))).append("'>")
                    .append(esc(item.substringAfter('\t'))).append("</a>")
            }
        }
        sb.append("</main></body></html>")
        return sb.toString()
    }

    private val EMOJIS = arrayOf("Harf (Aa)", "😎", "🦊", "🐯", "🚀", "⚡", "🎮", "🌙", "🔥", "🐼")
    private val COLORS = arrayOf(
        "Ko'k" to "#8ab4f8", "Qizil" to "#f28b82", "Yashil" to "#81c995", "Sariq" to "#fdd663",
        "Binafsha" to "#c58af9", "Pushti" to "#ff8bcb", "Moviy" to "#78d9ec", "To'q sariq" to "#fcad70"
    )
    private val REQ_ACCOUNT = 43

    private fun refreshHomeIfShown() {
        try {
            if (tabs.isNotEmpty() && cw().web.url?.startsWith(HOME_BASE) == true) loadHome(cw().web)
        } catch (_: Exception) { }
    }

    private fun showAccountMenu() {
        val email = prefs.getString("g_email", "") ?: ""
        val items = arrayOf(
            "Google bilan kirish (saytga)",
            "Telefondagi Google akkauntni tanlash",
            "Profilni sozlash (ism, emoji, rang)",
            "Mahalliy akkauntlar",
            "Google'dan chiqish"
        )
        AlertDialog.Builder(this)
            .setTitle(if (email.isNotEmpty()) email else "Akkaunt")
            .setItems(items) { _, i ->
                when (i) {
                    0 -> newTab("https://accounts.google.com/")
                    1 -> pickGoogleAccount()
                    2 -> editProfile()
                    3 -> showLocalAccounts()
                    4 -> {
                        prefs.edit().remove("g_email").apply()
                        newTab("https://accounts.google.com/Logout")
                    }
                }
            }.setNegativeButton("Yopish", null).show()
    }

    // Telefondagi Google akkauntlar ro'yxatidan tanlash (tizim oynasi, ruxsat kerak emas)
    private fun pickGoogleAccount() {
        @Suppress("DEPRECATION")
        val i = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
        try { startActivityForResult(i, REQ_ACCOUNT) } catch (_: Exception) { toast("Akkaunt oynasini ochib bo'lmadi") }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_ACCOUNT && resultCode == RESULT_OK) {
            val email = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME) ?: return
            prefs.edit().putString("g_email", email).apply()
            refreshHomeIfShown()
            toast("Akkaunt: $email")
        }
    }

    private fun editProfile() {
        val field = EditText(this).apply {
            hint = "Ism"
            setSingleLine(true)
            setText(prefs.getString("p_name", ""))
            filters = arrayOf(android.text.InputFilter.LengthFilter(32))
        }
        AlertDialog.Builder(this).setTitle("Profil").setView(field)
            .setNegativeButton("Bekor qilish", null)
            .setPositiveButton("Keyingi") { _, _ ->
                prefs.edit().putString("p_name", field.text.toString().trim()).apply()
                AlertDialog.Builder(this).setTitle("Avatar").setItems(EMOJIS) { _, i ->
                    prefs.edit().putString("p_emoji", if (i == 0) "" else EMOJIS[i]).apply()
                    AlertDialog.Builder(this).setTitle("Rang")
                        .setItems(COLORS.map { it.first }.toTypedArray()) { _, c ->
                            prefs.edit().putString("p_color", COLORS[c].second).apply()
                            refreshHomeIfShown()
                        }.show()
                }.show()
            }.show()
    }

    private fun showLocalAccounts() {
        val names = prefs.getString("local_accounts", "")!!.split("|").filter { it.isNotBlank() }
        val options = (names.map { "👤 $it" } + "＋ Yangi mahalliy profil").toTypedArray()
        AlertDialog.Builder(this).setTitle("Surfer akkauntlari (faqat telefonda)")
            .setItems(options) { _, index ->
                if (index == names.size) {
                    val field = EditText(this).apply {
                        hint = "Profil nomi"
                        setSingleLine(true)
                        filters = arrayOf(android.text.InputFilter.LengthFilter(32))
                    }
                    AlertDialog.Builder(this).setTitle("Yangi profil")
                        .setView(field)
                        .setNegativeButton("Bekor qilish", null)
                        .setPositiveButton("Saqlash") { _, _ ->
                            val value = field.text.toString().trim().replace("|", "")
                            if (value.isNotEmpty()) {
                                val updated = (names + value).distinct()
                                prefs.edit().putString("local_accounts", updated.joinToString("|"))
                                    .putString("local_account", value).apply()
                                loadHome(cw().web)
                            }
                        }.show()
                } else {
                    prefs.edit().putString("local_account", names[index]).apply()
                    loadHome(cw().web)
                }
            }.setNeutralButton("Joriy profilni o'chirish") { _, _ ->
                val current = prefs.getString("local_account", "Mehmon")
                val remaining = names.filter { it != current }
                prefs.edit().putString("local_accounts", remaining.joinToString("|"))
                    .putString("local_account", remaining.firstOrNull() ?: "Mehmon").apply()
                loadHome(cw().web)
            }.setNegativeButton("Yopish", null).show()
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
            setPadding(dp(10), dp(8), dp(4), dp(8))
        }
        address = EditText(this).apply {
            setSingleLine()
            hint = "Google'da qidiring yoki manzil kiriting"
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            background = pill(Color.parseColor("#16203a"))
            setPadding(dp(18), dp(11), dp(18), dp(11))
            imeOptions = EditorInfo.IME_ACTION_GO
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSelectAllOnFocus(true)
            setOnEditorActionListener { _, _, _ -> go(text.toString()); true }
        }
        tabBtn = Button(this).apply {
            text = "1"
            textSize = 13f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), Color.WHITE)
                cornerRadius = dp(8).toFloat()
            }
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener { showTabs() }
        }
        top.addView(address, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        top.addView(tabBtn, LinearLayout.LayoutParams(dp(32), dp(32)).apply { setMargins(dp(10), 0, dp(2), 0) })
        top.addView(ibtn(R.drawable.ic_more) { showMenu() })

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
            background = pill(Color.parseColor("#16203a"), 18)
            setPadding(dp(14), dp(8), dp(14), dp(8))
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
            progressTintList = ColorStateList.valueOf(Color.parseColor("#4DA3FF"))
        }

        content = FrameLayout(this)

        // Pastki panel
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0f1830"))
        }
        fun add(res: Int, f: () -> Unit) =
            bottom.addView(ibtn(res, f), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        add(R.drawable.ic_back) { if (cw().web.canGoBack()) cw().web.goBack() }
        add(R.drawable.ic_forward) { if (cw().web.canGoForward()) cw().web.goForward() }
        add(R.drawable.ic_home) { loadHome(cw().web) }
        add(R.drawable.ic_refresh) { cw().web.reload() }
        add(R.drawable.ic_add) { newTab() }

        root.addView(top)
        root.addView(findBar)
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottom)
        setContentView(root)

        newTab(intent?.data?.toString())
    }

    private fun loadHome(w: WebView) = w.loadDataWithBaseURL(HOME_BASE, homeHtml(), "text/html", "utf-8", null)

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
            userAgentString = mobileUa()
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
        if (!incognito) CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
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
                if (u.scheme == "surfer") {
                    if (u.host == "profile") showAccountMenu()
                    return true
                }
                when (u.scheme) {
                    "http", "https", "about", "data", "blob", "file" -> return false
                }
                try { startActivity(Intent(Intent.ACTION_VIEW, u)) } catch (_: Exception) { }
                return true
            }

            override fun shouldInterceptRequest(v: WebView?, r: WebResourceRequest?): WebResourceResponse? {
                if (r == null) return null
                if (r.url.host == "surfer.home" && r.url.path == "/logo.png") {
                    return try {
                        WebResourceResponse("image/png", null, assets.open("logo.png"))
                    } catch (_: Exception) {
                        null
                    }
                }
                if (!adblock || r.isForMainFrame) return null
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
        address.background = pill(Color.parseColor(if (tabs[i].incognito) "#3a1e4a" else "#16203a"))
        address.setPadding(dp(18), dp(11), dp(18), dp(11))
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
            "Akkaunt va profil",
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
                1 -> { showAccountMenu() }
                2 -> { newTab(null, true) }
                3 -> { showFind() }
                4 -> { addBookmark() }
                5 -> { showBookmarks() }
                6 -> {
                    dark = !dark
                    prefs.edit().putBoolean("dark", dark).apply()
                    cw().web.reload()
                }
                7 -> { toggleDesktop() }
                8 -> {
                    adblock = !adblock
                    prefs.edit().putBoolean("adblock", adblock).apply()
                    cw().web.reload()
                }
                9 -> { showScripts() }
                10 -> { closeTab() }
                11 -> { clearData() }
            }
        }.show()
    }

    // Google kirishi oddiy WebView'ni ("; wv") rad etadi, shuning uchun brauzerga o'xshash UA
    private fun mobileUa(): String =
        WebSettings.getDefaultUserAgent(this).replace("; wv", "").replace(" Version/4.0", "")

    private fun toggleDesktop() {
        desktop = !desktop
        val s = cw().web.settings
        s.userAgentString = if (desktop) DESKTOP_UA else mobileUa()
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { newTab(it.toString()) }   // boshqa ilovadan kelgan havola yangi tabda
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        super.onPause()
    }
}
