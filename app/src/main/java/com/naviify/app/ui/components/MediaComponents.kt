package com.naviify.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.domain.model.Album
import com.naviify.app.domain.model.Artist
import com.naviify.app.domain.model.Playlist
import com.naviify.app.domain.model.Track
import com.naviify.app.ui.theme.LocalNaviifyPalette
import com.naviify.app.ui.theme.NaviifyBlack
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary
import com.naviify.app.ui.download.TrackDownloadState

@Composable
fun CoverImage(
    coverArtId: String?,
    modifier: Modifier = Modifier,
    size: Int = 256,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val imageRequest = remember(coverArtId, size) {
        val url = CoverUrls.url(coverArtId, size)
        if (url == null) null
        else {
            ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey("cover-$coverArtId-$size")
                .diskCacheKey("cover-$coverArtId-$size")
                .crossfade(false)
                .build()
        }
    }
    Box(
        modifier = modifier
            .background(SurfaceCardHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Ambient backdrop: a soft diagonal gradient that lifts toward the top-right
 * corner in the active theme accent and settles into the background colour.
 * Two stops and one brush, so it costs a single draw call.
 */
@Composable
fun AmbientGlassBackdrop(
    modifier: Modifier = Modifier,
    accentColor: Color = LocalNaviifyPalette.current.accent,
    coverArtId: String? = null,
    backdropHeight: Dp = 440.dp,
    glowAlpha: Float = 0.35f,
    showGlassSheen: Boolean = true,
) {
    // A diagonal wash reads as ambient light rather than a flat flare, and it
    // gives the header depth without drawing attention to itself. The floor on
    // alpha keeps very dim accents from disappearing entirely.
    val backgroundColor = MaterialTheme.colorScheme.background
    val wash = accentColor.copy(alpha = (glowAlpha * 0.42f).coerceIn(0.04f, 0.22f))
    val midWash = accentColor.copy(alpha = wash.alpha * 0.50f)
    // The brush is immutable geometry, so build it once per colour change rather
    // than on every recomposition of the scrolling host.
    val brush = remember(backgroundColor, wash, midWash) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to wash,
                0.35f to midWash,
                0.70f to backgroundColor.copy(alpha = 0.55f),
                1.00f to Color.Transparent,
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(backdropHeight)
            .background(brush = brush),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = SpotifyGreen,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun ErrorBubble(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0x66FF3B30),
                ambientColor = Color(0x40000000),
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xF21C1C1E),
        border = BorderStroke(1.dp, Color(0x38FF453A)),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x28FF453A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Connection Error",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B),
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = CircleShape,
                color = SpotifyGreen,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onRetry),
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ErrorBubble(
            message = message,
            onRetry = onRetry,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = SpotifyGreen)
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Model-driven: `album` is @Immutable, so an unchanged card is skipped.
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        CoverImage(
            coverArtId = album.coverArtId,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(album.artist, album.year?.toString()).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCardHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (!artist.coverArtId.isNullOrBlank()) {
                CoverImage(
                    coverArtId = artist.coverArtId,
                    size = 256,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = artist.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SpotifyGreen,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${artist.albumCount} albums",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
fun AlbumRow(
    album: Album,
    onClick: () -> Unit,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverImage(
            coverArtId = album.coverArtId,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artist ?: "${album.songCount} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isFavorite != null && onToggleFavorite != null) {
            FavoriteIcon(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
    }
}

@Composable
fun ArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(SurfaceCardHigh),
            contentAlignment = Alignment.Center,
        ) {
            val url = remember(artist.coverArtId) { CoverUrls.url(artist.coverArtId, 256) }
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = artist.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = SpotifyGreen,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Artist · ${artist.albumCount} albums",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
            )
        }
        if (isFavorite != null && onToggleFavorite != null) {
            FavoriteIcon(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
    }
}

data class PlaylistTheme(
    val gradientColors: List<Color>,
    val accentColor: Color,
    val patternType: Int, // 0: diagonal curves, 1: concentric rings, 2: diamond, 3: soundwave bars
)

val PLAYLIST_THEMES = listOf(
    PlaylistTheme(listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2)), Color(0xFFE040FB), 0),
    PlaylistTheme(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)), Color(0xFFFFD200), 1),
    PlaylistTheme(listOf(Color(0xFF00B4DB), Color(0xFF0083B0)), Color(0xFF38EF7D), 2),
    PlaylistTheme(listOf(Color(0xFF11998E), Color(0xFF38EF7D)), Color(0xFFFFFFFF), 3),
    PlaylistTheme(listOf(Color(0xFFDA22FF), Color(0xFF9733EE)), Color(0xFFFF77A9), 0),
    PlaylistTheme(listOf(Color(0xFF1A2980), Color(0xFF26D0CE)), Color(0xFF00D2FF), 1),
    PlaylistTheme(listOf(Color(0xFFF7971E), Color(0xFFFFD200)), Color(0xFFFF4B2B), 2),
    PlaylistTheme(listOf(Color(0xFFF857A6), Color(0xFFFF5858)), Color(0xFFFFF066), 3),
    PlaylistTheme(listOf(Color(0xFF2E0854), Color(0xFF4A00E0)), Color(0xFF38BDF8), 0),
    PlaylistTheme(listOf(Color(0xFF8E0E00), Color(0xFF1F1C18)), Color(0xFFFF416C), 1),
    PlaylistTheme(listOf(Color(0xFF654ea3), Color(0xFFeaafc8)), Color(0xFFFFFFFF), 2),
    PlaylistTheme(listOf(Color(0xFF0575E6), Color(0xFF00F260)), Color(0xFFE0F7FA), 3),
)

fun getPlaylistTheme(playlistId: String, playlistName: String): PlaylistTheme {
    if (playlistId == "virtual-library" || playlistName.trim().equals("My own", ignoreCase = true)) {
        return PlaylistTheme(
            listOf(Color(0xFF005C53), Color(0xFF00B074), Color(0xFF00251A)),
            Color(0xFF1DB954),
            0,
        )
    }
    return when (playlistId) {
        "smart-chill" -> PlaylistTheme(
            listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2), Color(0xFF1E085A)),
            Color(0xFFE040FB),
            0,
        )
        "smart-maghribi" -> PlaylistTheme(
            listOf(Color(0xFF00695C), Color(0xFFC2185B), Color(0xFFB71C1C)),
            Color(0xFFFFD54F),
            1,
        )
        "smart-workout" -> PlaylistTheme(
            listOf(Color(0xFFFF416C), Color(0xFFFF4B2B), Color(0xFF8E0E00)),
            Color(0xFFFFD200),
            2,
        )
        "smart-weekly" -> PlaylistTheme(
            listOf(Color(0xFF0575E6), Color(0xFF00F260), Color(0xFF004D40)),
            Color(0xFF1DB954),
            3,
        )
        "smart-night" -> PlaylistTheme(
            listOf(Color(0xFF2E0854), Color(0xFF4A00E0), Color(0xFF090A0F)),
            Color(0xFF38BDF8),
            0,
        )
        "smart-focus" -> PlaylistTheme(
            listOf(Color(0xFF1A2980), Color(0xFF26D0CE), Color(0xFF0B1B3D)),
            Color(0xFF80D8FF),
            1,
        )
        else -> {
            val hash = kotlin.math.abs((playlistName.trim().lowercase() + playlistId).hashCode())
            PLAYLIST_THEMES[hash % PLAYLIST_THEMES.size]
        }
    }
}

@Composable
fun PlaylistCoverArt(
    playlistId: String,
    playlistName: String,
    coverUrl: String? = null,
    coverModel: Any? = null,
    memoryCacheKey: String? = null,
    diskCacheKey: String? = null,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    showLetter: Boolean = false,
) {
    val isSpecialLibrary = playlistId == "virtual-library" || playlistName.trim().equals("My own", ignoreCase = true)
    val theme = remember(playlistId, playlistName) { getPlaylistTheme(playlistId, playlistName) }
    val resolvedModel = coverModel ?: coverUrl.takeIf { !it.isNullOrBlank() }
    // Read outside the remember block below: LocalContext.current is a
    // @Composable accessor and cannot be invoked from a lambda.
    val imageContext = androidx.compose.ui.platform.LocalContext.current

    // Brush statique memoise : il etait recree a chaque recomposition, donc une
    // allocation par vignette et par frame de scroll.
    val gradientBrush = remember(theme) {
        Brush.linearGradient(
            colors = theme.gradientColors,
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    val patternModifier = remember(theme, resolvedModel) {
        if (resolvedModel == null) {
        Modifier.drawBehind {
            val w = size.width
            val h = size.height
            when (theme.patternType) {
                0 -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = w * 0.45f,
                        center = Offset(w * 0.8f, h * 0.8f),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = w * 0.30f,
                        center = Offset(w * 0.2f, h * 0.3f),
                    )
                }
                1 -> {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f),
                        radius = w * 0.75f,
                        center = Offset(w, 0f),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = w * 0.50f,
                        center = Offset(w, 0f),
                    )
                }
                2 -> {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f),
                        topLeft = Offset(w * 0.15f, h * 0.15f),
                        size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.2f, w * 0.2f),
                    )
                }
                else -> {
                    val barWidth = w * 0.05f
                    val gap = w * 0.03f
                    var startX = w * 0.55f
                    val heights = listOf(0.35f, 0.55f, 0.25f, 0.7f)
                    heights.forEach { factor ->
                        val barHeight = h * factor * 0.5f
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.15f),
                            topLeft = Offset(startX, h * 0.85f - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
                        )
                        startX += barWidth + gap
                    }
                }
            }
        }
    } else Modifier
    }

    Box(
        modifier = modifier
            .background(brush = gradientBrush)
            .then(patternModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (resolvedModel != null) {
            val request = remember(resolvedModel, memoryCacheKey, diskCacheKey) {
                coil.request.ImageRequest.Builder(imageContext)
                    .data(resolvedModel)
                    .apply {
                        memoryCacheKey?.let { memoryCacheKey(it) }
                        diskCacheKey?.let { diskCacheKey(it) }
                    }
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = playlistName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(iconSize * 1.6f)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isSpecialLibrary) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize),
                    )
                } else if (playlistId == "smart-chill") {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize),
                    )
                } else if (playlistId == "smart-maghribi") {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(iconSize),
                    )
                } else if (playlistId == "smart-workout") {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize),
                    )
                } else if (playlistId == "smart-weekly") {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(iconSize),
                    )
                } else if (playlistId == "smart-night") {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(iconSize),
                    )
                } else if (playlistId == "smart-focus") {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = Color(0xFF80D8FF),
                        modifier = Modifier.size(iconSize),
                    )
                } else if (showLetter && playlistName.isNotBlank()) {
                    Text(
                        text = playlistName.trim().take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = (iconSize.value * 0.7f).sp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVirtualLibrary = playlist.id == "virtual-library"
    val coverUrl = remember(playlist.id, playlist.coverArtId) {
        if (isVirtualLibrary) null else CoverUrls.playlistUrl(playlist.id, playlist.coverArtId, 256)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        PlaylistCoverArt(
            playlistId = playlist.id,
            playlistName = playlist.name,
            coverUrl = coverUrl,
            // Include the playlist id: the URL is signed per request, so a
            // URL-only key would collide across playlists and serve the wrong art.
            memoryCacheKey = "playlist-cover-${playlist.id}",
            diskCacheKey = "playlist-cover-${playlist.id}",
            iconSize = 44.dp,
            showLetter = true,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (isVirtualLibrary) "All server songs" else "${playlist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SmartMixCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            PlaylistCoverArt(
                playlistId = playlist.id,
                playlistName = playlist.name,
                coverUrl = null,
                iconSize = 42.dp,
                showLetter = false,
                modifier = Modifier.fillMaxSize(),
            )

            // Spotify-style Mix badge in top-left
            Surface(
                color = Color.Black.copy(alpha = 0.50f),
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    text = "MIX",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = SpotifyGreen,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    letterSpacing = 1.sp,
                )
            }

            // Quick Play Button in bottom-right
            if (onPlay != null) {
                Surface(
                    color = SpotifyGreen,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(38.dp)
                        .clickable(onClick = onPlay),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play ${playlist.name}",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = playlist.comment ?: "${playlist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
    }
}

@Composable
fun SpotifyQuickGridTile(
    title: String,
    coverUrl: String? = null,
    playlistId: String? = null,
    onClick: () -> Unit,
    onPlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SurfaceCardHigh.copy(alpha = 0.78f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
            ) {
                if (playlistId != null) {
                    PlaylistCoverArt(
                        playlistId = playlistId,
                        playlistName = title,
                        coverUrl = coverUrl,
                        iconSize = 22.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF1E1E1E)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            )

            if (onPlay != null) {
                Surface(
                    color = SpotifyGreen,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(32.dp)
                        .clickable(onClick = onPlay),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVirtualLibrary = playlist.id == "virtual-library"
    val coverUrl = remember(playlist.id, playlist.coverArtId) {
        if (isVirtualLibrary) null else CoverUrls.playlistUrl(playlist.id, playlist.coverArtId, 256)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistCoverArt(
            playlistId = playlist.id,
            playlistName = playlist.name,
            coverUrl = coverUrl,
            memoryCacheKey = "playlist-cover-${playlist.id}",
            diskCacheKey = "playlist-cover-${playlist.id}",
            iconSize = 26.dp,
            showLetter = true,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isVirtualLibrary) {
                    if (playlist.name == "Downloaded music") {
                        if (playlist.songCount > 0) "Offline · ${playlist.songCount} songs" else "Offline collection"
                    } else {
                        "All server songs · ${playlist.songCount} songs"
                    }
                } else {
                    "Playlist · ${playlist.songCount} songs"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TrackRow(
    index: Int = 0,
    track: Track,
    isFavorite: Boolean? = null,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    downloadState: TrackDownloadState? = null,
    onDownloadToggle: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
    showCover: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val trackActionHandler = LocalTrackActionHandler.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCover) {
            CoverImage(
                coverArtId = track.coverArtId,
                size = 128,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(12.dp))
        } else {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Rounded.Equalizer,
                    contentDescription = "Playing",
                    tint = SpotifyGreen,
                    modifier = Modifier.width(28.dp).size(16.dp),
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.width(28.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (isPlaying) SpotifyGreen else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (downloadState == TrackDownloadState.DONE) {
                    Box(
                        modifier = Modifier
                            .padding(end = 5.dp)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = "Downloaded",
                            tint = NaviifyBlack,
                            modifier = Modifier.size(9.dp),
                        )
                    }
                } else if (downloadState == TrackDownloadState.DOWNLOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(12.dp),
                        color = SpotifyGreen,
                        strokeWidth = 1.5.dp,
                    )
                }
                track.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (track.duration > 0) {
            Text(
                text = formatDuration(track.duration),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }

        if (isFavorite != null && onToggleFavorite != null) {
            FavoriteIcon(isFavorite = isFavorite, onClick = onToggleFavorite)
        }

        IconButton(
            onClick = {
                if (onOptionsClick != null) {
                    onOptionsClick()
                } else {
                    trackActionHandler?.openOptions(track, downloadState, onDownloadToggle)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Track options",
                tint = TextSecondary,
            )
        }
    }
}

@Composable
private fun DownloadButton(state: TrackDownloadState?, onClick: (() -> Unit)?) {
    IconButton(onClick = onClick ?: {}, enabled = onClick != null && state != TrackDownloadState.DOWNLOADING) {
        when (state) {
            TrackDownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = SpotifyGreen,
                    strokeWidth = 2.dp,
                )
            }
            TrackDownloadState.DONE -> {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Downloaded",
                    tint = SpotifyGreen,
                )
            }
            TrackDownloadState.FAILED -> {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Retry download",
                    tint = TextSecondary,
                )
            }
            null -> {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = "Download",
                    tint = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun FavoriteIcon(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = if (isFavorite) SpotifyGreen else TextSecondary,
        )
    }
}

fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "--:--"
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "$minutes:${remaining.toString().padStart(2, '0')}"
}
