package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Request / Response Data Classes (Moshi compatible) ---

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Retrofit Network Client ---

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Executes content generation query on a background thread.
     */
    suspend fun chatWithCarbonSaathi(
        prompt: String,
        userProfileInfo: String,
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "API Key is empty! Launching smart local offline response engine.")
            return@withContext generateOfflineResponse(prompt, userProfileInfo)
        }

        val systemInstructionText = """
You are "Carbon Saathi" — an everyday witty, brutal, quirky but always respectful climate buddy from India.
Created by Anshika Nigam (@anshikamordhaar).

Current User Details:
$userProfileInfo

Core Persona Guidelines:
1. Speak in a personal, witty, retro-warm tone. Use Hindi, English, or a natural combination of both (Hinglish/code-mix).
2. Never give boring lectures. Offer "Gehri baatein" (deep reflections) about climate, but package them with sharp humor, Indian slang, and warm analogies.
3. Personalize your response based on the user's details. Adjust your examples, slang, and comparisons:
   - Students/Teens/PG (Ravi/Priya style): Use Hinglish, memes, quirky dares, fast slang, blunt truths.
   - Farmers (Sanjay style): Focus on farming terms, crop rotation, soil health, drip irrigation, avoiding crop burning (parali mudda), organic manure, input costs.
   - Corporate/Meera: Address fast delivery, carbon footprint from streaming, AC usage, metro vs cabs, single-use cups.
   - Seniors/Dadu: Respectful, nostalgic Hindi, referencing "hamare zamaane mein" (simple, natural sustainable living).
   - Kids/Aarav: Highly encouraging, simple words, hero-like energy, switching off lights.

Formatting Structure (MANDATORY):
To allow the mobile app's retro cards UI to render your answers chunk-by-chunk, ALWAYS structure your entire response using the following tags exactly as shown (with content in-between):

[SUMMARY]
<One-line witty, friendly, or brutal summary in Hinglish/Hindi or English matching the user's profile and query.>

[METRIC]
<Estimated CO2 Savings (e.g. '5.4 kg CO2 saved per day') or Money saved (e.g. '₹1,500 saved/month') in bold. Be creative but realistic!>

[ANALYSIS]
<A simple, witty analogy, short story, or comparison of their action to real-world objects. Make them realize the impact of small actions!>

[ACTION]
- Action 1: <Clear practical, step-by-step action step with potential CO2/financial impact>
- Action 2: <Alternative step>

[DEEP_DIVE]
<Optional brief science nugget for curious minds (geeks) explained in simple, engaging terms.>
        """.trimIndent()

        // Map Chat History to Gemini inputs
        val contents = mutableListOf<Content>()
        
        // Add previous history (maximum 6 turns to stay responsive and clean)
        history.takeLast(6).forEach { msg ->
            contents.add(
                Content(
                    parts = listOf(Part(text = msg.content))
                )
            )
        }

        // Add latest query
        contents.add(
            Content(
                parts = listOf(Part(text = prompt))
            )
        )

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
            generationConfig = GenerationConfig(
                temperature = 0.8f,
                maxOutputTokens = 1200
            )
        )

        try {
            val response = service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!responseText.isNullOrBlank()) {
                responseText
            } else {
                Log.e(TAG, "Empty response candidate. Switched to offline fallback.")
                generateOfflineResponse(prompt, userProfileInfo)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call. Switched to smart offline fallback.", e)
            generateOfflineResponse(prompt, userProfileInfo)
        }
    }

    /**
     * Generates a dynamic, highly-tailored rule-based offline response in actual Carbon Saathi structure.
     * Fully compatible with offline environments.
     */
    fun generateOfflineResponse(prompt: String, userProfileInfo: String): String {
        // Safe line matcher
        fun getValue(label: String): String {
            return userProfileInfo.lineSequence()
                .firstOrNull { it.contains(label, ignoreCase = true) }
                ?.substringAfter(":")?.trim() ?: ""
        }

        val name = getValue("Name").ifEmpty { "Saathi" }
        val role = getValue("Role")
        val language = getValue("Language Pref")
        val location = getValue("Location").ifEmpty { "Nearby Space" }

        val isFarmer = role.contains("Farmer", ignoreCase = true) || role.contains("Sanjay", ignoreCase = true)
        val isTeen = role.contains("Teen", ignoreCase = true) || role.contains("Student", ignoreCase = true)
        val isParent = role.contains("Parent", ignoreCase = true) || role.contains("Corporate", ignoreCase = true)
        val isSenior = role.contains("Senior", ignoreCase = true) || role.contains("Dadu", ignoreCase = true)
        val isKid = role.contains("Kid", ignoreCase = true) || role.contains("Aarav", ignoreCase = true)

        val lowerPrompt = prompt.lowercase()

        // 1. Keyword check: AC / COOLING / ENERGY / TEMP
        if (lowerPrompt.contains("ac") || lowerPrompt.contains("conditioner") || lowerPrompt.contains("cool") || lowerPrompt.contains("temp")) {
            val summary = when {
                isTeen -> "Arey $name dude, AC set to 26°C are pure legend vibe checks! Power and money bill are half!"
                isFarmer -> "Sanjay ji, ghar ke cooler aur pankhe ko saste me maintain karein. Diesel and pump electrical load directly bachta hai."
                isSenior -> "Dadu, neem tree ki thandi shadow is pure peace. Set drawing-room AC locked to 26°C with slow fans."
                isKid -> "Hey Climate Marshal $name, let's keep home AC default to 26 degrees! Antarctica rules don't apply to drawing rooms!"
                else -> "Locking AC sleep temperatures to 26°C is the ultimate sweet-spot for budget and emission cuts."
            }
            return """
                [SUMMARY]
                $summary (Local Off-Grid Companion Mode active)

                [METRIC]
                **Saved ~3.8 kg CO2 and ₹45 per night!**

                [ANALYSIS]
                An average compressor works continuously when AC is set to 18°C. Bumping it to 26°C reduces active grid load up to 24%. It's like preventing almost 2kg of heavy coal from burning at regional power grid stations!

                [ACTION]
                - Keep default indoor AC temperatures locked at 26°C with a cozy low fan speed
                - Seal bedroom window frames correctly to avoid cool air leakage losses

                [DEEP_DIVE]
                Every single degree rise above 18°C saves around 6% of active motor electricity. 26°C delivers the supreme thermodynamic balance of human health and grid conservation.
            """.trimIndent()
        }

        // 2. Keyword check: PARALI / BURN / CROP / FARMING / KHAD / COMPOST / SOIL
        if (lowerPrompt.contains("parali") || lowerPrompt.contains("burning") || lowerPrompt.contains("residue") || lowerPrompt.contains("compost") || lowerPrompt.contains("soil") || lowerPrompt.contains("khaad") || lowerPrompt.contains("urea") || lowerPrompt.contains("fertilizer") || lowerPrompt.contains("waste")) {
            val summary = when {
                isFarmer -> "Sanjay ji, Parali jalane se mitti ki upjau shakti dhuen me beh jaati hai. Organic compost banana hi kisan ki shaan hai!"
                isTeen -> "Yo $name! Burning crop residue is totally old-school and toxic. Mulching parali directly back into soil is real smart-compost tech."
                isSenior -> "Sanjay ji / Dadu, traditional compost pits and organic dehati gobar khaad are pure gold for our agricultural soil biology."
                else -> "Kitchen waste composting and crop residue mulching locks key atmospheric carbon back into the earth cycles."
            }
            return """
                [SUMMARY]
                $summary (Local Off-Grid Companion Mode active)

                [METRIC]
                **Saved ~10 kg CO2 per crop cycle and enriched soil biology.**

                [ANALYSIS]
                Burning stubble or cardboard boxes releases carbon monoxide, soot particles, and greenhouse gases into the air ecosystem. Mulching residue back keeps the organic soil carbon locked and holds high moisture.

                [ACTION]
                - Spray simple organic decomposer mixes to degrade paddy straws naturally
                - Start a simple three-tier wet peeling aerobic composter in your PG balcony or kitchen garden

                [DEEP_DIVE]
                One ton of raw agricultural paddy residue releases roughly 1,500kg of CO2 upon combustion, along with active particulate matter, while destroying nitrogen and phosphorus soil colonies.
            """.trimIndent()
        }

        // 3. Keyword check: METRO / commute / transport / scooty / ride / vehicle / carpool / tea / tapri / walk / distance
        if (lowerPrompt.contains("metro") || lowerPrompt.contains("commute") || lowerPrompt.contains("scooty") || lowerPrompt.contains("ride") || lowerPrompt.contains("cab") || lowerPrompt.contains("carpool") || lowerPrompt.contains("tea") || lowerPrompt.contains("tapri") || lowerPrompt.contains("walk") || lowerPrompt.contains("cycle") || lowerPrompt.contains("commute")) {
            val summary = when {
                isTeen -> "Walk to the corner tapri for tea, $name, instead of starting that double-riding scooty. Walk clears physical carbs too!"
                isFarmer -> "Mandiyoon me cycle ya paidal paidal rasta tay karna is smart diesel budget practice."
                isSenior -> "$name ji, paidal chalne se dil aur ghutno ke swasthya dono badhiya bane rehte hain, aur environment footprint zero!"
                isKid -> "$name Climate Hero, let's walk 1km to the school park with friends! Walking is our superpower!"
                else -> "Commuting via public transit like bus or metro saves cab cancellation anxieties and direct peak air emissions."
            }
            return """
                [SUMMARY]
                $summary (Local Off-Grid Companion Mode active)

                [METRIC]
                **Saved ~2.8 kg CO2 and ₹150 in localized fuels!**

                [ANALYSIS]
                Riding single occupant cabs increases urban congestion. By shifting to electric metro lines or localized transit systems, the net carbon distribution drops by up to 80% per commuter.

                [ACTION]
                - Use municipal bus networks or local shared metro rails for commutes under 10km
                - Walking for distances under 2.5km is healthy for both pocket balances and climate targets

                [DEEP_DIVE]
                High-capacity rapid electric lines consume only a fraction of the per-capita horsepower of custom motor vehicles, making rapid transit systems the ultimate green-shield for Indian metros.
            """.trimIndent()
        }

        // 4. Keyword check: LIGHT / POWER / SWITCH / BULB / CHARGER / FAN / Power grid / Electricity
        if (lowerPrompt.contains("light") || lowerPrompt.contains("bulb") || lowerPrompt.contains("charger") || lowerPrompt.contains("fan") || lowerPrompt.contains("power") || lowerPrompt.contains("electricity") || lowerPrompt.contains("grid") || lowerPrompt.contains("vampire")) {
            val summary = when {
                isTeen -> "Leaving phone chargers plugged 'ON' all night is vampire draw, $name. Unplug! Hostel grids have limits!"
                isKid -> "Power Marshal $name check: no empty rooms should have fans running! Turn switches off and collect points."
                isSenior -> "Dadu says: 'Hamare purane zamaane me hum swayam grid grid grids band karte the, aaj kal switches check karna aavashyak hai.'"
                else -> "Ghosts don't study under empty lights! Switch off unneeded bedroom bulbs and embrace carbon savings."
            }
            return """
                [SUMMARY]
                $summary (Local Off-Grid Companion Mode active)

                [METRIC]
                **Saved ~0.4 kg CO2 per day of active grid unplug.**

                [ANALYSIS]
                Leaving grids on for idle adapters creates 'vampire electricity loads'. Modern power grids generate energy heavily from thermal coal stations, making every wasted watt a direct coal emitter.

                [ACTION]
                - Flick wall switches OFF for laptop bricks once fully charged
                - Upgrade old filament bulbs with smart high-efficiency LED lights

                [DEEP_DIVE]
                Standby power losses contribute to roughly 1% of the aggregate global greenhouse gases, showing how miniature structural actions on home switches accumulate to global footprints.
            """.trimIndent()
        }

        // 5. Keyword check: WATER / TAP / SHOWER / BUCKET / DRIP / REUSE
        if (lowerPrompt.contains("water") || lowerPrompt.contains("tap") || lowerPrompt.contains("shower") || lowerPrompt.contains("bath") || lowerPrompt.contains("bucket") || lowerPrompt.contains("drip") || lowerPrompt.contains("pots") || lowerPrompt.contains("plant")) {
            val summary = when {
                isTeen -> "Bucket bath is the absolute alpha state, $name! Showers look good only in music videos."
                isFarmer -> "Sanjay ji, dhaan aur khet me open flood flooding mat kijiye. Micro drip irrigation se mitti ki nami bani rahegi."
                isSenior -> "Kitchen me pulse wash (dal-chawal dhone ka paani) paudho me daalna hum purane buzurgon ka amrit niyam hai."
                isKid -> "Shut the handwash tap tightly when brushing or soaping, buddy! Every micro drop is a magic ripple!"
                else -> "Municipal overhead pumping consumes high electricity. Saving fresh water directly minimizes localized coal usage."
            }
            return """
                [SUMMARY]
                $summary (Local Off-Grid Companion Mode active)

                [METRIC]
                **Saved ~0.8 kg CO2 and 40L sweet liquid per day!**

                [ANALYSIS]
                Groundwater tables are pumped to roofs using heavy dynamic motors. Shutting taps and utilizing bucket baths prevents deep motor draws, directly saving home power bills.

                [ACTION]
                - Adopt a single cooling bucket wash method instead of prolonged continuous showers
                - Reuse clean vegetable rinsing runoff water for balcony green plants

                [DEEP_DIVE]
                One horsepower electric pumps pull substantial current from national energy streams. Saving 100 litres of water preserves nearly 0.1 kWh of grid coal load.
            """.trimIndent()
        }

        // 6. Keyword check: PLASTIC / BAG / TOTE / PACKAG / BOX / CAN / TUMBLER
        if (lowerPrompt.contains("plastic") || lowerPrompt.contains("bag") || lowerPrompt.contains("tote") || lowerPrompt.contains("packaging") || lowerPrompt.contains("box") || lowerPrompt.contains("bottle") || lowerPrompt.contains("tumbler")) {
            val summary = when {
                isTeen -> "Arey PG PG me disposable bottles are so old-school. Eco-cool jute tote bags and steel tumblers are the trend now, $name!"
                isFarmer -> "Pesticide ke khali plastic dibbo ko khet khet me mat fainkiyen, gobar khaad jute bora upaj hi shaan hai."
                isSenior -> "$name ji, purane kapdo ki thailiyaan bazzar le jaana hum dadiyon ka classic model hai, plastic bags toxic hain."
                else -> "Single-use packing cardboard boxes are convenient but create excessive waste. Choose local sustainable carries!"
            }
            return """
                [SUMMARY]
                $summary (Local Off-Grid Companion Mode active)

                [METRIC]
                **Saved ~0.5 kg CO2 and ₹30 in packaging waste fees.**

                [ANALYSIS]
                PET single-use bottles take centuries to decompose, shedding micro-plastics into soil. Standard reusable cloth bags represent a pristine way to keep trash heaps empty.

                [ACTION]
                - Mark 'No plastic packaging cups/boxes' on food deliveries
                - Keep a personal stainless steel water bottle in your work/travel kit

                [DEEP_DIVE]
                Refining and molding raw petroleum into PET bottles outputs 8x the weight of the raw plastic in pure atmospheric gases before logistics even start.
            """.trimIndent()
        }

        // General fallback
        val defaultSummary = when {
            isTeen -> "Yo dude! Carbon Saathi is live offline. Let's trace saste, brutal and quirky eco tweaks for your hostel life, $name!"
            isFarmer -> "Sanjay ji, aapki mitti, kheti aur diesel ki bachat ke saste dehati tarike seekhne ke liye Carbon Saathi active hai!"
            isSenior -> "$name ji, namaste. Hamare paramparik sustainable habits—sitting in shade, reuse water, shut main switches—ko discuss kijiye."
            isKid -> "Welcome $name partner! Let's complete micro power duties, eat sweet fresh apples, and secure green hero badges!"
            else -> "Namaste, $name. I am Carbon Saathi, active offline to save your mobile data and guide your micro carbon savings!"
        }

        val defaultAnalysis = when {
            isTeen -> "Global climate talks can feel super boring, but your personal, micro choices—like walk commutes, unplugging adapters, and using steel cups—are highly punchy and direct."
            isFarmer -> "Rasayanik urea and dhuen se mitti bejaan aur sasti ho jaati hai. Mulching organic compost mitti ko surakshit aur kheti ameer karti hai."
            isSenior -> "Purani traditional legacy systems are the gold standards. Small daily water recycling and shade-sitting checks save the environment instantly."
            isKid -> "Big climate heroes don't need huge lab setups! Shutting idle running taps, light fans, and eating native fresh fruits builds premium green points."
            else -> "Carbon audits are easy when broken down into transport commute modes, AC room settings, and simple municipal water routines."
        }

        val defaultActionsText = when {
            isTeen -> """
                - Unplug phone/laptop bricks fully to stop vampire loads
                - Complete your personalized 'Teastall Walking' eco challenge on home tab!
            """.trimIndent()
            isFarmer -> """
                - Mix local organic gobar manure to maintain deep micro-living soil cells
                - Run drip line controls in noon instead of flooding fields with diesel grids
            """.trimIndent()
            isSenior -> """
                - Save kitchen pulse wash and vegetable waters for potted backgarden herbs
                - Move drawing room tea times to natural cool green neem/peepal tree shadows
            """.trimIndent()
            isKid -> """
                - Act as an Electricity Inspector: switch off empty room grid boards!
                - Reject packed plastic snacks for seasonal local sweet amrud / grapes
            """.trimIndent()
            else -> """
                - Reset bedroom sleep AC defaults to 26°C with low fans
                - Choose public bus networks or local shared metro lines for commutes under 5km
            """.trimIndent()
        }

        return """
            [SUMMARY]
            $defaultSummary

            [METRIC]
            **Aiming to save 50 kg CO2 / week locally without internet!**

            [ANALYSIS]
            $defaultAnalysis

            [ACTION]
            $defaultActionsText

            [DEEP_DIVE]
            Standard per-capita carbon levels in India are around 1.8 metric tons per year. By altering micro home habits, every small township or family can decrease aggregate emissions up to 20%.
        """.trimIndent()
    }
}

// --- Parsed Content helper ---

data class ParsedSaathiResponse(
    val summary: String = "",
    val metric: String = "",
    val analysis: String = "",
    val actions: List<String> = emptyList(),
    val deepDive: String = ""
) {
    companion object {
        fun parse(text: String): ParsedSaathiResponse {
            val summaryMarker = "[SUMMARY]"
            val metricMarker = "[METRIC]"
            val analysisMarker = "[ANALYSIS]"
            val actionMarker = "[ACTION]"
            val deepDiveMarker = "[DEEP_DIVE]"

            var summary = ""
            var metric = ""
            var analysis = ""
            var actionsText = ""
            var deepDive = ""

            val sections = listOf(summaryMarker, metricMarker, analysisMarker, actionMarker, deepDiveMarker)
            val lines = text.split("\n")
            var currentHeader = ""
            val contentBuilder = HashMap<String, StringBuilder>()
            sections.forEach { contentBuilder[it] = StringBuilder() }

            for (line in lines) {
                val trimmed = line.trim()
                if (sections.contains(trimmed)) {
                    currentHeader = trimmed
                } else if (currentHeader.isNotEmpty()) {
                    contentBuilder[currentHeader]?.append(line)?.append("\n")
                }
            }

            summary = contentBuilder[summaryMarker].toString().trim()
            metric = contentBuilder[metricMarker].toString().trim()
            analysis = contentBuilder[analysisMarker].toString().trim()
            actionsText = contentBuilder[actionMarker].toString().trim()
            deepDive = contentBuilder[deepDiveMarker].toString().trim()

            // If parsing failed to find tags, fall back nicely by putting everything in summary
            if (summary.isEmpty() && metric.isEmpty() && analysis.isEmpty() && actionsText.isEmpty()) {
                summary = text
            }

            // Parse actions list from markdown bullet points
            val actions = actionsText.split("\n")
                .map { it.trim().trimStart('-', '*', '•').trim() }
                .filter { it.isNotEmpty() }

            return ParsedSaathiResponse(
                summary = summary,
                metric = metric,
                analysis = analysis,
                actions = actions,
                deepDive = deepDive
            )
        }
    }
}
