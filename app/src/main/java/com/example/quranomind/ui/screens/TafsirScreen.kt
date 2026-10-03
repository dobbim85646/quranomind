package com.example.quranomind.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quranomind.data.model.InterpreterMode
import com.example.quranomind.data.model.SurahInfo
import com.example.quranomind.ui.theme.GoldAccent
import com.example.quranomind.ui.theme.NavyLight
import com.example.quranomind.ui.theme.NavyPrimary
import com.example.quranomind.ui.theme.QuranAyahBg
import com.example.quranomind.ui.theme.QuranAyahBgDark
import com.example.quranomind.ui.viewmodel.QuranViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TafsirScreen(
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    val selectedSurah by viewModel.selectedSurah.collectAsState()
    val selectedAyah by viewModel.selectedAyah.collectAsState()
    val selectedInterpreter by viewModel.selectedInterpreter.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val tafsirResult by viewModel.tafsirResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFavorited by viewModel.isCurrentFavorited.collectAsState()
    val isSpeaking by viewModel.ttsHelper.isSpeaking.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showSurahDialog by remember { mutableStateOf(false) }

    if (showSurahDialog) {
        SurahSelectDialog(
            surahs = viewModel.allSurahs,
            currentSurah = selectedSurah,
            onSurahSelected = {
                viewModel.selectSurah(it)
                showSurahDialog = false
            },
            onDismiss = { showSurahDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Selection Controls Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("surah_picker_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Surah Picker Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable { showSurahDialog = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEnglish) "Selected Surah" else "السورة المختارة",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${selectedSurah.number}. ${selectedSurah.arabicName} (${selectedSurah.englishName})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Surah",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Ayah Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousAyah() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("prev_ayah_button"),
                            enabled = selectedAyah > 1 || selectedSurah.number > 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Ayah",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Ayah $selectedAyah of ${selectedSurah.ayahCount}"
                                else "الآية $selectedAyah من ${selectedSurah.ayahCount}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        IconButton(
                            onClick = { viewModel.nextAyah() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("next_ayah_button"),
                            enabled = selectedAyah < selectedSurah.ayahCount || selectedSurah.number < 114
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Ayah",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Quick Jump Ayahs Horizontal Slider or selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val quickAyahs = listOf(1, 5, 10, 20, 50, 100, selectedSurah.ayahCount)
                            .filter { it <= selectedSurah.ayahCount }
                            .distinct()

                        Text(
                            text = if (isEnglish) "Jump:" else "انتقال:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        quickAyahs.forEach { quickAyah ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (quickAyah == selectedAyah) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.selectAyah(quickAyah) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = quickAyah.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (quickAyah == selectedAyah) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Interpreter Selection Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isEnglish) "Select Interpreter / School" else "اختر منهج التفسير:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InterpreterMode.entries.forEach { mode ->
                        val isSelected = mode == selectedInterpreter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectInterpreter(mode) },
                            label = {
                                Text(
                                    text = if (isEnglish) mode.titleEnglish else mode.titleArabic,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("interpreter_chip_${mode.id}")
                        )
                    }
                }
            }
        }

        // Ayah & Tafsir Content
        item {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = GoldAccent)
                        Text(
                            text = if (isEnglish) "Loading Tafsir & Ayah..." else "جاري استحضار الآية والتفسير...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (tafsirResult != null) {
                val res = tafsirResult!!

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Holy Ayah Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ayah_text_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) QuranAyahBgDark else QuranAyahBg
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = GoldAccent.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "﴿ ${res.ayahText} ﴾",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 23.sp,
                                    lineHeight = 38.sp
                                ),
                                fontFamily = FontFamily.Serif,
                                textAlign = TextAlign.Center,
                                color = if (isDarkMode) GoldAccent else NavyPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "— سورة ${res.surahName} [الآية ${res.ayahNumber}] —",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tafsir Result Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tafsir_content_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Header badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (res.isAiGenerated) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (res.isAiGenerated) Icons.Default.AutoAwesome else Icons.Default.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (res.isAiGenerated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = res.interpreterName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (res.isAiGenerated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                // Favorite Toggle Button
                                IconButton(
                                    onClick = { viewModel.toggleFavoriteCurrent() },
                                    modifier = Modifier.testTag("favorite_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorited) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tafsir Text
                            Text(
                                text = res.tafsirArabic,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 28.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = if (isEnglish) TextAlign.Start else TextAlign.Justify
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons: Audio TTS, Copy
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (isSpeaking) {
                                            viewModel.stopSpeaking()
                                        } else {
                                            viewModel.speakText("${res.ayahText}. ${res.tafsirArabic}")
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tts_play_button")
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSpeaking) (if (isEnglish) "Stop" else "إيقاف")
                                        else (if (isEnglish) "Listen" else "استماع")
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.copyToClipboard("سورة ${res.surahName} - آية ${res.ayahNumber}:\n${res.ayahText}\n\nالتفسير (${res.interpreterName}):\n${res.tafsirArabic}")
                                    },
                                    modifier = Modifier.testTag("copy_tafsir_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (isEnglish) "Copy" else "نسخ")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SurahSelectDialog(
    surahs: List<SurahInfo>,
    currentSurah: SurahInfo,
    onSurahSelected: (SurahInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) surahs
        else {
            val q = searchQuery.trim().lowercase()
            surahs.filter {
                it.number.toString() == q ||
                it.arabicName.contains(q) ||
                it.englishName.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اختر السورة الكريمة (1 - 114)",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث بالاسم أو الرقم...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered) { surah ->
                        val isSelected = surah.number == currentSurah.number
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                                .clickable { onSurahSelected(surah) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = surah.number.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = surah.arabicName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${surah.englishName} • ${surah.ayahCount} آية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = surah.revelationType,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
