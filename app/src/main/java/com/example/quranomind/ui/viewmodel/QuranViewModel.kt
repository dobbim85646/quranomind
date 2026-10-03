package com.example.quranomind.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quranomind.data.local.AppDatabase
import com.example.quranomind.data.local.FavoriteEntity
import com.example.quranomind.data.model.CosmicWord
import com.example.quranomind.data.model.InterpreterMode
import com.example.quranomind.data.model.ScienceVerse
import com.example.quranomind.data.model.SearchResult
import com.example.quranomind.data.model.SurahInfo
import com.example.quranomind.data.model.TafsirResult
import com.example.quranomind.data.repository.GeminiService
import com.example.quranomind.data.repository.QuranRepository
import com.example.quranomind.util.ArabicNormalizer
import com.example.quranomind.util.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val geminiService = GeminiService()
    private val favoriteDao = AppDatabase.getDatabase(application).favoriteDao()
    val ttsHelper = TextToSpeechHelper(application)

    // Surahs list
    val allSurahs: List<SurahInfo> = repository.getAllSurahs()

    // Quran Tafsir Screen State
    private val _selectedSurah = MutableStateFlow(allSurahs.firstOrNull() ?: SurahInfo(1, "الفاتحة", "Al-Fatihah", 7))
    val selectedSurah: StateFlow<SurahInfo> = _selectedSurah.asStateFlow()

    private val _selectedAyah = MutableStateFlow(1)
    val selectedAyah: StateFlow<Int> = _selectedAyah.asStateFlow()

    private val _selectedInterpreter = MutableStateFlow(InterpreterMode.MAISSAR)
    val selectedInterpreter: StateFlow<InterpreterMode> = _selectedInterpreter.asStateFlow()

    private val _isEnglish = MutableStateFlow(false)
    val isEnglish: StateFlow<Boolean> = _isEnglish.asStateFlow()

    private val _tafsirResult = MutableStateFlow<TafsirResult?>(null)
    val tafsirResult: StateFlow<TafsirResult?> = _tafsirResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isCurrentFavorited = MutableStateFlow(false)
    val isCurrentFavorited: StateFlow<Boolean> = _isCurrentFavorited.asStateFlow()

    // Favorites
    val favorites: StateFlow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cosmic & Scientific
    val cosmicWords: List<CosmicWord> = repository.getCosmicWordsList()
    val scienceVerses: List<ScienceVerse> = repository.getScienceVersesList()

    // Search Screen State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Dreams Screen State
    private val _dreamText = MutableStateFlow("")
    val dreamText: StateFlow<String> = _dreamText.asStateFlow()

    private val _gender = MutableStateFlow("ذكر")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _dreamInterpretation = MutableStateFlow<String?>(null)
    val dreamInterpretation: StateFlow<String?> = _dreamInterpretation.asStateFlow()

    private val _isDreamLoading = MutableStateFlow(false)
    val isDreamLoading: StateFlow<Boolean> = _isDreamLoading.asStateFlow()

    // Dark Mode Toggle
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        // Automatically fetch initial tafsir for Al-Fatihah, Ayah 1
        fetchTafsir()
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setLanguage(english: Boolean) {
        _isEnglish.value = english
        fetchTafsir()
    }

    fun selectSurah(surah: SurahInfo) {
        _selectedSurah.value = surah
        _selectedAyah.value = 1
        fetchTafsir()
    }

    fun selectAyah(ayah: Int) {
        val maxAyah = _selectedSurah.value.ayahCount
        _selectedAyah.value = ayah.coerceIn(1, maxAyah)
        fetchTafsir()
    }

    fun nextAyah() {
        if (_selectedAyah.value < _selectedSurah.value.ayahCount) {
            selectAyah(_selectedAyah.value + 1)
        } else if (_selectedSurah.value.number < 114) {
            val nextSurah = allSurahs.find { it.number == _selectedSurah.value.number + 1 }
            if (nextSurah != null) {
                selectSurah(nextSurah)
            }
        }
    }

    fun previousAyah() {
        if (_selectedAyah.value > 1) {
            selectAyah(_selectedAyah.value - 1)
        } else if (_selectedSurah.value.number > 1) {
            val prevSurah = allSurahs.find { it.number == _selectedSurah.value.number - 1 }
            if (prevSurah != null) {
                _selectedSurah.value = prevSurah
                _selectedAyah.value = prevSurah.ayahCount
                fetchTafsir()
            }
        }
    }

    fun selectInterpreter(interpreter: InterpreterMode) {
        _selectedInterpreter.value = interpreter
        fetchTafsir()
    }

    fun fetchTafsir() {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null
            val surah = _selectedSurah.value
            val ayah = _selectedAyah.value
            val interpreter = _selectedInterpreter.value
            val inEnglish = _isEnglish.value

            // 1. Get ayah text locally
            var ayahText = repository.getAyahText(surah.number, ayah)
            if (ayahText.isNullOrBlank()) {
                val fetchResult = geminiService.fetchAyahText(surah.number, surah.arabicName, ayah)
                ayahText = fetchResult.getOrNull() ?: "سورة ${surah.arabicName}، الآية $ayah"
            }

            var tafsirText: String? = null
            var isAi = false

            // Check if local Tafsir is requested and available
            if (interpreter == InterpreterMode.MAISSAR && !inEnglish) {
                tafsirText = repository.getLocalTafsir(surah.number, ayah)
            }

            // Fallback or specific interpreter via Gemini
            if (tafsirText.isNullOrBlank()) {
                isAi = true
                val result = geminiService.interpretAyah(
                    surahNumber = surah.number,
                    surahName = surah.arabicName,
                    ayahNumber = ayah,
                    ayahText = ayahText,
                    interpreterId = interpreter.id,
                    inEnglish = inEnglish
                )
                if (result.isSuccess) {
                    tafsirText = result.getOrNull()
                } else {
                    val localFallback = repository.getLocalTafsir(surah.number, ayah)
                    if (localFallback != null) {
                        tafsirText = localFallback
                        _statusMessage.value = "تم عرض التفسير المحلي لتعذر الاتصال بالذكاء الاصطناعي."
                    } else {
                        tafsirText = "عذراً، تعذر جلب التفسير حالياً. يرجى التحقق من اتصال الإنترنت أو مفتاح API."
                        _statusMessage.value = result.exceptionOrNull()?.message
                    }
                }
            }

            val finalTafsir = tafsirText ?: "لا يتوفر تفسير لهذه الآية حالياً."
            val hash = ArabicNormalizer.md5("${surah.number}-$ayah-$finalTafsir")

            _tafsirResult.value = TafsirResult(
                surahNumber = surah.number,
                surahName = surah.arabicName,
                ayahNumber = ayah,
                ayahText = ayahText,
                tafsirArabic = finalTafsir,
                tafsirEnglish = if (inEnglish) finalTafsir else null,
                interpreterId = interpreter.id,
                interpreterName = if (inEnglish) interpreter.titleEnglish else interpreter.titleArabic,
                hash = hash,
                isAiGenerated = isAi
            )

            // Check favorite status
            val favs = favorites.value
            _isCurrentFavorited.value = favs.any { it.hash == hash }

            _isLoading.value = false
        }
    }

    fun toggleFavoriteCurrent() {
        val result = _tafsirResult.value ?: return
        viewModelScope.launch {
            if (_isCurrentFavorited.value) {
                favoriteDao.deleteByHash(result.hash)
                _isCurrentFavorited.value = false
                _statusMessage.value = "تمت الإزالة من المفضلة"
            } else {
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        surahNumber = result.surahNumber,
                        surahName = result.surahName,
                        ayahNumber = result.ayahNumber,
                        ayahText = result.ayahText,
                        tafsir = result.tafsirArabic,
                        translated = result.tafsirEnglish,
                        interpreter = result.interpreterName,
                        lang = if (_isEnglish.value) "english" else "arabic",
                        hash = result.hash
                    )
                )
                _isCurrentFavorited.value = true
                _statusMessage.value = "تم الحفظ في المفضلة بنجاح ✨"
            }
        }
    }

    fun deleteFavorite(entity: FavoriteEntity) {
        viewModelScope.launch {
            favoriteDao.deleteFavorite(entity)
            if (_tafsirResult.value?.hash == entity.hash) {
                _isCurrentFavorited.value = false
            }
        }
    }

    fun copyToClipboard(text: String, label: String = "QuranoMind") {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        _statusMessage.value = "تم النسخ إلى الحافظة بنجاح"
    }

    fun speakText(text: String) {
        ttsHelper.speak(text, isEnglish = _isEnglish.value)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun performSearch() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _isSearching.value = true
            val localResults = repository.searchLocalTafasir(query)
            if (localResults.isNotEmpty()) {
                _searchResults.value = localResults
                _statusMessage.value = "تم العثور على ${localResults.size} نتيجة في التفاسير المتاحة"
            } else {
                // Fallback to AI thematic search
                val aiResult = geminiService.searchAiTopic(query, inEnglish = _isEnglish.value)
                if (aiResult.isSuccess) {
                    val content = aiResult.getOrNull() ?: ""
                    _searchResults.value = listOf(
                        SearchResult(
                            surahName = "بحث واستكشاف إسلامي",
                            surahNumber = "-",
                            ayahNumber = "-",
                            ayahText = "موضوع البحث: $query",
                            tafsir = content,
                            hash = ArabicNormalizer.md5(content),
                            isAiGenerated = true
                        )
                    )
                } else {
                    _searchResults.value = emptyList()
                    _statusMessage.value = "لم يتم العثور على نتائج للبحث."
                }
            }
            _isSearching.value = false
        }
    }

    fun setDreamText(text: String) {
        _dreamText.value = text
    }

    fun setGender(selectedGender: String) {
        _gender.value = selectedGender
    }

    fun interpretDream() {
        val text = _dreamText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _isDreamLoading.value = true
            val result = geminiService.interpretDream(
                dreamText = text,
                gender = _gender.value,
                inEnglish = _isEnglish.value
            )
            if (result.isSuccess) {
                _dreamInterpretation.value = result.getOrNull()
            } else {
                _dreamInterpretation.value = "عذراً، حدث خطأ أثناء معالجة الرؤيا: ${result.exceptionOrNull()?.message}"
            }
            _isDreamLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
