package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- Data representation of a Challenge option in UI ---
data class DailyEcoChallenge(
    val id: String,
    val title: String,
    val category: String,
    val co2Saved: Double,
    val points: Int,
    val description: String,
    val characterContext: String
)

class CarbonViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = CarbonRepository(database)

    // Reactive states
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trackingLogs: StateFlow<List<TrackingLog>> = repository.trackingLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedChallenges: StateFlow<List<ChallengeLog>> = repository.completedChallenges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Mock speech-to-text transcriptions based on segment to make mic useful!
    private val voiceTranscriptions = mapOf(
        "Teens (Ravi)" to listOf(
            "Arey Saathi, hostel me paani waste ho raha hai, skip bathing se carbon bachega kya?",
            "Metro vs scooty me kitna carbon bachta hai, jaldi hisab batao memes ke sath!",
            "Carbon check: College canteen me plastic cup ban karwane me doston ke sath kya dare karun?"
        ),
        "Parents (Meera)" to listOf(
            "Ac levels 26 pe chalane se kitna electricity bill aur carbon kam hoga?",
            "Fast food home deliveries se packaging carbon footprint kitna badhta hai?",
            "Can I plant a small kitchen herbal garden in my apartment balcony?"
        ),
        "Seniors (Dadu)" to listOf(
            "Hamare zamaane me neem ki dhao me baith kar dopahar guzaarte the, aaj AC ki aadat ho gayi hai. Isse kaise bachen?",
            "Purane kapdo ko reuse karne ka koi badhiya parampariq tarika bataiye.",
            "Water conservation ke hamare zamaane ke tarike aaj kaise kaam aa sakte hain?"
        ),
        "Farmers (Sanjay)" to listOf(
            "Parali jalaye bina mitti ko upjau kaise banayein, sasta tarika kya hai?",
            "Deere deere chemical urea ki jagah gobar khaat kaise dhyan se upyog karein?",
            "Saur urja (solar pumps) lagane me mitti aur diesel pump ka kharcha dono kaise bachega?"
        ),
        "Kids (Aarav)" to listOf(
            "Didi, class me sabko climate hero banane ke liye kya simple tips dun?",
            "Switching off light bulb everyday saves how much carbon?",
            "Mujhe plastic chip packet ke badle local amrud khane pe kya badge milega?"
        )
    )

    fun getRandomSpeechSimulation(ageGroup: String): String {
        val list = voiceTranscriptions[ageGroup] ?: listOf(
            "Saathi! Carbon saving ke real methods kya hain saste me?",
            "Ghar me kachra segregate karne se footprint kaise bachega?",
            "How to use simple kitchen composting and what is the exact step?"
        )
        return list.random()
    }

    // Role-tailored custom challenge builder
    fun getChallengesForRole(role: String): List<DailyEcoChallenge> {
        return when {
            role.contains("Teen", ignoreCase = true) || role.contains("Student", ignoreCase = true) -> listOf(
                DailyEcoChallenge(
                    "t1",
                    "Teastall Walking",
                    "Transport",
                    0.6,
                    15,
                    "Walk 2km to the tea tapri instead of taking double-ride scooty.",
                    "Ravi says: 'Arey back-seat heavy weights, pedestrian mode direct toggle karo!'"
                ),
                DailyEcoChallenge(
                    "t2",
                    "Hostel Unplug",
                    "Energy",
                    0.2,
                    10,
                    "Unplug your phone and laptop chargers once fully charged, no idle draw.",
                    "Priya says: 'Vampire draw is real, hostel room boards aren't unlimited resources!'"
                ),
                DailyEcoChallenge(
                    "t3",
                    "Bucket Bath Dare",
                    "Water",
                    0.8,
                    20,
                    "Use a single bucket of cool water instead of infinite running showers.",
                    "Ravi says: 'Showers are for movies, bucket bath is the real alpha state!'"
                )
            )
            role.contains("Farmer", ignoreCase = true) || role.contains("Sanjay", ignoreCase = true) -> listOf(
                DailyEcoChallenge(
                    "f1",
                    "Mulching over Parali",
                    "Farming",
                    10.0,
                    50,
                    "Incorporate crop residue/weeds directly in soil instead of burning.",
                    "Sanjay says: 'Mitti ki shakti jalegi toh dhandha kaise chalega, khad banao!'"
                ),
                DailyEcoChallenge(
                    "f2",
                    "Drip Pump Controller",
                    "Water",
                    4.5,
                    30,
                    "Run micro drip lines for 2 hours during noon instead of open diesel pump flooding.",
                    "Sanjay says: 'Diesel tank is not a water pipeline, save fuel and keep roots strong!'"
                ),
                DailyEcoChallenge(
                    "f3",
                    "Gobar Khaad Blend",
                    "Farming",
                    3.5,
                    25,
                    "Mix local organic manure instead of heavy dose of chemical urea.",
                    "Sanjay says: 'Local organic is pure gold for soil biology, keep chemistry low.'"
                )
            )
            role.contains("Corporate", ignoreCase = true) || role.contains("Parent", ignoreCase = true) || role.contains("Adult", ignoreCase = true) -> listOf(
                DailyEcoChallenge(
                    "c1",
                    "Default AC @ 26°C",
                    "Energy",
                    3.8,
                    20,
                    "Set your AC to 26 degrees and use a low fan for sleeping.",
                    "Meera says: 'You don't need Switzerland freezing inside while Delhi/Mumbai burns outside!'"
                ),
                DailyEcoChallenge(
                    "c2",
                    "Refuse Delivery Steam Box",
                    "Shopping",
                    0.5,
                    15,
                    "Reject extra plastic packaging boxes or steam cups in lunch delivery.",
                    "Meera says: 'Unboxing is satisfying but landfill space is fully booked!'"
                ),
                DailyEcoChallenge(
                    "c3",
                    "Metro / Walk commute",
                    "Transport",
                    2.8,
                    25,
                    "Ditch cab calling; walk to local metro or bus for distances under 3km.",
                    "Meera says: 'Saves cab cancellation anxiety and 2.8 kg CO2 directly!'"
                )
            )
            role.contains("Senior", ignoreCase = true) || role.contains("Dadu", ignoreCase = true) -> listOf(
                DailyEcoChallenge(
                    "s1",
                    "Neem Tree Shadow",
                    "Legacy",
                    0.8,
                    15,
                    "Sit in natural tree shade for tea time instead of closed AC drawing room.",
                    "Dadu says: 'Prakriti ki thandak me jo sukuun hai, wo machine me kahaan?'"
                ),
                DailyEcoChallenge(
                    "s2",
                    "Kitchen Water Reuse",
                    "Water",
                    0.6,
                    20,
                    "Collect kitchen pulse wash water and reuse it to water backgarden pots.",
                    "Dadu says: 'Ek boond paani ki bohot baadi dastaan hoti hai, saathi.'"
                ),
                DailyEcoChallenge(
                    "s3",
                    "Nostalgic Unplug",
                    "Energy",
                    0.3,
                    10,
                    "Switch off main wall grids at night of unused TV boxes and old radios.",
                    "Dadu says: 'Quiet hours mean total energy peace.'"
                )
            )
            else -> listOf( // Default kids or simple
                DailyEcoChallenge(
                    "k1",
                    "Switch-Off Inspector",
                    "Energy",
                    0.4,
                    15,
                    "Climate Hero Duty: Turn off lights and fans in empty rooms.",
                    "Aarav says: 'I am the family electricity marshal, no idle watts allowed!'"
                ),
                DailyEcoChallenge(
                    "k2",
                    "Local Fruit Snack",
                    "Food",
                    0.3,
                    10,
                    "Eat local seasonal fruits instead of packed plastic snack bags.",
                    "Aarav says: 'Local grapes/amrud are sweet, plastic chips are toxic!'"
                ),
                DailyEcoChallenge(
                    "k3",
                    "Tap Defender",
                    "Water",
                    0.5,
                    15,
                    "Shut tap fully during hand washing or toothbrushing rounds.",
                    "Aarav says: 'Every drop saved makes a small ocean ripple!'"
                )
            )
        }
    }

    /**
     * Initializes a new Profile (Onboarding)
     */
    fun setupUserProfile(
        name: String,
        ageGroup: String,
        location: String,
        role: String,
        language: String
    ) {
        viewModelScope.launch {
            val initialProfile = UserProfile(
                name = name,
                ageGroup = ageGroup,
                location = location,
                userRole = role,
                preferredLanguage = language,
                totalCo2Saved = 0.0,
                dailyStreak = 1,
                lastActionTimestamp = System.currentTimeMillis()
            )
            repository.saveProfile(initialProfile)

            // Auto-welcome query simulation
            repository.clearChatHistory()
            repository.insertChatMessage(
                ChatMessage(
                    role = "saathi",
                    content = "[SUMMARY]\nNamaste $name! Big welcome to Carbon Saathi! 🙏 I am your climate companion, ready to guide your day in simple language ($language) with custom $ageGroup tips.\n\n[METRIC]\n**Let's target saving 50 kg CO2 this week!**\n\n[ANALYSIS]\nLiving in $location can be carbon-heavy, but with simple, small changes—tea taps instead of cab trips, solar controllers, and conscious composting—we can make a huge impact without giving boring speeches.\n\n[ACTION]\n- Step 1: Browse your customized Daily Challenges on the Home tab!\n- Step 2: Track any of your daily tasks (like transport/AC levels) below.\n- Step 3: Type any query in English/Hindi/Hinglish to test my wit!"
                )
            )
        }
    }

    /**
     * Sends user message to Gemini and processes response
     */
    fun sendChatMessage(text: String, isVoice: Boolean = false) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val profile = userProfile.value ?: UserProfile(
                name = "Saathi",
                ageGroup = "Parents (Meera)",
                location = "Urban Space",
                preferredLanguage = "Hinglish",
                userRole = "Professional"
            )

            // Save user's question
            val userMsg = ChatMessage(role = "user", content = text, isVoice = isVoice)
            repository.insertChatMessage(userMsg)

            _isGenerating.value = true

            // Formulate user details
            val profileDesc = """
                Name: ${profile.name}
                Segment/Age: ${profile.ageGroup}
                Location: ${profile.location}
                Language Pref: ${profile.preferredLanguage}
                Role: ${profile.userRole}
                Current CO2 Saved: ${profile.totalCo2Saved} kg
                Current Streak: ${profile.dailyStreak} days
            """.trimIndent()

            val currentHistory = chatMessages.value

            // Call Gemini
            val saathiReply = GeminiClient.chatWithCarbonSaathi(
                prompt = text,
                userProfileInfo = profileDesc,
                history = currentHistory
            )

            // If reply has actionable parsed metric values, let's extract them to award point boosts
            val parsed = ParsedSaathiResponse.parse(saathiReply)
            
            // Save model response
            val saathiMsg = ChatMessage(role = "saathi", content = saathiReply)
            repository.insertChatMessage(saathiMsg)

            _isGenerating.value = false

            // Update streak if active chat day
            repository.updateStreakIfNeeded(profile)
        }
    }

    /**
     * Records a carbon tracking activity
     */
    fun trackAction(category: String, activity: String, quantity: Double, co2Saved: Double) {
        val pointsEarned = (co2Saved * 10).toInt().coerceAtLeast(5)
        viewModelScope.launch {
            repository.recordCo2Saved(co2Saved, pointsEarned, category, activity, quantity)
            
            // Insert confirmation message directly to Chat
            val logMessage = "Logged Eco Activity: I $activity for $quantity units and saved $co2Saved kg of CO2 directly! 🌿 (+ $pointsEarned Points)"
            repository.insertChatMessage(
                ChatMessage(
                    role = "user",
                    content = logMessage
                )
            )

            val triggerResponseText = "[SUMMARY]\nShabash! Awesome tracking, Saathi! You saved $co2Saved kg of CO2 directly with '$activity'.\n\n[METRIC]\n**Carbon Saved: $co2Saved kg | Points: +$pointsEarned**\n\n[ANALYSIS]\nThis small choice is like saving standard electricity units. If every Indian in your segment does this once a week, we can prevent gigatons of heavy coal from burning! Gehri baat, but pure maths.\n\n[ACTION]\n- Carry on keeping the streak alive!\n- Track more transport or agricultural logs when they happen."
            repository.insertChatMessage(
                ChatMessage(
                    role = "saathi",
                    content = triggerResponseText
                )
            )

            // Update streak
            userProfile.value?.let {
                repository.updateStreakIfNeeded(it)
            }
        }
    }

    /**
     * Marks a custom challenge card as completed
     */
    fun completeDailyChallenge(challenge: DailyEcoChallenge) {
        viewModelScope.launch {
            // Check if already completed to avoid double award
            val alreadyDone = completedChallenges.value.any { it.challengeId == challenge.id }
            if (!alreadyDone) {
                repository.completeChallenge(
                    challengeId = challenge.id,
                    title = challenge.title,
                    category = challenge.category,
                    co2Saved = challenge.co2Saved
                )

                // Log into chat for visual progression feedback
                val alertUser = "Done Daily Challenge: Completed '${challenge.title}'! Saved ${challenge.co2Saved} kg of CO2."
                repository.insertChatMessage(
                    ChatMessage(role = "user", content = alertUser)
                )

                val replyText = "[SUMMARY]\nWah! Daily challenge fully executed: ${challenge.title}. You are proving to be a real eco champion.\n\n[METRIC]\n**Saved ${challenge.co2Saved} kg CO2 | Earned ${challenge.points} Points**\n\n[ANALYSIS]\n${challenge.category} challenge completed! By taking small practical steps instead of just debating global warming, you are showing us the perfect root-impact method. Keeping it quirky and highly progressive!\n\n[ACTION]\n- Try out other daily eco actions\n- Ask me a carbon doubt if you want more ideas!"
                repository.insertChatMessage(
                    ChatMessage(role = "saathi", content = replyText)
                )

                // Update streak
                userProfile.value?.let {
                    repository.updateStreakIfNeeded(it)
                }
            }
        }
    }

    fun resetChallenges() {
        viewModelScope.launch {
            repository.clearAllChallenges()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }
}
