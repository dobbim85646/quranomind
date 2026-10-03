package com.example.quranomind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quranomind.data.model.CosmicWord
import com.example.quranomind.data.model.ScienceVerse
import com.example.quranomind.ui.theme.EmeraldGreen
import com.example.quranomind.ui.theme.GoldAccent
import com.example.quranomind.ui.theme.NavyPrimary
import com.example.quranomind.ui.viewmodel.QuranViewModel

@Composable
fun ScienceScreen(
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    val isEnglish by viewModel.isEnglish.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val cosmicWords = viewModel.cosmicWords
    val scienceVerses = viewModel.scienceVerses

    val filteredCosmic = remember(searchQuery, cosmicWords) {
        if (searchQuery.isBlank()) cosmicWords
        else {
            val q = searchQuery.trim().lowercase()
            cosmicWords.filter {
                it.word.contains(q) ||
                it.tafsir.contains(q) ||
                it.scienceTerm.lowercase().contains(q) ||
                it.explanation.contains(q)
            }
        }
    }

    val filteredVerses = remember(searchQuery, scienceVerses) {
        if (searchQuery.isBlank()) scienceVerses
        else {
            val q = searchQuery.trim().lowercase()
            scienceVerses.filter {
                it.surahName.contains(q) ||
                it.verseText.contains(q) ||
                it.scientificExplanation.contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero Explanation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldGreen,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnglish) "Cosmic Signs & Quranic Science" else "الآيات الكونية والإعجاز العلمي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isEnglish)
                            "Explore profound astronomical and scientific terms mentioned in the Holy Quran alongside modern astrophysics & biology discoveries."
                        else
                            "استكشف المصطلحات الكونية والحقائق العلمية المذكورة في القرآن الكريم وتوافقها مع مكتشفات الفيزياء الفلكية والأحياء الحديثة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (isEnglish) "Search cosmic words or verses..." else "ابحث في الكلمات الكونية أو الآيات العلمية..."
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("science_search_field"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = if (isEnglish) "Cosmic Words (${filteredCosmic.size})" else "الكلمات الكونية (${filteredCosmic.size})",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (isEnglish) "Scientific Verses (${filteredVerses.size})" else "الآيات العلمية (${filteredVerses.size})",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCosmic) { item ->
                    CosmicWordCard(item = item, isEnglish = isEnglish, viewModel = viewModel)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredVerses) { item ->
                    ScienceVerseCard(item = item, isEnglish = isEnglish, viewModel = viewModel)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun CosmicWordCard(
    item: CosmicWord,
    isEnglish: Boolean,
    viewModel: QuranViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cosmic_word_card_${item.word}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = item.word,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.scienceTerm,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = {
                        viewModel.speakText("${item.word}. المعنى: ${item.tafsir}. العلم: ${item.scienceTerm}. ${item.explanation}")
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        viewModel.copyToClipboard("الكلمة القرآنية: ${item.word}\nالتفسير: ${item.tafsir}\nالمعنى العلمي: ${item.scienceTerm}\nالشرح: ${item.explanation}")
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "📖 ${if (isEnglish) "Linguistic Tafsir:" else "التفسير اللغوي:"} ${item.tafsir}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "🔬 ${if (isEnglish) "Scientific Explanation:" else "الشرح العلمي المعاصر:"} ${item.explanation}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ScienceVerseCard(
    item: ScienceVerse,
    isEnglish: Boolean,
    viewModel: QuranViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("science_verse_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "سورة ${item.surahName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row {
                    IconButton(onClick = {
                        viewModel.speakText("سورة ${item.surahName}: ${item.verseText}. ${item.scientificExplanation}")
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        viewModel.copyToClipboard("سورة ${item.surahName}:\n﴿ ${item.verseText} ﴾\n\nالإعجاز العلمي والتفسير:\n${item.scientificExplanation}")
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "﴿ ${item.verseText} ﴾",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 28.sp
                ),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.scientificExplanation,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
