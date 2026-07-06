package com.finoria.app.ui.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Une ligne de fonctionnalité présentée sur l'écran de bienvenue.
 *
 * @property icon        icône Material (décorative — le sens est porté par le texte)
 * @property color       couleur d'accent, distincte par fonctionnalité
 * @property title       titre court (2-4 mots)
 * @property description phrase courte expliquant le bénéfice
 */
private data class WelcomeFeature(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val description: String,
)

/**
 * Liste ordonnée des fonctionnalités mises en avant au premier démarrage.
 *
 * NB : la version Android est 100 % locale (Room), sans synchronisation cloud —
 * la fonctionnalité « Synchronisation cloud » de la version iOS est donc omise.
 */
private val WelcomeFeatures = listOf(
    WelcomeFeature(
        icon = Icons.Filled.AccountBalanceWallet,
        color = Color(0xFF4CAF50),
        title = "Gestion multi-comptes",
        description = "Créez et gérez plusieurs comptes bancaires avec des styles personnalisés.",
    ),
    WelcomeFeature(
        icon = Icons.Filled.Bolt,
        color = Color(0xFF2196F3),
        title = "Transactions rapides",
        description = "Ajoutez revenus et dépenses en quelques secondes grâce aux raccourcis.",
    ),
    WelcomeFeature(
        icon = Icons.Filled.Autorenew,
        color = Color(0xFFFF9800),
        title = "Transactions récurrentes",
        description = "Automatisez vos dépenses et revenus réguliers : loyer, salaire, abonnements…",
    ),
    WelcomeFeature(
        icon = Icons.Filled.PieChart,
        color = Color(0xFF9C27B0),
        title = "Analyses détaillées",
        description = "Visualisez la répartition de vos dépenses et revenus par catégorie.",
    ),
    WelcomeFeature(
        icon = Icons.Filled.CalendarMonth,
        color = Color(0xFFF44336),
        title = "Navigation temporelle",
        description = "Explorez vos finances par jour, mois ou année dans le calendrier.",
    ),
    WelcomeFeature(
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        color = Color(0xFF009688),
        title = "Prévisions futures",
        description = "Anticipez votre solde avec les transactions à venir et récurrentes.",
    ),
    WelcomeFeature(
        icon = Icons.Filled.Category,
        color = Color(0xFFE91E63),
        title = "Catégories personnalisées",
        description = "Créez vos propres catégories avec une icône et une couleur pour organiser vos transactions.",
    ),
)

/**
 * Écran de bienvenue affiché **une seule fois**, au tout premier démarrage.
 *
 * En-tête + liste défilable des fonctionnalités + bouton « Continuer » toujours
 * visible en bas. La seule façon de sortir est le bouton : le retour système est
 * neutralisé ([BackHandler]) pour garantir que le flag `hasSeenWelcome` est posé
 * via un chemin unique et contrôlé ([onContinue]).
 */
@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bloque le geste/bouton retour tant que l'utilisateur n'a pas appuyé sur « Continuer ».
    BackHandler(enabled = true) { }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Consomme les taps sur les zones vides pour qu'aucune interaction ne
            // « fuie » vers le Scaffold superposé en dessous (barre de navigation…).
            .pointerInput(Unit) { detectTapGestures { } }
            .systemBarsPadding(),
    ) {
        // Contenu défilable : peut dépasser la hauteur de l'écran sur petits appareils.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "Bienvenue dans",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Finoria",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(36.dp))

            WelcomeFeatures.forEach { feature ->
                FeatureRow(feature)
                Spacer(Modifier.height(24.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        // Bouton d'action, toujours visible sous le contenu défilable.
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .height(52.dp),
        ) {
            Text(
                text = "Continuer",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun FeatureRow(feature: WelcomeFeature) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(feature.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null, // décoratif : le sens est porté par le titre + description
                tint = feature.color,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
