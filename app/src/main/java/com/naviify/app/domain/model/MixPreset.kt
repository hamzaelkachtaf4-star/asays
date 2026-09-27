package com.naviify.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Les cinq familles de presets du menu Mix, dans l'ordre des pages de la maquette
 * de reference : Volume, EQ, Filter, Effects, Looping.
 */
enum class MixCategory(val title: String) {
    VOLUME("Volume"),
    EQ("EQ"),
    FILTER("Filter"),
    EFFECTS("Effects"),
    LOOPING("Looping"),
}

/**
 * Un preset du menu Mix : le nom et la description affiches, plus le reglage reel
 * applique au moteur de lecture.
 *
 * [needsRender] vaut vrai quand le moteur temps reel ne sait pas (encore) produire
 * exactement ce preset : filtre passe-bas/passe-haut, echo, boucles de mesures. Ces
 * presets restent selectionnables et **s'entendent pour de vrai dans l'apercu**,
 * parce que l'apercu est fabrique par le serveur (ffmpeg) et non par le telephone.
 * En lecture normale, l'app applique le mode le plus proche indique par [mode].
 * Aucun effet n'est simule en silence : ce qui n'est pas audible est signale ici.
 */
@Immutable
data class MixPreset(
    val id: String,
    val category: MixCategory,
    val name: String,
    val description: String,
    val mode: PlaylistMixMode,
    val overlapMs: Int? = null,
    val bassSwap: Boolean? = null,
    val equalPower: Boolean? = null,
    val loopBeats: Int? = null,
    val needsRender: Boolean = false,
)

/** Catalogue complet du menu Mix. */
object MixPresets {

    val all: List<MixPreset> = listOf(
        // ---------------------------------------------------------------- Volume
        MixPreset(
            id = "vol_overlap", category = MixCategory.VOLUME, name = "Overlap",
            description = "Les deux morceaux jouent ensemble, sans creux de silence",
            mode = PlaylistMixMode.FADE, bassSwap = false, equalPower = true,
        ),
        MixPreset(
            id = "vol_fade_in_out", category = MixCategory.VOLUME, name = "Fade in fade out",
            description = "Fondu de sortie sur l'ancien, fondu d'entree sur le nouveau",
            mode = PlaylistMixMode.FADE, bassSwap = false, equalPower = true,
        ),
        MixPreset(
            id = "vol_cut_in_fade_out", category = MixCategory.VOLUME, name = "Cut in fade out",
            description = "Le nouveau demarre net pendant que l'ancien s'efface",
            mode = PlaylistMixMode.SLAM, bassSwap = false, equalPower = false,
            needsRender = true,
        ),
        MixPreset(
            id = "vol_fade_in_cut_out", category = MixCategory.VOLUME, name = "Fade in cut out",
            description = "L'ancien s'efface puis le nouveau monte en fondu",
            mode = PlaylistMixMode.FADE, bassSwap = false, equalPower = false,
            needsRender = true,
        ),
        MixPreset(
            id = "vol_center_cut", category = MixCategory.VOLUME, name = "Center cut",
            description = "Coupe franche au milieu du recouvrement",
            mode = PlaylistMixMode.SLAM, bassSwap = false, equalPower = false,
            needsRender = true,
        ),

        // -------------------------------------------------------------------- EQ
        MixPreset(
            id = "eq_center_bass", category = MixCategory.EQ, name = "Center bass swap",
            description = "Echange des basses au milieu du recouvrement",
            mode = PlaylistMixMode.AUTO, bassSwap = true, equalPower = true,
        ),
        MixPreset(
            id = "eq_start_bass", category = MixCategory.EQ, name = "Start bass swap",
            description = "Coupe les basses de l'ancien des le debut",
            mode = PlaylistMixMode.AUTO, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "eq_end_bass", category = MixCategory.EQ, name = "End bass swap",
            description = "Garde les basses jusqu'a la toute fin du recouvrement",
            mode = PlaylistMixMode.AUTO, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "eq_3_band", category = MixCategory.EQ, name = "3-band fade",
            description = "Basses, mediums et aigus s'echangent chacun a leur tour",
            mode = PlaylistMixMode.AUTO, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "eq_quick_cut", category = MixCategory.EQ, name = "Quick bass cut",
            description = "Retire les basses d'un coup juste avant la bascule",
            mode = PlaylistMixMode.SLAM, bassSwap = true, equalPower = false,
            needsRender = true,
        ),

        // ---------------------------------------------------------------- Filter
        MixPreset(
            id = "fl_lp_lp", category = MixCategory.FILTER, name = "Low pass in low pass out",
            description = "Les deux morceaux s'eteignent et reviennent en sourdine",
            mode = PlaylistMixMode.MELT, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "fl_lp_hp", category = MixCategory.FILTER, name = "Low pass in high pass out",
            description = "Sortie etouffee, entree qui s'eclaircit",
            mode = PlaylistMixMode.MELT, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "fl_hp_out", category = MixCategory.FILTER, name = "High pass out",
            description = "L'ancien perd ses basses et disparait",
            mode = PlaylistMixMode.RISE, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "fl_hp_in", category = MixCategory.FILTER, name = "High pass in",
            description = "Le nouveau arrive d'abord sans basses",
            mode = PlaylistMixMode.RISE, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "fl_hp_hp", category = MixCategory.FILTER, name = "High pass in high pass out",
            description = "Montee par les aigus des deux cotes",
            mode = PlaylistMixMode.RISE, bassSwap = true, equalPower = true,
            needsRender = true,
        ),

        // --------------------------------------------------------------- Effects
        MixPreset(
            id = "fx_reverb_out", category = MixCategory.EFFECTS, name = "Reverb out end",
            description = "La fin de l'ancien part dans une reverbe",
            mode = PlaylistMixMode.FADE, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "fx_echo_half_cut", category = MixCategory.EFFECTS, name = "Echo 1/2 cut end",
            description = "Un demi-temps d'echo puis coupe",
            mode = PlaylistMixMode.SLAM, bassSwap = true, equalPower = false,
            needsRender = true,
        ),
        MixPreset(
            id = "fx_echo_half_out", category = MixCategory.EFFECTS, name = "Echo 1/2 out end",
            description = "Un demi-temps d'echo en sortie",
            mode = PlaylistMixMode.FADE, bassSwap = true, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "fx_echo_three_quarter", category = MixCategory.EFFECTS, name = "Echo 3/4 cut end",
            description = "Trois quarts de temps d'echo puis coupe",
            mode = PlaylistMixMode.SLAM, bassSwap = true, equalPower = false,
            needsRender = true,
        ),

        // --------------------------------------------------------------- Looping
        MixPreset(
            id = "lp_none", category = MixCategory.LOOPING, name = "None",
            description = "Aucune boucle : le morceau part directement",
            mode = PlaylistMixMode.FADE, loopBeats = 0, equalPower = true,
        ),
        MixPreset(
            id = "lp_1", category = MixCategory.LOOPING, name = "1 beat loop",
            description = "Boucle d'un temps sur la fin de l'ancien",
            mode = PlaylistMixMode.FADE, loopBeats = 1, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "lp_2", category = MixCategory.LOOPING, name = "2 beat loop",
            description = "Boucle de deux temps, le temps de caler le nouveau",
            mode = PlaylistMixMode.FADE, loopBeats = 2, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "lp_4", category = MixCategory.LOOPING, name = "4 beat loop",
            description = "Boucle d'une mesure complete",
            mode = PlaylistMixMode.FADE, loopBeats = 4, equalPower = true,
            needsRender = true,
        ),
        MixPreset(
            id = "lp_8", category = MixCategory.LOOPING, name = "8 beat loop",
            description = "Boucle de deux mesures, ideale pour poser un drop",
            mode = PlaylistMixMode.FADE, loopBeats = 8, equalPower = true,
            needsRender = true,
        ),
    )

    fun byCategory(category: MixCategory): List<MixPreset> =
        all.filter { it.category == category }

    fun byId(id: String?): MixPreset? = all.firstOrNull { it.id == id }

    /** Preset dont le reglage correspond exactement a la configuration courante. */
    fun matching(
        mode: PlaylistMixMode,
        bassSwap: Boolean,
        equalPower: Boolean,
        loopBeats: Int? = null,
    ): MixPreset? = all.firstOrNull {
        it.mode == mode && it.bassSwap == bassSwap && it.equalPower == equalPower &&
            (it.loopBeats ?: 0) == (loopBeats ?: 0)
    }
}
