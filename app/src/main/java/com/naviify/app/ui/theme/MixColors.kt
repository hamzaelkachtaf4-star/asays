package com.naviify.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.naviify.app.domain.model.MixCategory

/**
 * Palette du menu Mix, relevee sur la maquette de reference envoyee par Tayeb.
 *
 * Chaque famille de presets a sa couleur d'accent (la petite barre verticale a
 * gauche du titre) et cette couleur se retrouve dans la courbe correspondante
 * tracee sur la forme d'onde : le volume en cyan, l'EQ en jaune, le filtre en
 * rose, les effets en bleu pale, la boucle en orange.
 *
 * L'app a un theme clair et un theme sombre, mais le menu Mix garde ces teintes
 * sombres dans les deux cas : c'est un panneau d'edition audio, il doit rester
 * lisible au-dessus de la forme d'onde.
 */
object MixColors {
    val volume = Color(0xFF3EA6FF)      // cyan : courbes de volume
    val eq = Color(0xFFFFC107)          // ambre : coupe des basses
    val filter = Color(0xFFE040FB)      // magenta : passe-bas / passe-haut
    val effects = Color(0xFF8AB4F8)     // bleu pale : reverb, echo
    val looping = Color(0xFFFF9800)     // orange : boucles de mesures

    /** Coche verte d'un preset actif (comme sur la maquette). */
    val selected = Color(0xFF1ED760)

    /** Bouton principal "Done" : rose/rouge de la maquette. */
    val done = Color(0xFFE5375E)

    /** Enveloppe sortante (pointille) et enveloppe entrante (pleine). */
    val curveOutgoing = Color(0x99FFFFFF)
    val curveIncoming = Color(0xFFFF4D6D)

    /** Forme d'onde : crete et bande basse, comme les deux couleurs du serveur. */
    val wave = Color(0xFF3D6BE8)
    val waveLow = Color(0xFFC8782D)

    /** Fond des panneaux de la feuille Mix. */
    val panel = Color(0xFF141416)
    val panelHigh = Color(0xFF1C1C1F)
    val rowSelected = Color(0x14FFFFFF)

    fun accent(category: MixCategory): Color = when (category) {
        MixCategory.VOLUME -> volume
        MixCategory.EQ -> eq
        MixCategory.FILTER -> filter
        MixCategory.EFFECTS -> effects
        MixCategory.LOOPING -> looping
    }
}
