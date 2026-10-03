package com.example.quranomind.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quranomind.R
import com.example.quranomind.ui.theme.GoldAccent
import com.example.quranomind.ui.theme.NavyPrimary
import com.example.quranomind.ui.viewmodel.QuranViewModel

sealed class AppTab(val index: Int, val titleAr: String, val titleEn: String, val icon: ImageVector) {
    object Tafsir : AppTab(0, "التفسير", "Tafsir", Icons.Default.MenuBook)
    object Science : AppTab(1, "الكون والعلم", "Science", Icons.Default.Science)
    object Dreams : AppTab(2, "الرؤى", "Dreams", Icons.Default.NightsStay)
    object Search : AppTab(3, "البحث", "Search", Icons.Default.Search)
    object Favorites : AppTab(4, "المفضلة", "Favorites", Icons.Default.Favorite)
    object Surahs : AppTab(5, "السور", "Surahs", Icons.Default.FormatListNumbered)

    companion object {
        val all = listOf(Tafsir, Science, Dreams, Search, Favorites, Surahs)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: QuranViewModel) {
    var currentTab by remember { mutableIntStateOf(0) }
    val isEnglish by viewModel.isEnglish.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GoldAccent,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "QuranoMind",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = if (isEnglish) "Quran Tafsir & Cosmic Science" else "المصباح الوهّاج لتفسير القرآن",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Button
                    IconButton(
                        onClick = { viewModel.setLanguage(!isEnglish) },
                        modifier = Modifier.testTag("lang_toggle_button")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = if (isEnglish) "عربي" else "EN",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Dark Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = GoldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppTab.all.forEach { tab ->
                    val isSelected = currentTab == tab.index
                    val label = if (isEnglish) tab.titleEn else tab.titleAr

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab.index },
                        icon = {
                            if (tab == AppTab.Favorites && favorites.isNotEmpty()) {
                                BadgedBox(badge = {
                                    Badge { Text(favorites.size.toString()) }
                                }) {
                                    Icon(imageVector = tab.icon, contentDescription = label)
                                }
                            } else {
                                Icon(imageVector = tab.icon, contentDescription = label)
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.titleEn.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> TafsirScreen(viewModel = viewModel)
                1 -> ScienceScreen(viewModel = viewModel)
                2 -> DreamScreen(viewModel = viewModel)
                3 -> SearchScreen(viewModel = viewModel)
                4 -> FavoritesScreen(viewModel = viewModel)
                5 -> SurahListScreen(
                    viewModel = viewModel,
                    onSurahClick = { surah ->
                        viewModel.selectSurah(surah)
                        currentTab = 0
                    }
                )
            }
        }
    }
}
