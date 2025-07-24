package com.kayamasoft.ksmeasuring

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.telephony.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kayamasoft.ksmeasuring.ui.theme.KSMeasuringTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.ClickableText
import android.os.PowerManager
import androidx.compose.runtime.saveable.rememberSaveable

class MainActivity : ComponentActivity() {

    private var permissionRequested = false
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = applicationContext.packageName
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions += Manifest.permission.POST_NOTIFICATIONS
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
            permissionRequested = true
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setContent {
            KSMeasuringTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    //権限がリクエスト中の場合、UIを表示しないようにする
                    if (!permissionRequested || hasAllPermissions(this)) {
                        AppScaffold(telephonyManager, connectivityManager)
                    }
                }
            }
        }
    }

    private fun hasAllPermissions(context: Context): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions += Manifest.permission.POST_NOTIFICATIONS
        }

        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "一部の権限が拒否されました。アプリが正しく動作しない可能性があります。", Toast.LENGTH_LONG).show()
            } else {
                recreate()
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(telephonyManager: TelephonyManager, connectivityManager: ConnectivityManager) {
    var showInfo by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "KS Measuring",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                NavigationDrawerItem(
                    label = { Text("通信情報") },
                    selected = !showInfo,
                    onClick = {
                        showInfo = false
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("アプリ情報") },
                    selected = showInfo,
                    onClick = {
                        showInfo = true
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("KS Measuring") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "KS Measuring")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (showInfo) {
                    AppInfoSection()
                } else {
                    SignalInfoScreen(telephonyManager, connectivityManager)
                }
            }
        }
    }
}

@Composable
fun AppInfoSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text("アプリ情報", style = MaterialTheme.typography.titleMedium)
        Text("KS Measuring")
        Text("Version 1.0.2")
        Text("KayamaSoft")
        Text("Licensed under the Apache License 2.0")
        ClickableLink(label = "KayamaSoft Webページ", url = "https://www.kayamasoft.org")
        ClickableLink(label = "プライバシーポリシー", url = "https://www.kayamasoft.org/privacy.html")
        ClickableLink(label = "お問い合わせ", url = "mailto:hello@kayamasoft.org")
    }
}

@Composable
fun ClickableLink(label: String, url: String) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(style = SpanStyle(color = Blue, textDecoration = TextDecoration.Underline)) {
            append(label)
        }
        pop()
    }
    ClickableText(
        text = annotatedString,
        onClick = {
            annotatedString.getStringAnnotations(tag = "URL", start = it, end = it)
                .firstOrNull()?.let { stringAnnotation ->
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(stringAnnotation.item))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

fun isLoggingServiceRunning(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == SignalLoggingService::class.java.name
    }
}

@Composable
fun SignalInfoScreen(telephonyManager: TelephonyManager, connectivityManager: ConnectivityManager) {
    val context = LocalContext.current
    var signalInfo by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val rsrpHistory = remember { mutableStateListOf<Float>() }

    var lastRxBytes by remember { mutableStateOf(TrafficStats.getTotalRxBytes()) }
    var lastTxBytes by remember { mutableStateOf(TrafficStats.getTotalTxBytes()) }
    var lastTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var isLogging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            isLogging = isLoggingServiceRunning(context)
            delay(1000L)
        }
    }

    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val newRx = TrafficStats.getTotalRxBytes()
            val newTx = TrafficStats.getTotalTxBytes()
            val dlMbps = ((newRx - lastRxBytes) * 8 / 1_000_000.0) / ((now - lastTimeMillis) / 1000.0)
            val ulMbps = ((newTx - lastTxBytes) * 8 / 1_000_000.0) / ((now - lastTimeMillis) / 1000.0)

            lastRxBytes = newRx
            lastTxBytes = newTx
            lastTimeMillis = now

            signalInfo = getSignalInfo(context, telephonyManager, connectivityManager, dlMbps, ulMbps, rsrpHistory)

            delay(1000L)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(signalInfo) { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = label, modifier = Modifier.weight(0.4f))
                    Text(text = value, modifier = Modifier.weight(0.6f))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("RSRPグラフ (直近 ${rsrpHistory.size}件)")
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.LightGray)) {
            val points = rsrpHistory.takeLast(50)
            val min = points.minOrNull() ?: -140f
            val max = points.maxOrNull() ?: -40f
            val range = (max - min).takeIf { it > 0 } ?: 1f
            points.forEachIndexed { index, value ->
                val x = size.width / points.size * index
                val y = size.height * (1f - (value - min) / range)
                drawCircle(Color.Red, radius = 4f, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }

        val context = LocalContext.current
        val isLogging by produceState(initialValue = false) {
            while (true) {
                value = isLoggingServiceRunning(context)
                delay(1000)
            }
        }

        Button(
            onClick = {
                if (!isLogging) {
                    context.startForegroundService(Intent(context, SignalLoggingService::class.java))
                } else {
                    context.stopService(Intent(context, SignalLoggingService::class.java))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(if (isLogging) "ログ停止" else "ログ開始")
        }
    }
}

fun getSignalInfo(
    context: Context,
    tm: TelephonyManager,
    cm: ConnectivityManager,
    dlMbps: Double,
    ulMbps: Double,
    rsrpHistory: MutableList<Float>
): List<Pair<String, String>> {
    val items = mutableListOf<Pair<String, String>>()
    val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    items += "測定時刻" to now

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        items += "パーミッション" to "ACCESS_FINE_LOCATION がありません"
        return items
    }

    val cellList = tm.allCellInfo
    val primary = cellList.firstOrNull { it.isRegistered }
    val networkType = tm.dataNetworkType
    items += "通信方式" to networkTypeToString(networkType)
    items += "キャリア" to tm.networkOperatorName
    val mccmnc = tm.networkOperator
    if (mccmnc.length >= 5) {
        items += "MCC / MNC" to "${mccmnc.substring(0, 3)} / ${mccmnc.substring(3)}"
    }

    var rsrp = ""
    var rsrq = ""
    var sinr = ""
    var dbm = ""
    var pci = ""
    var tac = ""
    var eci = ""
    var earfcn = ""
    var band = ""
    var level = ""
    var asu = ""
    var cellType = ""
    var plmn = ""
    var BS_band = ""
    var cqiReport = ""

    if (primary != null) {
        when (primary) {
            is CellInfoLte -> {
                val id = primary.cellIdentity as CellIdentityLte
                val s = primary.cellSignalStrength as CellSignalStrengthLte
                rsrp = "${s.rsrp}".also { rsrpHistory.add(s.rsrp.toFloat()) }
                rsrq = "${s.rsrq}"
                sinr = "${s.rssnr}"
                dbm = "${s.dbm}"
                pci = id.pci.toString()
                tac = id.tac.toString()
                eci = id.ci.toString()
                earfcn = id.earfcn.toString()
                band = BandMapper.getLteBand(id.earfcn)
                asu = s.asuLevel.toString()
                level = getSignalLevelString(s.level)
                cellType = "CellInfoLte"
                plmn = id.mccString + id.mncString
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bandsArray = id.bands
                    if (bandsArray.isNotEmpty()) {
                        BS_band += "B" + bandsArray.joinToString(",B")
                    }
                }
            }
            is CellInfoNr -> {
                val id = primary.cellIdentity as CellIdentityNr
                val s = primary.cellSignalStrength as CellSignalStrengthNr

                rsrp = "${s.dbm}".also { rsrpHistory.add(s.dbm.toFloat()) }
                rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${s.ssRsrq}"
                } else {
                    "不明"
                }
                sinr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    "${s.ssSinr}"
                } else {
                    "不明"
                }
                dbm = "${s.dbm}"
                pci = id.pci.toString()
                tac = id.tac.toString()
                eci = id.nci.toString()
                earfcn = id.nrarfcn.toString()
                band = BandMapper.getNrBand(id.nrarfcn)
                asu = s.asuLevel.toString()
                level = getSignalLevelString(s.level)
                cellType = "CellInfoNr"
                plmn = id.mccString + id.mncString

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bandsArray = id.bands
                    if (bandsArray.isNotEmpty()) {
                        BS_band += "n" + bandsArray.joinToString(",n")
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val cqiList = (primary.cellSignalStrength as? CellSignalStrengthNr)?.csiCqiReport
                    cqiReport = if (!cqiList.isNullOrEmpty()) {
                        "${cqiList.joinToString()} (Avg: ${cqiList.average().toInt()})"
                    } else {
                        "未取得"
                    }
                }
            }

        }
    }

    items += "CellType" to cellType
    items += "ECI / NCI" to eci
    items += "TAC" to tac
    items += "PCI" to pci
    items += "RSRP(dBm)" to rsrp
    items += "RSRQ(dB)" to rsrq
    items += "SINR(dB)" to sinr
    items += "Band" to BS_band
    items += "QCI" to cqiReport
    items += "DL Thp.(Mbps)" to "%.2f".format(dlMbps)
    items += "UL Thp.(Mbps)" to "%.2f".format(ulMbps)
    items += "Neighbor Cell" to cellList.filter { !it.isRegistered }.take(5)
        .mapIndexed { index, ci ->
            when (ci) {
                is CellInfoLte -> "${index + 1}. LTE:${ci.cellIdentity.pci}/${ci.cellSignalStrength.dbm}"
                is CellInfoNr -> "${index + 1}. NR:${(ci.cellIdentity as CellIdentityNr).nci}/${ci.cellSignalStrength.dbm}"
                else -> "${index + 1}. -"
            }
        }.joinToString(separator = "\n")

    items += "OS評価レベル" to level
    items += "EARFCN / NRARFCN" to earfcn
    items += "ASU" to asu
    items += "is_Registered" to (primary?.isRegistered?.toString() ?: "不明")
    items += "Registered PLMN" to plmn
    items += "APN" to (tm.simOperatorName ?: "不明")
    items += "サービス状態" to when (tm.serviceState?.state) {
        ServiceState.STATE_IN_SERVICE -> "圏内"
        ServiceState.STATE_OUT_OF_SERVICE -> "圏外"
        else -> "不明"
    }
    items += "データ接続状態" to when (tm.dataState) {
        TelephonyManager.DATA_CONNECTED -> "接続中"
        TelephonyManager.DATA_DISCONNECTED -> "切断中"
        else -> "不明"
    }
    items += "SIMカード状態" to when (tm.simState) {
        TelephonyManager.SIM_STATE_READY -> "READY"
        TelephonyManager.SIM_STATE_ABSENT -> "ABSENT"
        else -> "OTHER"
    }
    return items
}

fun getSignalLevelString(level: Int): String = when (level) {
    4 -> "Excellent"
    3 -> "Good"
    2 -> "Moderate"
    1 -> "Poor"
    0 -> "None or Unknown"
    else -> "Unknown"
}

fun networkTypeToString(type: Int): String = when (type) {
    1 -> "GPRS"
    2 -> "EDGE"
    3 -> "UMTS"
    8 -> "HSDPA"
    9 -> "HSUPA"
    10 -> "HSPA"
    13 -> "LTE"
    15 -> "HSPAP"
    20 -> "5G NR"
    else -> "不明($type)"
}

