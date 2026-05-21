/*
 * Hagrid! - Universal Background Ad Silencer for Android
 * Copyright (C) 2026 Pawan Simha R <iampawansimha.2004@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.silencer_android.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("hagrid_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { 
                mutableStateOf(prefs.getBoolean("is_dark_theme", systemDark)) 
            }
            
            HagridTheme(darkTheme = isDarkTheme) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                        PermissionGuard {
                            MainScreen(
                                onGrantPermission = { openNotificationSettings() },
                                onIgnoreBatteryOptimizations = { requestIgnoreBatteryOptimizations() },
                                onSimulateAd = { sendSimulationBroadcast(NotificationMuterService.ACTION_SIMULATE_AD) },
                                onSimulateTrack = { sendSimulationBroadcast(NotificationMuterService.ACTION_SIMULATE_TRACK) },
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { 
                                    isDarkTheme = !isDarkTheme
                                    prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
                                }
                            )
                        }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(bottom = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Hagrid Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Hagrid!",
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                color = Color.Black,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "Universal Ad Silencer",
                fontSize = 14.sp,
                color = GoogleBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Build on May 2026 by",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Pawan Simha R",
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onGrantPermission: () -> Unit,
    onIgnoreBatteryOptimizations: () -> Unit,
    onSimulateAd: () -> Unit,
    onSimulateTrack: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val isRunning by NotificationMuterService.isServiceActive.collectAsState()
    val isEngineEnabled by NotificationMuterService.isEngineRunning.collectAsState()
    val adsMuted by NotificationMuterService.totalAdsMuted.collectAsState()
    val dailyMutes by NotificationMuterService.dailyMutes.collectAsState()
    val hourlyMutes by NotificationMuterService.hourlyMutes.collectAsState()
    
    var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Bottom Sheet state for Developer Sandbox
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
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

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            DeveloperSandboxSheet(
                onSimulateAd = onSimulateAd,
                onSimulateTrack = onSimulateTrack,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
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
                        Column(verticalArrangement = Arrangement.spacedBy((-6).dp)) {
                            Text(
                                "Hagrid!", 
                                fontWeight = FontWeight.Black, 
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontSize = 18.sp,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text("Universal Ad Silencer", fontSize = 10.sp, color = GoogleBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings, 
                            contentDescription = "Settings",
                            tint = Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.shadow(2.dp)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().shadow(4.dp)) {
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
        containerColor = MaterialTheme.colorScheme.background
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val monitoringStatus = when {
                    !isRunning -> "Offline"
                    !isEngineEnabled -> "Paused"
                    else -> "Active"
                }
                val engineStatus = when {
                    !isRunning -> "Inactive"
                    !isEngineEnabled -> "Idle"
                    else -> "Scanning"
                }
                
                InfoMiniCard(
                    label = "Monitoring",
                    value = monitoringStatus,
                    icon = Icons.Default.FlashOn,
                    color = if (isEngineEnabled && isRunning) Color(0xFFFF9800) else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                InfoMiniCard(
                    label = "Engine",
                    value = engineStatus,
                    icon = Icons.Default.Security,
                    color = if (isEngineEnabled && isRunning) GoogleGreen else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
            }

            // Health Checklist (Crucial for Reviewers)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if(isRunning && hasPermission) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if(isRunning && hasPermission) GoogleGreen else GoogleRed
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if(isRunning && hasPermission) "System Healthy: Engine Active" else "Action Required: Complete Setup",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                    Text("Enable Notification Access", fontWeight = FontWeight.Bold, color = Color.White)
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
                ChartCard("Mutes per Day", Modifier.weight(1f), isBar = true, data = dailyMutes)
                ChartCard("Hourly Activity", Modifier.weight(1f), isBar = false, data = hourlyMutes)
            }

            // Historical Summary
            HistoricalSummaryCard(totalMutes = adsMuted, isActive = isEngineEnabled)
        }
    }
}

@Composable
fun DeveloperSandboxSheet(
    onSimulateAd: () -> Unit,
    onSimulateTrack: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Developer Sandbox",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Build on May 2026 by", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pawan Simha R", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("iampawansimha.2004@gmail.com", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SocialIcon("x", Color.Black) { openUrl(context, "https://x.com/pawansimha") }
                        SocialIcon("in", GoogleBlue) { openUrl(context, "https://www.linkedin.com/in/pawansimha") }
                        SocialIcon("git", Color.Black) { openUrl(context, "https://github.com/PawanSimha") }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Testing Tools", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onSimulateAd,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoogleYellow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simulate Ad", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSimulateTrack,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoogleBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simulate Track", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = if (isDarkTheme) GoogleBlue else GoogleYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isDarkTheme) "Dark Mode" else "Light Mode",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GoogleBlue,
                    checkedTrackColor = GoogleBlue.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun SocialIcon(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun InfoMiniCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun ChartCard(title: String, modifier: Modifier = Modifier, isBar: Boolean, data: FloatArray) {
    val maxValue = data.maxOrNull()?.takeIf { it > 0 } ?: 1f
    
    Card(
        modifier = modifier.height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(if(isBar) "Real data from your connected devices this week." else "Service engagement patterns from all logs.", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                if (isBar) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                        data.forEach { count ->
                            val heightFactor = (count / maxValue).coerceIn(0.05f, 1f)
                            Box(modifier = Modifier.width(10.dp).fillMaxHeight(heightFactor).background(GoogleBlue.copy(alpha = 0.6f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        }
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (data.any { it > 0 }) {
                            val path = androidx.compose.ui.graphics.Path()
                            val stepX = size.width / (data.size - 1)
                            
                            data.forEachIndexed { index, count ->
                                val x = index * stepX
                                val y = size.height - (count / maxValue * size.height)
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, color = GoogleBlue, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
                        } else {
                            // Show a flat line if no data
                            drawLine(color = GoogleBlue.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 4f)
                        }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Historical Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Real performance metrics from your local database.", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalMutes", fontSize = 28.sp, fontWeight = FontWeight.Black, color = GoogleBlue)
                    Text("TOTAL MUTES LOGGED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
                Box(modifier = Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.surfaceVariant))
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
            Text("Build on May 2026 by", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
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

@Composable
fun PermissionGuard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasNotificationAccess by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isIgnoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = isNotificationServiceEnabled(context)
                isIgnoringBattery = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasNotificationAccess && isIgnoringBattery) {
        content()
    } else {
        SetupScreen(
            hasNotificationAccess = hasNotificationAccess,
            isIgnoringBattery = isIgnoringBattery,
            onGrantNotification = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            },
            onIgnoreBattery = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun SetupScreen(
    hasNotificationAccess: Boolean,
    isIgnoringBattery: Boolean,
    onGrantNotification: () -> Unit,
    onIgnoreBattery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(100.dp).clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Setup Required",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            "Hagrid! needs these permissions to run invisibly in the background and mute ads effectively.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PermissionItem(
            title = "Notification Access",
            description = "Needed to detect when an ad starts playing.",
            isGranted = hasNotificationAccess,
            onClick = onGrantNotification
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PermissionItem(
            title = "Battery Optimization",
            description = "Needed to keep the service alive in the background.",
            isGranted = isIgnoringBattery,
            onClick = onIgnoreBattery
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        if (hasNotificationAccess && isIgnoringBattery) {
            Button(
                onClick = { /* Will be handled by PermissionGuard state change */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoogleGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("GET STARTED", fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                "Please enable both permissions to continue",
                fontSize = 12.sp,
                color = GoogleRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) GoogleGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isGranted) BorderStroke(1.dp, GoogleGreen) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isGranted) GoogleGreen else Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 12.sp, color = Color.Gray)
            }
            
            if (!isGranted) {
                TextButton(onClick = onClick) {
                    Text("ALLOW", fontWeight = FontWeight.Black, color = GoogleBlue)
                }
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
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
