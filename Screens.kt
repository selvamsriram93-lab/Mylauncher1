package com.example.mylauncher

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val textShadow = Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 8f)

@Composable
fun LauncherApp(resumeTick: Int, homeTick: Int) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    var dock by remember { mutableStateOf(emptyList<DockItem>()) }
    var drawerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(resumeTick) {
        val loaded = withContext(Dispatchers.Default) { loadApps(ctx) to loadDock(ctx) }
        apps = loaded.first
        dock = loaded.second
    }
    LaunchedEffect(homeTick) { drawerOpen = false }
    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(dock = dock, onOpenDrawer = { drawerOpen = true })
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            DrawerScreen(apps = apps, onLaunched = { drawerOpen = false })
        }
    }
}

@Composable
fun HomeScreen(dock: List<DockItem>, onOpenDrawer: () -> Unit) {
    val ctx = LocalContext.current
    val now by produceState(Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }
    val timePattern = if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
    val time = SimpleDateFormat(timePattern, Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -120f) onOpenDrawer() },
                    onVerticalDrag = { _, dy -> total += dy }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        Text(time, color = Color.White, fontSize = 64.sp, style = TextStyle(shadow = textShadow))
        Text(date, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, style = TextStyle(shadow = textShadow))
        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onOpenDrawer() }
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text("Search apps", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
        }
        Spacer(Modifier.weight(1f))
        Text("Swipe up for all apps", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, style = TextStyle(shadow = textShadow))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dock.forEach { item ->
                Image(
                    bitmap = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { runCatching { ctx.startActivity(item.intent) } }
                )
            }
        }
    }
}

@Composable
fun DrawerScreen(apps: List<AppEntry>, onLaunched: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) { apps.filter { it.label.contains(query, ignoreCase = true) } }
    val grouped = remember(filtered) {
        filtered.groupBy {
            val c = it.label.firstOrNull()?.uppercaseChar() ?: '#'
            if (c.isLetter()) c else '#'
        }.toSortedMap()
    }
    val fieldColor = Color.White.copy(alpha = 0.12f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6101418))
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .padding(horizontal = 12.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search ${apps.size} apps") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = fieldColor,
                unfocusedContainerColor = fieldColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize()
        ) {
            grouped.forEach { (letter, list) ->
                item(key = "header_$letter", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        letter.toString(),
                        color = Color(0xFF8AB4F8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                    )
                }
                items(list, key = { it.pkg + "/" + it.cls }) { app ->
                    AppCell(app, onLaunched)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCell(app: AppEntry, onLaunched: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    launchApp(ctx, app)
                    onLaunched()
                },
                onLongClick = { openAppInfo(ctx, app) }
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
