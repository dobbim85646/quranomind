package com.example.quranomind.data.repository

import com.example.quranomind.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiService {

    private val model = "gemini-3.5-flash"
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"

    suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("لم يتم العثور على مفتاح Gemini API. يرجى التحقق من إعدادات المفاتيح.")
            )
        }

        try {
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-goog-api-key", apiKey)
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }

            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val partObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(partObj)
                }
                put("contents", contents)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

            if (responseCode !in 200..299) {
                val errorMsg = try {
                    JSONObject(responseText).getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    "HTTP $responseCode: $responseText"
                }
                return@withContext Result.failure(Exception("خطأ في الاتصال بالذكاء الاصطناعي: $errorMsg"))
            }

            val json = JSONObject(responseText)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val content = candidates.getJSONObject(0).optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    return@withContext Result.success(text)
                }
            }
            Result.failure(Exception("لم يُرجع الذكاء الاصطناعي استجابة صالحة"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAyahText(surahNumber: Int, surahName: String, ayahNumber: Int): Result<String> {
        val prompt = "أعد النص القرآني الدقيق للآية رقم $ayahNumber من سورة $surahName (رقم $surahNumber) بالرسم العثماني أو الإملائي المضبوط بالشكل فقط، بدون أي مقدمات أو هوامش أو تفسير."
        return generateContent(prompt)
    }

    suspend fun interpretAyah(
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int,
        ayahText: String,
        interpreterId: String,
        inEnglish: Boolean
    ): Result<String> {
        val basePrompt = when (interpreterId) {
            "maissar" -> "قدم تفسيراً موجزاً وواضحاً وسهل الفهم على طريقة التفسير الميسر للآية الكريمة: \"$ayahText\" من سورة $surahName (الآية $ayahNumber)."
            "ibn_kathir" -> "تفسير الآية الكريمة: \"$ayahText\" من سورة $surahName (الآية $ayahNumber) استناداً إلى منهج تفسير القرآن العظيم للإمام ابن كثير مع بيان سبب النزول إن وجد والآثار المروية."
            "qurtubi" -> "تفسير الآية الكريمة: \"$ayahText\" من سورة $surahName (الآية $ayahNumber) وفق تفسير الجامع لأحكام القرآن للإمام القرطبي مع بيان الأحكام واللطائف اللغوية."
            "saadi" -> "تفسير الآية الكريمة: \"$ayahText\" من سورة $surahName (الآية $ayahNumber) بأسلوب تفسير تيسير الكريم الرحمن للشيخ عبد الرحمن السعدي مع إبراز المعاني التربوية والإيمانية."
            "all" -> "مقارنة منهجية مركزة بين تفاسير (ابن كثير، القرطبي، والسعدي) للآية الكريمة: \"$ayahText\" من سورة $surahName (الآية $ayahNumber)، مع إبراز زاوية كل مفسر وخلاصة مشتركة."
            else -> "تفسير شامل وميسر مع تدبر إيماني ودروس مستفادة للآية الكريمة: \"$ayahText\" من سورة $surahName (الآية $ayahNumber)."
        }

        val prompt = if (inEnglish) {
            "$basePrompt\n\nProvide the final explanation in English, with accurate theological translation and clear, elegant phrasing for English readers."
        } else {
            basePrompt
        }

        return generateContent(prompt)
    }

    suspend fun interpretDream(dreamText: String, gender: String, inEnglish: Boolean): Result<String> {
        val langInstruction = if (inEnglish) "Provide the explanation in English." else "قدم التفسير باللغة العربية بأسلوب وقور وطمأنة."
        val prompt = """
            أنا ($gender). حلمت بالآتي:
            "$dreamText"
            
            يرجى تقديم تفسير وتحليل شرعي وتربوي لهذا المنام استناداً إلى آداب وقواعد تفسير الرؤى في الإسلام (مثل ما ورد عن ابن سيرين والنابلسي والمحققين)، مع مراعاة الآتي:
            1. التذكير بأن علم الرؤى ظني واستئناسي ولا يُبنى عليه أحكام قطعية.
            2. استخراج الرموز الأساسية ودلالاتها المبشرة أو الإرشادية.
            3. تقديم نصيحة وتوجيه إيماني ودعاء مناسب.
            $langInstruction
        """.trimIndent()
        return generateContent(prompt)
    }

    suspend fun searchAiTopic(query: String, inEnglish: Boolean): Result<String> {
        val langInstruction = if (inEnglish) "Write the response in English." else "اكتب الإجابة باللغة العربية الفصحى."
        val prompt = """
            قدم معلومات إسلامية شاملة وموثقة حول موضوع: '$query'.
            استشهد بالآيات القرآنية الكريمة والأحاديث النبوية الصحيحة ذات الصلة مع شرح موجز لمعانيها، والدروس العملية للمسلم في حياته اليومية.
            $langInstruction
        """.trimIndent()
        return generateContent(prompt)
    }
}
