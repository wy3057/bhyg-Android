package com.example.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.bilibili.QrCodeGenerator
import org.json.JSONObject

// Vibrant luxury Bilibili Pink-themed color palette
val BiliPink = Color(0xFFFB7299)
val BiliDark = Color(0xFF1E1E2C)
val BiliDarkGray = Color(0xFF2D2D3F)
val BiliLightCharcoal = Color(0xFF3E3E56)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrabberScreen(viewModel: GrabberViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("账号登录", "项目配置", "控制台")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ConfirmationNumber,
                            contentDescription = "BHYG Logo",
                            tint = BiliPink,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "BHYG 会员购抢票助手",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    if (viewModel.isLoggedIn) {
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = "退出登录",
                                tint = Color.LightGray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BiliDark,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BiliDark,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val icon = when (index) {
                        0 -> if (isSelected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle
                        1 -> if (isSelected) Icons.Filled.Tune else Icons.Outlined.Tune
                        else -> if (isSelected) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        label = { Text(title, color = if (isSelected) BiliPink else Color.Gray) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = if (isSelected) BiliPink else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = BiliLightCharcoal
                        )
                    )
                }
            }
        },
        containerColor = BiliDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BiliDark)
        ) {
            when (selectedTab) {
                0 -> LoginTab(viewModel)
                1 -> ConfigTab(viewModel)
                2 -> ConsoleTab(viewModel)
            }

            // Payment success Pop-up
            if (viewModel.showPayDialog) {
                PaymentDialog(viewModel)
            }
        }
    }
}

@Composable
fun LoginTab(viewModel: GrabberViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (viewModel.isLoggedIn) {
            // Logged in UI
            Card(
                colors = CardDefaults.cardColors(containerColor = BiliDarkGray),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BiliPink.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (viewModel.userAvatar.isNotEmpty()) {
                        AsyncImage(
                            model = viewModel.userAvatar,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(3.dp, BiliPink, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(BiliLightCharcoal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Default Face",
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = viewModel.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MID: ${viewModel.userMid}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = Color(0xFF10C487).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10C487))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "安全连接已就绪",
                                color = Color(0xFF10C487),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.checkSessionState() },
                        colors = ButtonDefaults.buttonColors(containerColor = BiliLightCharcoal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "刷新")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("刷新登录状态", color = Color.White)
                    }
                }
            }
        } else {
            // Login view carrying QR Poll states
            Text(
                "请扫描二维码登录 Bilibili",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "为了抢得会员购门票，程序需要获取登录凭证。请在手机中打开哔哩哔哩APP进行扫码验证",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            val qrBitmap = remember(viewModel.qrCodeUrl) {
                viewModel.qrCodeUrl?.let { QrCodeGenerator.generateQrCodeBitmap(it, 400) }
            }

            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, BiliPink, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Bilibili Core Login QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = "Qr QR Dummy",
                            tint = Color.LightGray,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "点击下方获取二维码",
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "系统状态: ${viewModel.qrLoginStatus}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (viewModel.qrLoginStatus.contains("过期") || viewModel.qrLoginStatus.contains("失败")) Color.Red else BiliPink
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.refreshQrCode() },
                colors = ButtonDefaults.buttonColors(containerColor = BiliPink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Filled.Sync, contentDescription = "Refresh qr")
                Spacer(modifier = Modifier.width(8.dp))
                Text("刷新/获取 登录二维码", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigTab(viewModel: GrabberViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Project Loader card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BiliDarkGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "1. 导入会员购项目",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.projectIdInput,
                        onValueChange = { viewModel.projectIdInput = it },
                        label = { Text("项目 Mid 或 编号 (ProjectId)", color = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BiliPink,
                            unfocusedBorderColor = BiliLightCharcoal,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchProjectDetails() },
                        colors = ButtonDefaults.buttonColors(containerColor = BiliPink),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !viewModel.isLoadingProject,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (viewModel.isLoadingProject) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("解析会员购项目参数", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Selected specifications detail card
        if (viewModel.projectName.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BiliDarkGray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "当前载入: ${viewModel.projectName}",
                            fontWeight = FontWeight.Bold,
                            color = BiliPink,
                            fontSize = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Screens selection Chip
                        Text("2. 选择场次", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.screens.forEach { screen ->
                                val isSelected = viewModel.selectedScreen?.optString("id") == screen.optString("id")
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onScreenSelected(screen) },
                                    label = { Text(screen.optString("name", "")) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BiliPink,
                                        selectedLabelColor = Color.White,
                                        containerColor = BiliLightCharcoal,
                                        labelColor = Color.LightGray
                                    )
                                )
                            }
                        }

                        // Sku selection details
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("3. 选择票档 (价格)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.skus.forEach { sku ->
                                val isSelected = viewModel.selectedSku?.optString("id") == sku.optString("id")
                                val priceInRmb = sku.optInt("price", 0) / 100
                                val showText = "${sku.optString("desc", "")} (${priceInRmb}元)"
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectedSku = sku },
                                    label = { Text(showText) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BiliPink,
                                        selectedLabelColor = Color.White,
                                        containerColor = BiliLightCharcoal,
                                        labelColor = Color.LightGray
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Real registered account buyers selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BiliDarkGray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "4. 选择常用购票人 (根据票数勾选)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "当前订单绑定限制: ${if (viewModel.idBindType == 0) "一号一联系人" else "实名终身绑定"}（已勾选: ${viewModel.selectedBuyers.size} 人）",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (viewModel.userBuyers.isEmpty()) {
                            Text("无常用购票人候选，请先绑定您的会员购实名身份", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            viewModel.userBuyers.forEach { buyer ->
                                val isSelected = viewModel.selectedBuyers.contains(buyer)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleBuyerSelection(buyer) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleBuyerSelection(buyer) },
                                        colors = CheckboxDefaults.colors(checkedColor = BiliPink)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(buyer.optString("name", ""), color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("手机号: ${buyer.optString("tel", "未知")}   身份证: ${buyer.optString("personal_id", "保密")}", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = BiliLightCharcoal)
                            }
                        }
                    }
                }
            }

            // Run speed parameters controller
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BiliDarkGray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "5. 高级策略参数配置",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Count controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("购买张数:", color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { if (viewModel.ticketCount > 1) viewModel.ticketCount-- },
                                    colors = ButtonDefaults.buttonColors(containerColor = BiliLightCharcoal),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    "${viewModel.ticketCount}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Button(
                                    onClick = { if (viewModel.ticketCount < 4) viewModel.ticketCount++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = BiliLightCharcoal),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hot Project bypass
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("极热抢票模式 (自动防限流)", color = Color.White)
                                Text("极客必备，推荐开启 wts 混淆验证", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = viewModel.isHotProject,
                                onCheckedChange = { viewModel.isHotProject = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = BiliPink, checkedTrackColor = BiliPink.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Frequency slider
                        Text("重试并发间隔: ${viewModel.intervalMs}ms", color = Color.White)
                        Slider(
                            value = viewModel.intervalMs.toFloat(),
                            onValueChange = { viewModel.intervalMs = it.toInt() },
                            valueRange = 100f..2000f,
                            colors = SliderDefaults.colors(thumbColor = BiliPink, activeTrackColor = BiliPink)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsoleTab(viewModel: GrabberViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "控制台运行日志",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            Button(
                onClick = { viewModel.clearLogs() },
                colors = ButtonDefaults.buttonColors(containerColor = BiliLightCharcoal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("清空", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Large console window
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, BiliLightCharcoal, RoundedCornerShape(12.dp))
        ) {
            if (viewModel.logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "控制台就绪。请确认配置，并轻按“开始自动抢票”",
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = false
                ) {
                    items(viewModel.logs) { log ->
                        val color = when {
                            log.contains("🎉") || log.contains("✅") -> Color(0xFF10C487)
                            log.contains("❌") || log.contains("错误") -> Color(0xFFFF5B5B)
                            log.contains("第") -> Color(0xFFF1C40F)
                            else -> Color.LightGray
                        }
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = color,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (viewModel.isRunning) {
                    viewModel.stopGrabbing()
                } else {
                    viewModel.startGrabbing()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.isRunning) Color(0xFFFF5B5B) else BiliPink
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = if (viewModel.isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = "Trigger button"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (viewModel.isRunning) "停止自动下单" else "启动 BHYG 极速抢票",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun PaymentDialog(viewModel: GrabberViewModel) {
    val b64PayCode = viewModel.payCodeUrl
    val payBitmap = remember(b64PayCode) {
        b64PayCode?.let { QrCodeGenerator.generateQrCodeBitmap(it, 400) }
    }

    Dialog(onDismissRequest = { viewModel.showPayDialog = false }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BiliDarkGray),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, BiliPink, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Success tick",
                    tint = Color(0xFF10C487),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "安全下单锁定成功",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    "订单锁定ID: ${viewModel.payOrderId}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (payBitmap != null) {
                        Image(
                            bitmap = payBitmap.asImageBitmap(),
                            contentDescription = "支付二维码",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "请尽速扫描上方二维码完成支付；或在 10 分钟内打开手机哔哩哔哩APP客户端 -> \n\"会员购-我的订单\" 进行合并付款，到期订单将会自动取消",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.showPayDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BiliPink),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("我记下了，返回", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
