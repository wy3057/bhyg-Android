package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bilibili.BilibiliSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GrabberViewModel(application: Application) : AndroidViewModel(application) {
    val session = BilibiliSession(application)
    private val tag = "GrabberViewModel"

    // Authentication States
    var isLoggedIn by mutableStateOf(false)
        private set
    var username by mutableStateOf("")
        private set
    var userMid by mutableStateOf("")
        private set
    var userAvatar by mutableStateOf("")
        private set

    // QR Login state
    var qrCodeUrl by mutableStateOf<String?>(null)
        private set
    var qrCodeKey by mutableStateOf("")
        private set
    var qrLoginStatus by mutableStateOf("未生成") // "未生成", "请扫码", "扫码成功，请在手机确认", "已过期", "登录成功", "出错"
    private var qrPollJob: Job? = null

    // Project Details Fetch States
    var projectIdInput by mutableStateOf("1001701") // Standard default BML id
    var isLoadingProject by mutableStateOf(false)
    var projectName by mutableStateOf("")
    var idBindType by mutableStateOf(0) // 0: contact link only, 1: single id single profile, 2: single price profile

    // parsed screen spec details lists
    val screens = mutableStateListOf<JSONObject>()
    var selectedScreen by mutableStateOf<JSONObject?>(null)

    val skus = mutableStateListOf<JSONObject>()
    var selectedSku by mutableStateOf<JSONObject?>(null)

    var ticketCount by mutableStateOf(1)
    var isHotProject by mutableStateOf(true)
    var intervalMs by mutableStateOf(300) // 300ms default

    // User Buyers Profiles multi selection
    val userBuyers = mutableStateListOf<JSONObject>()
    val selectedBuyers = mutableStateListOf<JSONObject>()

    // Mission Control running states
    var isRunning by mutableStateOf(false)
    val logs = mutableStateListOf<String>()
    private var missionJob: Job? = null

    // Payment qr-code popup outcome status
    var payCodeUrl by mutableStateOf<String?>(null)
        private set
    var payOrderId by mutableStateOf("")
        private set
    var showPayDialog by mutableStateOf(false)

    init {
        checkSessionState()
    }

    fun checkSessionState() {
        viewModelScope.launch {
            val userProfile = withContext(Dispatchers.IO) { session.checkLogin() }
            if (userProfile != null) {
                isLoggedIn = true
                username = userProfile.optString("uname", "Bilibili用户")
                userMid = userProfile.optString("mid", "")
                userAvatar = userProfile.optString("face", "")
                addLog("检测到已登录账号: $username")
            } else {
                isLoggedIn = false
                username = ""
                userMid = ""
                userAvatar = ""
                addLog("未登录或登录状态已失效，请扫描二维码登录")
            }
        }
    }

    // Refresh of scan verification QR values
    fun refreshQrCode() {
        qrPollJob?.cancel()
        qrLoginStatus = "正在生成..."
        viewModelScope.launch {
            try {
                val qr = withContext(Dispatchers.IO) { session.generateQrCode() }
                qrCodeUrl = qr.first
                qrCodeKey = qr.second
                qrLoginStatus = "请使用哔哩哔哩APP扫码"
                startQrPolling()
            } catch (e: Exception) {
                qrLoginStatus = "生成失败: ${e.message}"
            }
        }
    }

    private fun startQrPolling() {
        qrPollJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                val status = withContext(Dispatchers.IO) { session.pollQrCode(qrCodeKey) }
                when (status) {
                    "success" -> {
                        qrLoginStatus = "登陆成功"
                        checkSessionState()
                        qrPollJob?.cancel()
                        break
                    }
                    "wait_confirm" -> {
                        qrLoginStatus = "已扫码，请在哔哩哔哩客户端确认登录"
                    }
                    "expired" -> {
                        qrLoginStatus = "二维码已过期，请点击刷新"
                        qrPollJob?.cancel()
                        break
                    }
                    "failed" -> {
                        // Keep polling or continue
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { session.clearSession() }
            checkSessionState()
            userBuyers.clear()
            selectedBuyers.clear()
            addLog("当前账号已退出登录")
        }
    }

    // Fetch and sync specs
    fun fetchProjectDetails() {
        if (projectIdInput.isEmpty()) return
        isLoadingProject = true
        projectName = "加载中..."
        screens.clear()
        skus.clear()
        selectedScreen = null
        selectedSku = null

        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) { session.getProjectInfo(projectIdInput) }
            isLoadingProject = false
            if (data != null) {
                projectName = data.optString("name", "未知名会员购项目")
                idBindType = data.optInt("id_bind", 0)
                
                val screenList = data.optJSONArray("screen_list")
                if (screenList != null) {
                    for (i in 0 until screenList.length()) {
                        screens.add(screenList.getJSONObject(i))
                    }
                    if (screens.isNotEmpty()) {
                        onScreenSelected(screens.first())
                    }
                }
                addLog("成功载入项目: $projectName")
                fetchBuyerList(projectIdInput)
            } else {
                projectName = ""
                addLog("载入项目信息失败，请检查ProjectId是否正确")
            }
        }
    }

    fun onScreenSelected(screen: JSONObject) {
        selectedScreen = screen
        skus.clear()
        selectedSku = null
        val ticketList = screen.optJSONArray("ticket_list")
        if (ticketList != null) {
            for (i in 0 until ticketList.length()) {
                skus.add(ticketList.getJSONObject(i))
            }
            if (skus.isNotEmpty()) {
                selectedSku = skus.first()
            }
        }
    }

    private fun fetchBuyerList(projectId: String) {
        if (projectId.isEmpty()) return
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { session.getBuyerList(projectId) }
            userBuyers.clear()
            selectedBuyers.clear()
            if (list != null) {
                for (i in 0 until list.length()) {
                    userBuyers.add(list.getJSONObject(i))
                }
                Log.d(tag, "Fetched ${userBuyers.size} target buyers")
            }
        }
    }

    fun toggleBuyerSelection(buyer: JSONObject) {
        if (selectedBuyers.contains(buyer)) {
            selectedBuyers.remove(buyer)
        } else {
            selectedBuyers.add(buyer)
        }
    }

    fun addLog(msg: String) {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = sdf.format(Date())
        logs.add(0, "[$timestamp] $msg")
    }

    fun clearLogs() {
        logs.clear()
    }

    // Launch core repeat mission ticket checkout coroutine
    fun startGrabbing() {
        if (!isLoggedIn) {
            addLog("错误 — 请先登陆您的哔哩哔哩账号！")
            return
        }
        if (selectedScreen == null || selectedSku == null) {
            addLog("错误 — 请先加载并选择规格与档次！")
            return
        }
        val count = ticketCount
        val idBind = idBindType
        if (idBind != 0 && selectedBuyers.size != count) {
            addLog("错误 — 当前档次需要实名认证。由于您选择了 $count 张票，请必须选择 exact $count 个常用购票人账户！")
            return
        }

        isRunning = true
        addLog("▶ 开始抢票，档位: ${selectedSku?.optString("desc")}, 数量: $count, 速度: ${intervalMs}ms")
        
        // Prepare buyer JSON payload
        val buyersArr = JSONArray()
        for (b in selectedBuyers) {
            val jsonBuyer = JSONObject()
            jsonBuyer.put("id", b.optLong("id"))
            jsonBuyer.put("name", b.optString("name"))
            jsonBuyer.put("tel", b.optString("tel"))
            jsonBuyer.put("personal_id", b.optString("personal_id"))
            jsonBuyer.put("id_type", b.optInt("id_type"))
            buyersArr.put(jsonBuyer)
        }
        val buyerJsonString = buyersArr.toString()

        val projectId = projectIdInput
        val screenId = selectedScreen?.optString("id") ?: ""
        val skuId = selectedSku?.optString("id") ?: ""
        val priceCents = selectedSku?.optInt("price") ?: 0
        val totalPayCents = priceCents * count
        val hotProj = isHotProject

        missionJob = viewModelScope.launch(Dispatchers.IO) {
            var attempt = 1
            while (isRunning) {
                withContext(Dispatchers.Main) {
                    addLog("第 $attempt 次尝试，请求获取准备 token...")
                }
                
                // 1. Prepare Step
                try {
                    val tokens = session.prepareToken(
                        projectId = projectId,
                        screenId = screenId,
                        skuId = skuId,
                        count = count
                    )

                    if (tokens.first.isNotEmpty()) {
                        val (token, ptoken) = tokens
                        withContext(Dispatchers.Main) {
                            addLog("获取 Token 成功! 开始发起 createOrder 下单...")
                        }

                        // 2. Order V2 Checkout Step
                        val orderResult = session.createOrder(
                            projectId = projectId,
                            screenId = screenId,
                            skuId = skuId,
                            count = count,
                            payMoney = totalPayCents,
                            idBind = idBind,
                            buyerJson = buyerJsonString,
                            token = token,
                            ptoken = ptoken,
                            isHotProject = hotProj
                        )

                        val code = if (orderResult.has("code")) orderResult.getInt("code") else orderResult.optInt("errno", -1)
                        val message = orderResult.optString("message", orderResult.optString("msg", "Unknown error"))
                        
                        if (code == 0) {
                            val orderData = orderResult.optJSONObject("data")
                            val orderId = orderData?.optString("orderId") ?: ""
                            val orderToken = orderData?.optString("token") ?: ""
                            
                            withContext(Dispatchers.Main) {
                                addLog("🎉 抢票创建成功！订单ID: $orderId")
                                addLog("正在查询订单支付二维码...")
                            }

                            // 3. Confirm Status pay param
                            var codeUrl: String? = null
                            for (statusCheck in 1..5) {
                                delay(1000)
                                val statusResult = session.getOrderStatus(orderId, orderToken, projectId)
                                if (statusResult != null && statusResult.optInt("code", -1) == 0) {
                                    val statusData = statusResult.optJSONObject("data")
                                    val payParam = statusData?.optJSONObject("payParam")
                                    codeUrl = payParam?.optString("code_url")
                                    if (!codeUrl.isNullOrEmpty()) {
                                        break
                                    }
                                }
                            }

                            if (!codeUrl.isNullOrEmpty()) {
                                withContext(Dispatchers.Main) {
                                    isRunning = false
                                    payCodeUrl = codeUrl
                                    payOrderId = orderId
                                    showPayDialog = true
                                    addLog("✅ 票务已被锁定！请即时扫描二维码在手机上支付")
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    isRunning = false
                                    addLog("⚠️ 下单成功，但获取支付二维码超时，请打开哔哩哔哩APP至\"我的订单\"尽快完成支付！")
                                }
                            }
                            break
                        } else {
                            withContext(Dispatchers.Main) {
                                addLog("❌ 下单失败: [$code] $message")
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            addLog("❌ 准备 Token 获取失败: token 为空")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        addLog("❌ ${e.message}")
                    }
                }

                attempt++
                delay(intervalMs.toLong())
            }
        }
    }

    fun stopGrabbing() {
        isRunning = false
        missionJob?.cancel()
        addLog("⏸ 自动抢票任务已终止")
    }

    override fun onCleared() {
        super.onCleared()
        qrPollJob?.cancel()
        missionJob?.cancel()
    }
}
