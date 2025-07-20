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

class MainActivity : ComponentActivity() {

    private var permissionRequested = false
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )

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
                    // 権限がリクエスト中の場合、まだ UI を表示しないようにする
                    if (!permissionRequested || hasAllPermissions(this)) {
                        AppScaffold(telephonyManager, connectivityManager)
                    }
                }
            }
        }
    }

    private fun hasAllPermissions(context: Context): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
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
                recreate() // 権限が付与された場合に UI を再描画
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
        Text("Version 1.0.0")
        Text("KayamaSoft")
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

@Composable
fun SignalInfoScreen(telephonyManager: TelephonyManager, connectivityManager: ConnectivityManager) {
    val context = LocalContext.current
    var signalInfo by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val rsrpHistory = remember { mutableStateListOf<Float>() }

    val logDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ksmeasuring")
    if (!logDir.exists()) logDir.mkdirs()
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val logFile = File(logDir, "KSM_${timestamp}.csv")

    var lastRxBytes by remember { mutableStateOf(TrafficStats.getTotalRxBytes()) }
    var lastTxBytes by remember { mutableStateOf(TrafficStats.getTotalTxBytes()) }
    var lastTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var headerWritten by remember { mutableStateOf(logFile.exists()) }

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

            try {
                val outputStream = FileOutputStream(logFile, true)
                val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
                if (!headerWritten) {
                    writer.write("\uFEFF") // BOM
                    writer.append(signalInfo.joinToString(",") { escapeCsv(it.first) }).append("\n")
                    headerWritten = true
                }
                writer.append(signalInfo.joinToString(",") { escapeCsv(it.second) }).append("\n")
                writer.flush()
                writer.close()
            } catch (_: Exception) {}

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
    var nci = ""
    var nrTAC = ""
    var timingAdvance = "-"

    if (primary != null) {
        when (primary) {
            is CellInfoLte -> {
                val id = primary.cellIdentity as CellIdentityLte
                val s = primary.cellSignalStrength as CellSignalStrengthLte
                rsrp = "${s.rsrp} dBm".also { rsrpHistory.add(s.rsrp.toFloat()) }
                rsrq = "${s.rsrq} dB"
                sinr = "${s.rssnr} dB"
                dbm = "${s.dbm} dBm"
                pci = id.pci.toString()
                tac = id.tac.toString()
                eci = id.ci.toString()
                earfcn = id.earfcn.toString()
                band = BandMapper.getLteBand(id.earfcn)
                asu = s.asuLevel.toString()
                level = getSignalLevelString(s.level)
                timingAdvance = s.timingAdvance.toString()
                cellType = "CellInfoLte"
                plmn = id.mccString + id.mncString
            }
            is CellInfoNr -> {
                val id = primary.cellIdentity as CellIdentityNr
                val s = primary.cellSignalStrength as CellSignalStrengthNr
                rsrp = "${s.dbm} dBm".also { rsrpHistory.add(s.dbm.toFloat()) }
                sinr = "不明"
                dbm = "${s.dbm} dBm"
                pci = "-"
                nrTAC = id.tac.toString()
                nci = id.nci.toString()
                earfcn = id.nrarfcn.toString()
                band = BandMapper.getNrBand(id.nrarfcn)
                asu = s.asuLevel.toString()
                level = getSignalLevelString(s.level)
                cellType = "CellInfoNr"
                plmn = id.mccString + id.mncString
            }
        }
    }

    items += "ECI" to eci
    items += "TAC" to (if (networkType == TelephonyManager.NETWORK_TYPE_NR) nrTAC else tac)
    items += "PCI" to pci
    items += "RSRP" to rsrp
    items += "RSRQ" to rsrq
    items += "SINR / SNR" to sinr
    items += "受信電力の絶対値" to dbm
    items += "DLスループット" to "%.2f Mbps".format(dlMbps)
    items += "ULスループット" to "%.2f Mbps".format(ulMbps)
    items += "Neighbor Cell" to cellList.filter { !it.isRegistered }.take(5)
        .joinToString { ci ->
            when (ci) {
                is CellInfoLte -> "LTE:${ci.cellIdentity.pci}/${ci.cellSignalStrength.dbm}"
                is CellInfoNr -> "NR:${(ci.cellIdentity as CellIdentityNr).nci}/${ci.cellSignalStrength.dbm}"
                else -> "-"
            }
        }
    items += "CAの有無" to "不明"
    items += "アンテナピクトの数" to level
    items += "EARFCN / NRARFCN" to earfcn
    items += "Band" to band
    items += "ASU" to asu
    items += "is_Registered" to (primary?.isRegistered?.toString() ?: "不明")
    items += "CellType" to cellType
    items += "Registered PLMN" to plmn
    items += "NR Cell Identity" to nci
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
    items += "APN" to (tm.simOperatorName ?: "不明")
    return items
}

fun getSignalLevelString(level: Int): String = when (level) {
    4 -> "4"
    3 -> "3"
    2 -> "2"
    1 -> "1"
    0 -> "0"
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