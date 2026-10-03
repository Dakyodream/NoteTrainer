package com.dakyodream.notetrainer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "ℹ️ Aide",
        body = """
            **Comment jouer ?**

            Trois modes de jeu s'inspirant du Simon vous aident à mémoriser les notes :

            1. **Note ➜ Nom** : une note apparaît sur la partition (clé de Sol ou de Fa, choisie au hasard ou dans les réglages). Touchez le bouton portant le nom de la note.

            2. **Nom ➜ Note** : l'inverse ! Le nom de la note s'affiche (avec son octave). Touchez la bonne position sur la portée. Utilisez les boutons ♭ ♮ ♯ pour choisir l'altération.

            3. **Oreille ➜ Note** : le jeu joue un son pendant 1 seconde. Écoutez la séquence de notes et placez chaque note sur la portée, dans l'ordre (comme un Simon !).

            **Difficultés**

            - Facile : notes naturelles uniquement, larges tolérances.
            - Moyen : dièses et bémols possibles.
            - Difficile : plus de manches, moins de temps.
            - Expert : séquences plus longues, tout est permis.

            **Astuces**

            - En clé de Sol, les notes des lignes (de bas en haut) sont E G B D F (« Chaque Gamin Badinant Déniche Facilement »).
            - Les notes des interlignes sont F A C E.
            - Prenez votre temps : il n'y a pas de limite de temps par note dans les deux premiers modes.
        """.trimIndent(),
        onBack = onBack
    )
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "👤 Crédits",
        body = """
            **NoteTrainer** — version 1.0.0

            Projet open source et gratuit, publié sous licence MIT.

            Développé pour la communauté des apprenants pianistes. 💜

            **Technologies**

            - Kotlin + Jetpack Compose (Material 3)
            - Sons générés localement par synthèse (aucun fichier audio externe, aucune API payante)

            **Remerciements**

            - La documentation Android (developer.android.com)
            - Tous les contributeurs et joueuses/joueurs qui proposeront des améliorations !

            Envie de contribuer ? Consultez la licence et le README du projet.
        """.trimIndent(),
        onBack = onBack
    )
}

@Composable
fun LicenseScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "⚖️ Licence MIT",
        body = """
            Copyright (c) 2026 Nicoud Guillaume

            Permission is hereby granted, free of charge, to any person obtaining a copy
            of this software and associated documentation files (the "Software"), to deal
            in the Software without restriction, including without limitation the rights
            to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
            copies of the Software, and to permit persons to whom the Software is
            furnished to do so, subject to the following conditions:

            The above copyright notice and this permission notice shall be included in all
            copies or substantial portions of the Software.

            THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
            IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
            FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
            AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
            LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
            OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
            SOFTWARE.
        """.trimIndent(),
        onBack = onBack
    )
}

@Composable
private fun SimpleInfoScreen(title: String, body: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        body.split("\n\n").forEach { paragraph ->
            Text(paragraph.trimIndent(), fontSize = 14.sp, lineHeight = 20.sp)
        }
        Button(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth()
        ) {
            Text("← Retour au menu")
        }
    }
}
