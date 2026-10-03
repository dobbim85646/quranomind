package com.example.quranomind.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quranomind.ui.theme.EmeraldGreen
import com.example.quranomind.ui.theme.GoldAccent
import com.example.quranomind.ui.theme.NavyPrimary
import com.example.quranomind.ui.viewmodel.QuranViewModel

@Composable
fun DreamScreen(
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    val dreamText by viewModel.dreamText.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val dreamResult by viewModel.dreamInterpretation.collectAsState()
    val isDreamLoading by viewModel.isDreamLoading.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val isSpeaking by viewModel.ttsHelper.isSpeaking.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Islamic Guidance Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (isEnglish) "Islamic Guidance on Dreams" else "ضوابط شرعية لتفسير الرؤى والأحلام",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnglish)
                            "True visions are from Allah. Interpretations are conjectural guidelines to instill hope and guidance, and cannot replace Islamic rulings or certainty."
                        else
                            "الرؤيا الصالحة من الله تعالى، وتعبيرها ظني استئناسي للاستبشار والتوجيه، ولا يترتب عليها حكم شرعي ولا قطيعة رحم. استعذ بالله من وساوس الشيطان.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Dream Input Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dream_input_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isEnglish) "Describe Your Dream" else "صف رؤياك أو منامك:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Gender Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Ruler / Gender:" else "صاحب الرؤيا:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = gender == "ذكر",
                        onClick = { viewModel.setGender("ذكر") },
                        label = { Text(if (isEnglish) "Male" else "ذكر") }
                    )
                    FilterChip(
                        selected = gender == "أنثى",
                        onClick = { viewModel.setGender("أنثى") },
                        label = { Text(if (isEnglish) "Female" else "أنثى") }
                    )
                }

                // Description Input
                OutlinedTextField(
                    value = dreamText,
                    onValueChange = { viewModel.setDreamText(it) },
                    placeholder = {
                        Text(
                            text = if (isEnglish)
                                "Write the details of the dream, symbols, and emotions felt..."
                            else
                                "اكتب تفاصيل الرؤيا وما رأيته بوضوح والرموز البارزة..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("dream_text_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Interpret Button
                Button(
                    onClick = { viewModel.interpretDream() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("interpret_dream_button"),
                    enabled = dreamText.isNotBlank() && !isDreamLoading
                ) {
                    if (isDreamLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isEnglish) "Analyzing Symbols..." else "جاري تحليل الرموز...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Interpret Dream" else "تفسير المنام وفق الضوابط الشرعية",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dream Result
        if (dreamResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dream_result_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (isEnglish) "Interpretation & Advice" else "تأويل الرؤيا والتوجيه الإيماني",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Row {
                            IconButton(onClick = {
                                if (isSpeaking) {
                                    viewModel.stopSpeaking()
                                } else {
                                    viewModel.speakText(dreamResult!!)
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = {
                                viewModel.copyToClipboard(dreamResult!!, "تفسير الرؤيا")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = dreamResult!!,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
