package com.sakib.sakibstream

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.style.TextOverflow
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

private val Bg = Color(0xFF090B10)
private val Panel = Color(0xFF171B24)
private val Accent = Color(0xFFE5093A)
private val Muted = Color(0xFF9AA3B2)
private val prefsName = "sakibstream_settings"

data class StreamSource(val id: String, val name: String, val url: String, val group: String)

private val movieSources = listOf(
    StreamSource("7movies-ws", "7Movies", "https://7movies.ws/", "Movies"),
    StreamSource("flystream", "FlyStream", "https://flystream.net/", "Movies"),
    StreamSource("shuttltv", "ShuttlTV", "https://shuttletv.pk/", "Movies"),
    StreamSource("vivarium", "Vivarium", "https://vivarium.st/", "Movies"),
    StreamSource("arrowtv", "ArrowTV", "https://arrowtv.net/", "Movies"),
    StreamSource("cinezo", "Cinezo", "https://www.cinezo.st/", "Movies"),
    StreamSource("cinejoy", "CineJoy", "https://cinejoy.pk/search", "Movies"),
    StreamSource("7movies-ac", "7Movies.ac", "https://7movies.ac/", "Movies"),
    StreamSource("cinecat", "Cinecat", "https://cinecat.eu/", "Movies"),
    StreamSource("popcornmovies", "PopcornMovies", "https://popcornmovies.st/", "Movies"),
    StreamSource("bingebox", "Bingebox", "https://bingebox.st/", "Movies"),
    StreamSource("ghostflix", "Ghostflix", "https://ghostflix.st/", "Movies"),
    StreamSource("fprime", "FPrime", "https://fprime.at/", "Movies"),
    StreamSource("meowtv", "MeowTV", "https://meowtv.ru/", "Movies"),
    StreamSource("sleepy", "Sleepy", "https://sleepy.lat/", "Movies"),
    StreamSource("moviebite", "MovieBite", "https://moviebite.org/", "Movies"),
    StreamSource("moviebox", "MovieBox", "https://officialmoviebox.com/", "Movies"),
    StreamSource("123movies", "123Movies", "https://123moviesfree.club/", "Movies"),
    StreamSource("m2box", "M2Box", "https://m2box.org/", "Movies"),
    StreamSource("youtube", "YouTube", "https://www.youtube.com/", "Movies"),
    StreamSource("mkvdrama", "MKVDrama", "https://mkvdrama.net/", "Movies"),
    StreamSource("zetmoon", "Zetmoon", "https://zetmoon.live/", "Movies")
)

private val liveSources = listOf(
    StreamSource("ntv-matches", "NTV Matches", "https://ntv.st/matches/raptor", "Live"),
    StreamSource("zlive", "ZLive", "https://zlive.st/", "Live"),
    StreamSource("thedaddy", "TheDaddy", "https://thedaddy.li/", "Live"),
    StreamSource("iptv-web", "IPTV Web", "https://iptv-web.app/", "Live"),
    StreamSource("easy-web-tv", "Easy Web TV", "https://zhangboheng.github.io/Easy-Web-TV-M3u8/routes/tv.html", "Live"),
    StreamSource("tim-streams", "Tim Streams", "https://tim-streams.com/", "Live"),
    StreamSource("strmcrnr", "StrmCrnr", "https://strmcrnr.st/", "Live"),
    StreamSource("ppv", "PPV", "https://ppv.st/", "Live"),
    StreamSource("sportsbite", "SportsBite", "https://sportsbite.org/", "Live")
)

private val allSources = movieSources + liveSources

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SakibStreamApp() }
    }
}

@Composable
fun SakibStreamApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE) }
    var tab by remember { mutableStateOf("Home") }
    var activeGroup by remember {
        mutableStateOf(prefs.getString("last_group", "Movies") ?: "Movies")
    }
    var activeSourceId by remember {
        mutableStateOf(prefs.getString("last_source", "7movies-ws") ?: "7movies-ws")
    }
    var browsing by remember { mutableStateOf(true) }
    var failedIds by remember { mutableStateOf(setOf<String>()) }
    var statusMessage by remember { mutableStateOf("") }

    val currentSource = allSources.firstOrNull { it.id == activeSourceId } ?: movieSources.first()
    val groupSources = if (activeGroup == "Live") liveSources else movieSources

    fun persistSource(source: StreamSource) {
        activeSourceId = source.id
        activeGroup = source.group
        prefs.edit().putString("last_source", source.id)
            .putString("last_group", source.group).apply()
    }

    fun chooseSource(source: StreamSource) {
        failedIds = emptySet()
        statusMessage = ""
        persistSource(source)
        browsing = true
    }

    fun switchGroup(group: String) {
        failedIds = emptySet()
        statusMessage = ""
        val first = if (group == "Live") liveSources.first() else movieSources.first()
        persistSource(first)
    }

    fun handleSourceFailure() {
        if (activeSourceId in failedIds) return
        val nowFailed = failedIds + activeSourceId
        failedIds = nowFailed
        val currentIndex = groupSources.indexOfFirst { it.id == activeSourceId }.let { if (it < 0) 0 else it }
        val candidates = (1..groupSources.size)
            .map { offset -> groupSources[(currentIndex + offset) % groupSources.size] }
            .filter { it.id !in nowFailed }
        if (candidates.isNotEmpty()) {
            val next = candidates.first()
            statusMessage = "Couldn't load ${currentSource.name}. Trying ${next.name}…"
            persistSource(next)
        } else {
            statusMessage = "All listed sources failed to load. Choose another source or retry."
        }
    }

    BackHandler(enabled = browsing) { browsing = false }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Accent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (browsing) {
                SourceBrowser(
                    source = currentSource,
                    sources = groupSources,
                    activeGroup = activeGroup,
                    status = statusMessage,
                    onChoose = { chooseSource(it) },
                    onSwitchGroup = { switchGroup(it) },
                    onClose = { browsing = false },
                    onFailure = { handleSourceFailure() }
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    when (tab) {
                        "Movies" -> SourceDirectory("Movies", movieSources, activeSourceId, ::chooseSource)
                        "Live" -> SourceDirectory("Live TV & Sports", liveSources, activeSourceId, ::chooseSource)
                        "Providers" -> ProviderInfoScreen()
                        else -> HomeScreen(
                            lastSource = currentSource,
                            onContinue = { browsing = true },
                            onMovies = { tab = "Movies" },
                            onLive = { tab = "Live" },
                            onProviders = { tab = "Providers" }
                        )
                    }
                    NavigationBar(containerColor = Panel, contentColor = Color.White) {
                        listOf("Home", "Movies", "Live", "Providers").forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = {
                                    Icon(
                                        when (item) {
                                            "Movies" -> Icons.Default.Movie
                                            "Live" -> Icons.Default.Tv
                                            "Providers" -> Icons.Default.Extension
                                            else -> Icons.Default.Home
                                        }, contentDescription = item
                                    )
                                },
                                label = { Text(item) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Accent,
                                    indicatorColor = Color(0xFF30202A)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    lastSource: StreamSource,
    onContinue: () -> Unit,
    onMovies: () -> Unit,
    onLive: () -> Unit,
    onProviders: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SAKIB", color = Accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("STREAM", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(28.dp))
        Text("STREAMING, SIMPLIFIED", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
        Text("Your next watch.", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(22.dp))
        HomeTile("Continue watching", lastSource.name, "Open your last selected source", Icons.Default.PlayArrow, onContinue)
        Spacer(Modifier.height(12.dp))
        HomeTile("Movies", "Browse movie sources", "Choose from your movie websites", Icons.Default.Movie, onMovies)
        Spacer(Modifier.height(12.dp))
        HomeTile("Live TV & Sports", "Browse live sources", "Switch between live and sports websites", Icons.Default.Tv, onLive)
        Spacer(Modifier.height(12.dp))
        HomeTile("Provider manager", "Provider repositories", "Repository compatibility information", Icons.Default.Extension, onProviders)
        Spacer(Modifier.weight(1f))
        Text("Sources are third-party websites. Availability and playback depend on each source.",
            color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun HomeTile(
    title: String, subtitle: String, detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(46.dp).background(Color(0xFF30202A), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Accent)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = Color.White, fontSize = 13.sp)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun SourceDirectory(
    title: String, sources: List<StreamSource>, selectedId: String, onOpen: (StreamSource) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("SAKIB", color = Accent, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text("STREAM", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        Text(title, Modifier.padding(horizontal = 20.dp), fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("${sources.size} sources · Tap to open or switch", Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
            color = Muted, fontSize = 12.sp)
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(sources) { source ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                        .background(if (source.id == selectedId) Color(0xFF30202A) else Panel, RoundedCornerShape(13.dp))
                        .clickable { onOpen(source) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (source.group == "Live") Icons.Default.LiveTv else Icons.Default.Language,
                        null, tint = Accent)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(source.name, fontWeight = FontWeight.SemiBold)
                        Text(source.url, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (source.id == selectedId) {
                        Text("LAST", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.OpenInNew, null, tint = Muted)
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SourceBrowser(
    source: StreamSource,
    sources: List<StreamSource>,
    activeGroup: String,
    status: String,
    onChoose: (StreamSource) -> Unit,
    onSwitchGroup: (String) -> Unit,
    onClose: () -> Unit,
    onFailure: () -> Unit
) {
    val context = LocalContext.current
    val latestFailure by rememberUpdatedState(onFailure)
    val webView = remember {
        WebView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) latestFailure()
                }

                override fun onReceivedHttpError(
                    view: WebView?, request: WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true &&
                        (errorResponse?.statusCode ?: 200) >= 400) latestFailure()
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    handler?.cancel()
                    latestFailure()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    // Cosmetic cleanup only: hide common footer/copyright blocks, not site navigation.
                    view?.evaluateJavascript(
                        """(function(){
                          try {
                            document.querySelectorAll('footer,[class*="footer"],[id*="footer"]').forEach(function(e){e.style.display='none';});
                            document.querySelectorAll('body *').forEach(function(e){
                              if(e.children.length===0){
                                var t=(e.innerText||e.textContent||'').toLowerCase();
                                if(t.length<180 && (t.includes('all rights reserved') || t.includes('© copyright'))){
                                  e.style.display='none';
                                }
                              }
                            });
                          } catch(e) {}
                        })();""", null
                    )
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
    }

    LaunchedEffect(source.url) {
        if (webView.url != source.url) webView.loadUrl(source.url)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Panel).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
            Column(Modifier.weight(1f)) {
                Text("SAKIBSTREAM", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(source.name, color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = { webView.reload() }) { Icon(Icons.Default.Refresh, "Reload") }
        }
        Row(
            Modifier.fillMaxWidth().background(Bg).padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = activeGroup == "Movies", onClick = { onSwitchGroup("Movies") }, label = { Text("Movies") })
            FilterChip(selected = activeGroup == "Live", onClick = { onSwitchGroup("Live") }, label = { Text("Live TV") })
        }
        Row(
            Modifier.fillMaxWidth().background(Bg).horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            sources.forEach { item ->
                FilterChip(
                    selected = item.id == source.id,
                    onClick = { if (item.id != source.id) onChoose(item) },
                    label = { Text(item.name, maxLines = 1) }
                )
            }
        }
        if (status.isNotBlank()) {
            Text(status, Modifier.fillMaxWidth().background(Color(0xFF30202A)).padding(8.dp),
                color = Color.White, fontSize = 11.sp)
        }
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ProviderInfoScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SAKIB", color = Accent, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text("STREAM", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(22.dp))
        Text("Provider extensions", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "The previous ZIP contained a demo-only repository form. It did not download manifests or run extensions. This build keeps provider support clearly separated from website shortcuts.",
            color = Muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))
        Text("Vega compatibility", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            "Vega providers use their own JavaScript provider contract and sandbox bridge. They cannot be loaded as ordinary URLs or as the starter JSON sample. A compatible isolated runtime and provider API adapter must be ported before Vega extensions can work here.",
            color = Muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(18.dp))
        Text("Reference repositories", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text("Vega app: github.com/vega-org/vega-app", color = Color.White, fontSize = 12.sp)
        Text("Vega provider template: github.com/vega-org/providers-template", color = Color.White, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text("Only use sources and media you are authorized to access.",
            color = Muted, fontSize = 11.sp)
    }
}
