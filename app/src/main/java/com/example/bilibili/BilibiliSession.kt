package com.example.bilibili

import android.content.Context
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.TimeUnit

class BilibiliSession(private val context: Context) {
    private val tag = "BilibiliSession"
    private val prefName = "bilibili_prefs"
    private val cookiesPrefKey = "cookies_string"
    private val sharedPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    // Current cookies in memory
    private val cookieStore = mutableMapOf<String, Cookie>()

    // Device metrics
    val deviceFp: String by lazy { UUID.randomUUID().toString().replace("-", "") }
    private val buvid: String by lazy { generateBuvid() }
    private val fp: String by lazy { generateFp() }

    init {
        loadCookiesFromStorage()
    }

    private fun generateBuvid(): String {
        val rand1 = UUID.randomUUID().toString().replace("-", "").lowercase()
        val buvidRaw = "XU${rand1[2]}${rand1[12]}${rand1[22]}$rand1".uppercase()
        return buvidRaw
    }

    private fun generateFp(): String {
        val rand2 = UUID.randomUUID().toString().replace("-", "").lowercase()
        val ts = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val raw = rand2 + ts + (1000000000..9999999999).random().toString()
        return raw.take(40)
    }

    // Custom CookieJar to persist cookies
    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(cookieStore) {
                for (cookie in cookies) {
                    cookieStore[cookie.name] = cookie
                }
                saveCookiesToStorage()
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            synchronized(cookieStore) {
                // Ensure buvid cookies are present
                ensureBuvidCookies(url.host)
                return cookieStore.values.filter { it.matches(url) }
            }
        }
    }

    private fun ensureBuvidCookies(host: String) {
        val domain = if (host.contains("bilibili.com")) "bilibili.com" else host
        val path = "/"
        if (!cookieStore.containsKey("buvid3")) {
            cookieStore["buvid3"] = Cookie.Builder()
                .name("buvid3")
                .value("XU" + UUID.randomUUID().toString().replace("-", "").take(32).uppercase())
                .domain(domain)
                .path(path)
                .build()
        }
        if (!cookieStore.containsKey("buvid_fp")) {
            cookieStore["buvid_fp"] = Cookie.Builder()
                .name("buvid_fp")
                .value(fp)
                .domain(domain)
                .path(path)
                .build()
        }
        if (!cookieStore.containsKey("deviceFingerprint")) {
            cookieStore["deviceFingerprint"] = Cookie.Builder()
                .name("deviceFingerprint")
                .value(deviceFp)
                .domain(domain)
                .path(path)
                .build()
        }
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // Load persisted cookies from SharedPreferences
    private fun loadCookiesFromStorage() {
        val cookiesStr = sharedPrefs.getString(cookiesPrefKey, null)
        if (!cookiesStr.isNullOrEmpty()) {
            try {
                val json = JSONObject(cookiesStr)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val name = keys.next()
                    val cookieJson = json.getJSONObject(name)
                    val value = cookieJson.getString("value")
                    val domain = cookieJson.getString("domain")
                    val path = cookieJson.getString("path")
                    val secure = cookieJson.getBoolean("secure")
                    val httpOnly = cookieJson.getBoolean("httpOnly")
                    val expiresAt = cookieJson.getLong("expiresAt")

                    val builder = Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(domain)
                        .path(path)
                        .expiresAt(expiresAt)
                    if (secure) builder.secure()
                    if (httpOnly) builder.httpOnly()

                    cookieStore[name] = builder.build()
                }
                Log.d(tag, "Loaded cookies: ${cookieStore.keys}")
            } catch (e: Exception) {
                Log.e(tag, "Error loading cookies", e)
            }
        }
    }

    // Save cookies to legacy string storage
    fun saveCookiesToStorage() {
        try {
            val json = JSONObject()
            for ((name, cookie) in cookieStore) {
                val cookieJson = JSONObject()
                cookieJson.put("value", cookie.value)
                cookieJson.put("domain", cookie.domain)
                cookieJson.put("path", cookie.path)
                cookieJson.put("secure", cookie.secure)
                cookieJson.put("httpOnly", cookie.httpOnly)
                cookieJson.put("expiresAt", cookie.expiresAt)
                json.put(name, cookieJson)
            }
            sharedPrefs.edit().putString(cookiesPrefKey, json.toString()).apply()
        } catch (e: Exception) {
            Log.e(tag, "Error saving cookies", e)
        }
    }

    // Reset login session
    fun clearSession() {
        synchronized(cookieStore) {
            cookieStore.clear()
            sharedPrefs.edit().remove(cookiesPrefKey).apply()
        }
    }

    fun getCsrf(): String {
        return cookieStore["bili_jct"]?.value ?: ""
    }

    // Check login state via Bilibili nav API
    fun checkLogin(): JSONObject? {
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/nav")
            .header("User-Agent", getBrowserUserAgent())
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    if (json.optInt("code", -1) == 0) {
                        return json.optJSONObject("data")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking Bilibili login state", e)
        }
        return null
    }

    // Generate Bilibili Web Qr Code for scan login
    fun generateQrCode(): Pair<String, String> {
        val request = Request.Builder()
            .url("https://passport.bilibili.com/x/passport-login/web/qrcode/generate")
            .header("User-Agent", getBrowserUserAgent())
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    if (json.optInt("code", -1) == 0) {
                        val data = json.getJSONObject("data")
                        return Pair(data.getString("url"), data.getString("qrcode_key"))
                    } else {
                        throw Exception("API Error: ${json.optString("message", "Unknown")}")
                    }
                } else {
                    throw Exception("HTTP Request Failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Network Error: ${e.message}", e)
        }
    }

    // Poll QR scan result. Returns string code: "success", "wait_scan", "wait_confirm", "expired", "failed"
    fun pollQrCode(key: String): String {
        val url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?source=main-fe-header&qrcode_key=$key"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", getBrowserUserAgent())
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    if (json.optInt("code", -1) == 0) {
                        val qrCodeData = json.getJSONObject("data")
                        val code = qrCodeData.optInt("code", -1)
                        return when (code) {
                            0 -> {
                                // Double check session login
                                checkLogin()
                                "success"
                            }
                            86101 -> "wait_scan"
                            86090 -> "wait_confirm"
                            86038 -> "expired"
                            else -> "failed"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error polling qr code state", e)
        }
        return "failed"
    }

    // Fetch Project specifications from Bilibili Member Purchase
    fun getProjectInfo(projectId: String): JSONObject? {
        val url = "https://show.bilibili.com/api/ticket/project/getV2?version=134&id=$projectId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", getBrowserUserAgent())
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    if (json.optInt("code", -1) == 0) {
                        return json.optJSONObject("data")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching project specifications", e)
        }
        return null
    }

    // Fetch User Added Buyers Profiles list
    fun getBuyerList(projectId: String): JSONArray? {
        val url = "https://show.bilibili.com/api/ticket/buyer/list?is_default&projectId=$projectId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", getBrowserUserAgent())
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    Log.d(tag, "getBuyerList response: $bodyStr")
                    val json = JSONObject(bodyStr)
                    if (json.optInt("errno", -1) == 0 || json.optInt("code", -1) == 0) {
                        val data = json.getJSONObject("data")
                        return data.optJSONArray("list")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching buyers from Bilibili", e)
        }
        return null
    }

    // Fetch dynamic security tickets prepare tokens
    fun prepareToken(
        projectId: String,
        screenId: String,
        skuId: String,
        count: Int
    ): Pair<String, String> {
        val url = "https://show.bilibili.com/api/ticket/order/prepare?project_id=$projectId"
        
        val jsonPayload = JSONObject()
        jsonPayload.put("project_id", projectId.toLong())
        jsonPayload.put("screen_id", screenId.toLong())
        jsonPayload.put("order_type", 1)
        jsonPayload.put("count", count)
        jsonPayload.put("sku_id", skuId.toLong())
        jsonPayload.put("token", TokenGenerator.generateCtoken())
        jsonPayload.put("newRisk", true)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", getBrowserUserAgent())
            .header("Origin", "https://show.bilibili.com")
            .header("Referer", "https://show.bilibili.com/")
            .post(body)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string() ?: ""
            Log.d(tag, "Prepare Resp: $bodyStr")
            val json = JSONObject(bodyStr)
            val code = if (json.has("code")) json.getInt("code") else json.optInt("errno", -1)
            if (code == 0) {
                val data = json.getJSONObject("data")
                val token = data.optString("token", "")
                val ptoken = data.optString("ptoken", "").replace("=", "")
                return Pair(token, ptoken)
            } else {
                val msg = json.optString("message", json.optString("msg", "Unknown"))
                throw Exception("Prepare Token 失败: [$code] $msg")
            }
        }
    }

    // Create ticket booking order
    fun createOrder(
        projectId: String,
        screenId: String,
        skuId: String,
        count: Int,
        payMoney: Int,
        idBind: Int,
        buyerJson: String,
        token: String,
        ptoken: String,
        isHotProject: Boolean
    ): JSONObject {
        val orderUrl = "https://show.bilibili.com/api/ticket/order/createV2?project_id=$projectId" +
                if (isHotProject && ptoken.isNotEmpty()) "&ptoken=$ptoken" else ""

        val jsonPayload = JSONObject()
        jsonPayload.put("project_id", projectId.toLong())
        jsonPayload.put("screen_id", screenId.toLong())
        jsonPayload.put("sku_id", skuId.toLong())
        jsonPayload.put("count", count)
        jsonPayload.put("pay_money", payMoney)
        jsonPayload.put("order_type", 1)
        jsonPayload.put("timestamp", System.currentTimeMillis())
        jsonPayload.put("id_bind", idBind)
        jsonPayload.put("need_contact", if (idBind == 0) 1 else 0)
        jsonPayload.put("is_package", 0)
        jsonPayload.put("package_num", 1)
        
        if (idBind != 0) {
            jsonPayload.put("buyer_info", buyerJson)
            // Retrieve own UID if possible
            val myUid = cookieStore["DedeUserID"]?.value ?: ""
            val myName = cookieStore["DedeUserID__ckMd5"]?.value ?: ""
            val contact = JSONObject()
            contact.put("uid", myUid)
            contact.put("username", myName)
            contact.put("tel", "")
            jsonPayload.put("contactInfo", contact)
        } else {
            // Need contact info
            val buyerArr = JSONArray(buyerJson)
            if (buyerArr.length() > 0) {
                val b = buyerArr.getJSONObject(0)
                val contact = JSONObject()
                contact.put("uid", b.optString("uid", ""))
                contact.put("username", b.optString("name", ""))
                contact.put("tel", b.optString("tel", ""))
                jsonPayload.put("contactInfo", contact)
                jsonPayload.put("buyer", b.optString("name", ""))
                jsonPayload.put("tel", b.optString("tel", ""))
            }
        }

        jsonPayload.put("token", token)
        jsonPayload.put("deviceId", deviceFp)
        jsonPayload.put("version", "1.1.0")
        jsonPayload.put("newRisk", true)
        jsonPayload.put("requestSource", "neul-next")

        val clickPos = JSONObject()
        clickPos.put("x", (200..400).random())
        clickPos.put("y", (200..400).random())
        jsonPayload.put("clickPosition", clickPos)

        if (isHotProject) {
            jsonPayload.put("ctoken", TokenGenerator.generateCtoken())
            jsonPayload.put("ptoken", ptoken)
            jsonPayload.put("orderCreateUrl", "https://show.bilibili.com/api/ticket/order/createV2")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonPayload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(orderUrl)
            .header("User-Agent", getBrowserUserAgent())
            .header("Origin", "https://show.bilibili.com")
            .header("Referer", "https://show.bilibili.com/")
            .post(body)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(tag, "Create order resp: $bodyStr")
                return JSONObject(bodyStr)
            }
        } catch (e: Exception) {
            val err = JSONObject()
            err.put("code", -114)
            err.put("message", "网络请求出错: ${e.message}")
            return err
        }
    }

    // Query order result to fetch payment link
    fun getOrderStatus(orderId: String, projectToken: String, projectId: String): JSONObject? {
        val url = "https://show.bilibili.com/api/ticket/order/createstatus?orderId=$orderId&project_id=$projectId&token=$projectToken"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 BHYG/66666")
            .get()
            .build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    return JSONObject(bodyStr)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching order status", e)
        }
        return null
    }

    private fun getBrowserUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
