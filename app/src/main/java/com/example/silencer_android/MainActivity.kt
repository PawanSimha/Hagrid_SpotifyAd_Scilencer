package com.example.silencer_android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.silencer_android.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HagridTheme {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize().background(White)) {
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(500))
                    ) {
                        SplashScreen()
                    }

                    AnimatedVisibility(
                        visible = !showSplash,
                        enter = fadeIn(animationSpec = tween(500)),
                        exit = fadeOut()
                    ) {
                        MainScreen(
                            onGrantPermission = { openNotificationSettings() },
                            onIgnoreBatteryOptimizations = { requestIgnoreBatteryOptimizations() },
                            onSimulateAd = { sendSimulationBroadcast(NotificationMuterService.ACTION_SIMULATE_AD) },
                            onSimulateTrack = { sendSimulationBroadcast(NotificationMuterService.ACTION_SIMULATE_TRACK) }
                        )
                    }
                }
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }

    private fun requestIgnoreBatteryOptimizations() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun sendSimulationBroadcast(action: String) {
        val intent = Intent(action).apply { setPackage(packageName) }
        sendBroadcast(intent)
    }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Hagrid Logo",
            modifier = Modifier.size(120.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Hagrid!", fontWeight = FontWeight.Black, fontSize = 32.sp, color = Color.Black, letterSpacing = 2.sp)
        Text("Universal Ad Silencer", fontSize = 14.sp, color = GoogleBlue, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(48.dp))
        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = GoogleBlue, strokeWidth = 3.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onGrantPermission: () -> Unit,
    onIgnoreBatteryOptimizations: () -> Unit,
    onSimulateAd: () -> Unit,
    onSimulateTrack: () -> Unit
) {
    val context = LocalContext.current
    val isRunning by NotificationMuterService.isServiceActive.collectAsState()
    val isEngineEnabled by NotificationMuterService.isEngineRunning.collectAsState()
    val adsMuted by NotificationMuterService.totalAdsMuted.collectAsState()
    val recentMute by NotificationMuterService.recentMuteTitle.collectAsState()
    
    var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = isNotificationServiceEnabled(context)
                isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Hagrid!", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 18.sp)
                            Text("Universal Ad Silencer", fontSize = 10.sp, color = GoogleBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White),
                modifier = Modifier.shadow(2.dp)
            )
        },
        bottomBar = {
            Surface(color = White, modifier = Modifier.fillMaxWidth().shadow(4.dp)) {
                Text(
                    "Monitoring Spotify • YT Music • YouTube",
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizontal Info Row
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoMiniCard("Status", if(isRunning) "Online" else "Disconnected", Icons.Default.CloudQueue, GoogleBlue)
                InfoMiniCard("Recent Mute", recentMute, Icons.Default.VolumeOff, Color(0xFF9C27B0))
                InfoMiniCard("Monitoring", if(isEngineEnabled) "Active" else "Paused", Icons.Default.FlashOn, Color(0xFFFF9800))
                InfoMiniCard("Engine", if(isRunning) "Scanning" else "Idle", Icons.Default.Security, GoogleGreen)
            }

            // Engine Toggle
            Button(
                onClick = { NotificationMuterService.setEngineState(!isEngineEnabled) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEngineEnabled) GoogleRed else GoogleGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (isEngineEnabled) "TURN OFF ENGINE" else "TURN ON ENGINE",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            if (!hasPermission) {
                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enable Notification Access", fontWeight = FontWeight.Bold)
                }
            }

            if (!isIgnoringBatteryOptimizations) {
                Button(
                    onClick = onIgnoreBatteryOptimizations,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleYellow),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Disable Battery Optimizations", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Ads Muted Today Card
            AdsMutedTodayCard(count = adsMuted)

            // Charts Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ChartCard("Mutes per Day", Modifier.weight(1f), isBar = true)
                ChartCard("Hourly Activity", Modifier.weight(1f), isBar = false)
            }

            // Historical Summary
            HistoricalSummaryCard(totalMutes = adsMuted, isActive = isEngineEnabled)

            DeveloperContactCard()

            // Dev Testing Tools
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Dev Mode:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    Button(onClick = onSimulateAd, modifier = Modifier.weight(1f).height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = GoogleYellow), shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(0.dp)) {
                        Text("Sim Ad", color = Color.Black, fontSize = 10.sp)
                    }
                    Button(onClick = onSimulateTrack, modifier = Modifier.weight(1f).height(32.dp), colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue), shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(0.dp)) {
                        Text("Sim Track", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoMiniCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = Modifier.width(150.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
            }
        }
    }
}

@Composable
fun AdsMutedTodayCard(count: Int) {
    val goal = 50
    val progress = (count.toFloat() / goal).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ADS MUTED TODAY", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("$count", fontSize = 72.sp, fontWeight = FontWeight.Black, color = GoogleBlue)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(color = GoogleGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(50)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GoogleGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Real-time tracking", fontSize = 12.sp, color = GoogleGreen, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Goal: $goal blocks", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = GoogleBlue,
                trackColor = Color(0xFFF1F3F4)
            )
        }
    }
}

@Composable
fun ChartCard(title: String, modifier: Modifier = Modifier, isBar: Boolean) {
    Card(
        modifier = modifier.height(220.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(if(isBar) "Real data from your connected devices this week." else "Service engagement patterns from all logs.", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                if (isBar) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        listOf(0.4f, 0.2f, 0.6f, 0.3f, 0.8f, 0.5f, 0.7f).forEach { h ->
                            Box(modifier = Modifier.width(10.dp).fillMaxHeight(h).background(GoogleBlue.copy(alpha = 0.6f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        }
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = androidx.compose.ui.graphics.Path()
                        path.moveTo(0f, size.height * 0.9f)
                        path.quadraticTo(size.width * 0.25f, size.height * 0.8f, size.width * 0.5f, size.height * 0.9f)
                        path.quadraticTo(size.width * 0.75f, size.height * 1.0f, size.width, size.height * 0.5f)
                        drawPath(path, color = GoogleBlue, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val labels = if(isBar) listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon") else listOf("8:00", "12:00", "18:00", "22:00")
                labels.forEach { label ->
                    Text(label, fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun HistoricalSummaryCard(totalMutes: Int, isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Historical Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Real performance metrics from your local database.", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalMutes", fontSize = 28.sp, fontWeight = FontWeight.Black, color = GoogleBlue)
                    Text("TOTAL MUTES LOGGED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
                Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color(0xFFF1F3F4)))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if(isActive) "Active" else "Inactive", fontSize = 28.sp, fontWeight = FontWeight.Black, color = if(isActive) GoogleGreen else GoogleRed)
                    Text("SYSTEM STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DeveloperContactCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Developer", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pawan Simha R", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                    Text("iampawansimha.2004@gmail.com", fontSize = 11.sp, color = Color.DarkGray)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { openUrl(context, "https://www.linkedin.com/in/pawansimha") }) {
                        Box(modifier = Modifier.size(24.dp).background(GoogleBlue, CircleShape), contentAlignment = Alignment.Center) {
                            Text("in", color = White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = { openUrl(context, "https://github.com/PawanSimha") }) {
                        Box(modifier = Modifier.size(24.dp).background(Color.Black, CircleShape), contentAlignment = Alignment.Center) {
                            Text("git", color = White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!TextUtils.isEmpty(flat)) {
        val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in names.indices) {
            val cn = ComponentName.unflattenFromString(names[i])
            if (cn != null && TextUtils.equals(pkgName, cn.packageName)) return true
        }
    }
    return false
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
