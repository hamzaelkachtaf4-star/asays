package com.naviify.app.ui.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naviify.app.domain.model.MixCategory
import com.naviify.app.domain.model.MixPreset
import com.naviify.app.domain.model.MixPresets
import com.naviify.app.domain.model.MixWaveform
import com.naviify.app.ui.theme.MixColors
import com.naviify.app.ui.theme.SurfaceCard
import com.naviify.app.ui.theme.SurfaceCardHigh
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

/**
 * Le menu Mix, calque sur la maquette de reference : la forme d'onde REELLE des deux
 * morceaux en haut (servie par le serveur maison), un bouton pour **ecouter la
 * transition** sans rien appliquer, puis les cinq familles de presets en pages
 * balayables (Volume, EQ, Filter, Effects, Looping) avec la coche verte sur le
 * preset actif.
 *
 * Aucune forme d'onde n'est inventee : si [outgoingWaveform] / [incomingWaveform]
 * sont nuls (serveur injoignable), le panneau reste vide et seule la courbe de
 * volume est dessinee.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixPresetPickerSheet(
    onDismiss: () -> Unit,
    outgoingLabel: String,
    incomingLabel: String,
    selectedPresetId: String? = null,
    outgoingMeta: String? = null,
    incomingMeta: String? = null,
    outgoingWaveform: MixWaveform? = null,
    incomingWaveform: MixWaveform? = null,
    isAuditioning: Boolean = false,
    onPresetSelected: (MixPreset) -> Unit = {},
    onAudition: () -> Unit = {},
    onCustomize: (MixCategory) -> Unit = {},
    onDone: () -> Unit = {},
) {
    val categories = remember { MixCategory.values().toList() }
    val pagerState = rememberPagerState(pageCount = { categories.size })

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MixColors.panel,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {

            // ------------------------------------------------ forme d'onde + essai
            MixWaveformPanel(
                outgoingLabel = outgoingLabel,
                incomingLabel = incomingLabel,
                outgoingMeta = outgoingMeta,
                incomingMeta = incomingMeta,
                outgoing = outgoingWaveform,
                incoming = incomingWaveform,
                isAuditioning = isAuditioning,
                onAudition = onAudition,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Presets",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            // ------------------------------------------------- cinq pages de presets
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 12.dp,
            ) { page ->
                val category = categories[page]
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    MixCategoryHeader(
                        category = category,
                        onCustomize = { onCustomize(category) },
                    )
                    MixPresets.byCategory(category).forEach { preset ->
                        MixPresetRow(
                            preset = preset,
                            isSelected = preset.id == selectedPresetId,
                            onClick = { onPresetSelected(preset) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            MixPagerDots(count = categories.size, active = pagerState.currentPage)
            Spacer(Modifier.height(18.dp))

            // ------------------------------------------------------------ termine
            Surface(
                onClick = onDone,
                color = MixColors.done,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "Done",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 15.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** Panneau du haut : les deux morceaux, leur forme d'onde, le bouton d'ecoute. */
@Composable
private fun MixWaveformPanel(
    outgoingLabel: String,
    incomingLabel: String,
    outgoingMeta: String?,
    incomingMeta: String?,
    outgoing: MixWaveform?,
    incoming: MixWaveform?,
    isAuditioning: Boolean,
    onAudition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MixColors.panelHigh,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrackTag(outgoingLabel, outgoingMeta, Modifier.weight(1f))
                Text(
                    text = "→",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                TrackTag(incomingLabel, incomingMeta, Modifier.weight(1f), alignEnd = true)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                contentAlignment = Alignment.Center,
            ) {
                MixWaveformCanvas(
                    outgoing = outgoing,
                    incoming = incoming,
                    modifier = Modifier.fillMaxWidth().height(112.dp),
                )
                // Bouton d'ecoute : joue l'apercu fabrique par le serveur, sans
                // toucher a la file de lecture.
                Surface(
                    onClick = onAudition,
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(width = 54.dp, height = 38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAuditioning) Icons.Rounded.Pause
                            else Icons.Rounded.PlayArrow,
                            contentDescription = "Ecouter la transition",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            if (outgoing == null && incoming == null) {
                Text(
                    text = "Forme d'onde indisponible (serveur DJ injoignable)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun TrackTag(
    label: String,
    meta: String?,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        if (!meta.isNullOrBlank()) {
            Text(text = meta, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}

/**
 * Dessine la forme d'onde reelle : la fin du morceau sortant a gauche, le debut de
 * l'entrant a droite, avec la zone de recouvrement au centre. Deux series = deux
 * couleurs, comme la maquette (crete en bleu, basses en orange).
 *
 * Rien n'est dessine si le serveur n'a pas fourni de mesures : mieux vaut un panneau
 * vide qu'une courbe inventee.
 */
@Composable
private fun MixWaveformCanvas(
    outgoing: MixWaveform?,
    incoming: MixWaveform?,
    modifier: Modifier = Modifier,
) {
    val outPeaks = outgoing?.peaks.orEmpty()
    val outLow = outgoing?.low.orEmpty()
    val inPeaks = incoming?.peaks.orEmpty()
    val inLow = incoming?.low.orEmpty()

    Canvas(modifier = modifier) {
        if (outPeaks.isEmpty() && inPeaks.isEmpty()) return@Canvas
        val mid = size.height / 2f
        val half = size.width / 2f
        val gap = (half / 80f).coerceAtLeast(3f)

        // Cote sortant : les 80 dernieres tranches (la fin du morceau), a gauche.
        val outFrom = (outPeaks.size - 80).coerceAtLeast(0)
        var x = gap
        for (i in outFrom until outPeaks.size) {
            if (x > half - gap) break
            val h = (outPeaks[i] / 1000f) * (size.height * 0.46f)
            val l = (outLow.getOrElse(i) { 0 } / 1000f) * (size.height * 0.40f)
            drawLine(MixColors.wave, Offset(x, mid - h), Offset(x, mid + h), 2f)
            drawLine(MixColors.waveLow, Offset(x, mid - l), Offset(x, mid + l), 1.5f)
            x += gap
        }

        // Cote entrant : les 80 premieres tranches, a droite du centre.
        x = half + gap
        val inTo = minOf(80, inPeaks.size)
        for (i in 0 until inTo) {
            if (x > size.width - gap) break
            val h = (inPeaks[i] / 1000f) * (size.height * 0.46f)
            val l = (inLow.getOrElse(i) { 0 } / 1000f) * (size.height * 0.40f)
            drawLine(MixColors.wave, Offset(x, mid - h), Offset(x, mid + h), 2f)
            drawLine(MixColors.waveLow, Offset(x, mid - l), Offset(x, mid + l), 1.5f)
            x += gap
        }

        // Zone de recouvrement au centre.
        drawRect(
            color = MixColors.curveIncoming.copy(alpha = 0.08f),
            topLeft = Offset(half - gap * 4, 0f),
            size = androidx.compose.ui.geometry.Size(gap * 8, size.height),
        )
    }
}

/** Titre de famille : la barre de couleur, le nom, et le lien "Customize". */
@Composable
private fun MixCategoryHeader(category: MixCategory, onCustomize: () -> Unit) {
    val accent = MixColors.accent(category)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Text(
            text = category.title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        Text(
            text = "Customize",
            color = MixColors.selected,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onCustomize() },
        )
    }
}

/** Une ligne de preset : nom, description courte, coche verte si actif. */
@Composable
private fun MixPresetRow(
    preset: MixPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MixColors.rowSelected else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = preset.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Preset actif",
                    tint = MixColors.selected,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Les points sous les pages, comme sur la maquette. */
@Composable
private fun MixPagerDots(count: Int, active: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isActive = index == active
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isActive) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color.White.copy(alpha = 0.92f)
                        else Color.White.copy(alpha = 0.22f),
                    ),
            )
        }
    }
}
