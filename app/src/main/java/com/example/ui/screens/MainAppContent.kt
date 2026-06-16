package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.data.ParsedSaathiResponse
import com.example.data.UserProfile
import com.example.ui.theme.*
import com.example.viewmodel.CarbonViewModel
import com.example.viewmodel.DailyEcoChallenge
import kotlinx.coroutines.launch

// --- Data Representation of Avatar Card Sheet ---
data class AvatarPreset(
    val id: String,
    val name: String,
    val age: Int,
    val location: String,
    val role: String,
    val initial: String,
    val bg: Color,
    val toneDesc: String,
    val exampleChallenge: String
)

val avatarPresets = listOf(
    AvatarPreset("ravi", "Ravi", 19, "Tier-2 City Hostel", "Teens (Ravi)", "RV", WarmRetroCoral, "Hinglish, hostel memes, witty and slightly brutal", "Walk 2km for tea instead of scooty double-riding"),
    AvatarPreset("kusum", "Kusum Devi", 45, "Bihar Belt Village", "Seniors (Dadu)", "KD", SunGlowYellow, "Simple Hindi, local words, home economics", "Reuse kitchen pulse wash to water backyard plants"),
    AvatarPreset("meera", "Meera", 32, "Metro Apartment", "Parents (Meera)", "MR", LeafGreen, "Polished Hinglish, sharp corporate humor, AC savings", "Set bedroom AC default to 26°C with low fan"),
    AvatarPreset("aarav", "Aarav", 10, "School Student", "Kids (Aarav)", "AV", DeepSaathiTeal, "Simple English-Hindi mix, highly curious, light inspector", "Turn off empty room lights and close toothbrush taps"),
    AvatarPreset("dadu", "Dadu", 65, "Small Town Garden", "Seniors (Dadu)", "DD", Color(0xFFE2E8F0), "Formal sweet Hindi, stories from hamare zamaane mein", "Sit in natural tree shade for tea instead of power drawing room"),
    AvatarPreset("sanjay", "Sanjay", 40, "Agriculture UP", "Farmers (Sanjay)", "SJ", DeepSaathiTeal, "Hindi with farming terms, sasta crop mulch helper", "Mulch weed residue directly into soil instead of parali burning"),
    AvatarPreset("priya", "Priya", 24, "Urban PG Hosteler", "Teens (Ravi)", "PY", WarmRetroCoral, "Fast Hinglish, trendy sustainability tote bags", "Unplug charger vampire draws from PG socket boards")
)

@Composable
fun MainAppContent(viewModel: CarbonViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val trackingLogs by viewModel.trackingLogs.collectAsStateWithLifecycle()
    val completedChallenges by viewModel.completedChallenges.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    if (userProfile == null) {
        OnboardingScreen(onOnboardComplete = { name, ageGroup, location, role, lang ->
            viewModel.setupUserProfile(name, ageGroup, location, role, lang)
        })
    } else {
        val profile = userProfile!!
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = SoftSand,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("app_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home", modifier = Modifier.size(26.dp)) },
                        label = { Text("Saathi Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = DeepSaathiTeal,
                            indicatorColor = DeepSaathiTeal,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        ),
                        modifier = Modifier.testTag("nav_item_home")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(badge = {
                                if (isGenerating) {
                                    Badge(containerColor = WarmRetroCoral) {
                                        Text("...", color = Color.White)
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(26.dp))
                            }
                        },
                        label = { Text("Chat Guide", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = DeepSaathiTeal,
                            indicatorColor = DeepSaathiTeal,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        ),
                        modifier = Modifier.testTag("nav_item_chat")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Filled.History, contentDescription = "Log", modifier = Modifier.size(26.dp)) },
                        label = { Text("Impact Log", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = DeepSaathiTeal,
                            indicatorColor = DeepSaathiTeal,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        ),
                        modifier = Modifier.testTag("nav_item_logs")
                    )
                }
            },
            containerColor = SoftSand,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        profile = profile,
                        viewModel = viewModel,
                        completedChallenges = completedChallenges.map { it.challengeId }.toSet(),
                        onNavigateToChat = { selectedTab = 1 }
                    )
                    1 -> ChatScreen(
                        profile = profile,
                        messages = chatMessages,
                        isGenerating = isGenerating,
                        onSendMessage = { text, isVoice -> viewModel.sendChatMessage(text, isVoice) },
                        onQuickChallengeAccepted = { challenge ->
                            viewModel.completeDailyChallenge(challenge)
                            Toast.makeText(context, "${challenge.title} Accepted & Completed!", Toast.LENGTH_SHORT).show()
                        },
                        onClearHistory = { viewModel.clearChatHistory() },
                        viewModel = viewModel
                    )
                    2 -> ImpactLogScreen(
                        profile = profile,
                        logs = trackingLogs,
                        completedCount = completedChallenges.size,
                        onResetStats = {
                            viewModel.resetChallenges()
                            Toast.makeText(context, "Challenges reset successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

// --- ONBOARDING SCREEN ---
@Composable
fun OnboardingScreen(
    onOnboardComplete: (name: String, ageGroup: String, location: String, role: String, lang: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(avatarPresets.first()) }
    var language by remember { mutableStateOf("Hinglish") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Custom Retro Banner Drawing
            Canvas(
                modifier = Modifier
                    .size(110.dp)
                    .drawBehind {
                        // Shadow offset retro
                        drawCircle(
                            color = WarmRetroCoral,
                            radius = size.minDimension / 2.3f,
                            center = center + Offset(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
            ) {
                // Green organic boundary
                drawCircle(
                    color = DeepSaathiTeal,
                    radius = size.minDimension / 2.3f
                )

                // Handshake leaf inner details
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.25f)
                    cubicTo(
                        size.width * 0.2f, size.height * 0.45f,
                        size.width * 0.25f, size.height * 0.75f,
                        size.width * 0.5f, size.height * 0.85f
                    )
                    cubicTo(
                        size.width * 0.75f, size.height * 0.75f,
                        size.width * 0.8f, size.height * 0.45f,
                        size.width * 0.5f, size.height * 0.25f
                    )
                }
                drawPath(path = path, color = LeafGreen)
                // Center shake bar
                drawLine(
                    color = Color.White,
                    start = Offset(size.width * 0.42f, size.height * 0.58f),
                    end = Offset(size.width * 0.58f, size.height * 0.58f),
                    strokeWidth = 3.dp.toPx()
                )
                // Sun rays dot
                drawCircle(color = SunGlowYellow, radius = 5.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.42f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Carbon Saathi",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                color = DeepSaathiTeal,
                textAlign = TextAlign.Center
            )

            Text(
                text = "har roz ke chhote steps, bada climate impact.",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = WarmRetroCoral,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Enter Name Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Aapka Naam / Your Name:",
                        fontWeight = FontWeight.Bold,
                        color = DeepSaathiTeal,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("E.g., Anshika, Ramesh, Ravi...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepSaathiTeal,
                            unfocusedBorderColor = MutedSlate
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )
                }
            }

            // Choose Character Style Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Select Avatar (Match your role!):",
                        fontWeight = FontWeight.Bold,
                        color = DeepSaathiTeal,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "We customize your daily challenges & Saathi's voice tone accordingly.",
                        fontSize = 12.sp,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Avatar scroll list
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp)
                    ) {
                        avatarPresets.forEach { preset ->
                            val isSelected = selectedAvatar.id == preset.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .clickable { selectedAvatar = preset }
                                    .width(85.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(preset.bg)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) DeepSaathiTeal else Color.Transparent,
                                            shape = CircleShape
                                        )
                                ) {
                                    Text(
                                        text = preset.initial,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp,
                                        color = if (preset.bg == Color(0xFFE2E8F0)) DeepSaathiTeal else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) DeepSaathiTeal else MutedSlate,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Age: ${preset.age}",
                                    fontSize = 10.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description of Selected Avatar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftSand)
                            .border(1.dp, DeepSaathiTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Selected Vibe: ${selectedAvatar.role}",
                                fontWeight = FontWeight.Bold,
                                color = WarmRetroCoral,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "🗣️ ${selectedAvatar.toneDesc}",
                                fontSize = 12.sp,
                                color = DeepSaathiTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📍 Vibe Check location: ${selectedAvatar.location}",
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🎯 Sample challenge: '${selectedAvatar.exampleChallenge}'",
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                        }
                    }
                }
            }

            // Language Vibe Check Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Language / Boli:",
                        fontWeight = FontWeight.Bold,
                        color = DeepSaathiTeal,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Hinglish", "Hindi", "English").forEach { lang ->
                            val isSel = language == lang
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) DeepSaathiTeal else SoftSand)
                                    .clickable { language = lang }
                                    .border(1.dp, DeepSaathiTeal, RoundedCornerShape(20.dp))
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = lang,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else DeepSaathiTeal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Start Button with Thick Retro Drop Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clickable(enabled = name.isNotBlank()) {
                        if (name.isNotBlank()) {
                            onOnboardComplete(
                                name,
                                selectedAvatar.role,
                                selectedAvatar.location,
                                selectedAvatar.role,
                                language
                            )
                        }
                    }
                    .testTag("onboarding_start_button")
            ) {
                // Background Shadow Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (name.isNotBlank()) WarmRetroCoral else MutedSlate.copy(alpha = 0.5f))
                )
                // Foreground Action Card
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (name.isNotBlank()) DeepSaathiTeal else MutedSlate)
                        .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "Chalo Shuru Karein! 🚀",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// --- HOME SCREEN (CO2 METRICS, DAILY CHALLENGES & MANUAL TRACKING) ---
@Composable
fun HomeScreen(
    profile: UserProfile,
    viewModel: CarbonViewModel,
    completedChallenges: Set<String>,
    onNavigateToChat: () -> Unit
) {
    val challenges = remember(profile.userRole) {
        viewModel.getChallengesForRole(profile.userRole)
    }

    val scrollState = rememberScrollState()

    // Temporary Manual Tracking UI states
    var trackingCategory by remember { mutableStateOf("Transport") }
    var selectedActivity by remember { mutableStateOf("Eco Metro Ride") }
    var quantitySliderValue by remember { mutableFloatStateOf(5f) }

    val activityOptions = remember(trackingCategory) {
        when (trackingCategory) {
            "Transport" -> listOf("Eco Metro Ride", "Local Non-AC Bus", "Cycling/Walking Tapri ride", "Carpooling Uber/Ola")
            "Energy" -> listOf("LED Bulbs switched off", "AC switched to 26°C", "Solar pump active hours", "Unplugged TV standby logs")
            "Food" -> listOf("Local farm shopping purchase", "Skip packaging plastic boxes", "Home-made clay tea container", "Seasonal veg meal choice")
            "Farming" -> listOf("Weed residue composting", "Micro drip irrigation hours", "Organic cowdung manure compost", "Skip pesticide chemicals")
            else -> listOf("Tote shopping bag reuse", "Close idle running water taps")
        }
    }

    LaunchedEffect(trackingCategory) {
        selectedActivity = activityOptions.first()
    }

    val computedCo2Saved = remember(quantitySliderValue, selectedActivity) {
        // Simple mock formulas to calculate carbon savings realistically
        val multiplier = when (selectedActivity) {
            "Eco Metro Ride" -> 0.15 // kg per km
            "Local Non-AC Bus" -> 0.12
            "Cycling/Walking Tapri ride" -> 0.22
            "Carpooling Uber/Ola" -> 0.08
            "LED Bulbs switched off" -> 0.05 // kg per hour
            "AC switched to 26°C" -> 0.65
            "Solar pump active hours" -> 1.5
            "Weed residue composting" -> 2.5 // kg per cycle
            "Micro drip irrigation hours" -> 2.0
            "Organic cowdung manure compost" -> 1.8
            else -> 0.1 // miscellaneous
        }
        (quantitySliderValue * multiplier).coerceAtLeast(0.1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- 1. USER PROFILE PROFILE GREETING & WEATHER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(WarmRetroCoral)
                    .border(2.dp, DeepSaathiTeal, CircleShape)
            ) {
                // Namaste icon or initial
                Text(
                    text = "🙏",
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Namaste, ${profile.name}! 👋",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = DeepSaathiTeal
                )
                Text(
                    text = "Vibe: ${profile.userRole} | 📍 ${profile.location}",
                    fontSize = 12.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.Bold
                )
            }

            // Simulated Retro Sun / Weather Widget
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SunGlowYellow.copy(alpha = 0.2f))
                    .border(1.dp, SunGlowYellow, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.WbSunny,
                        contentDescription = "Sun Alert",
                        tint = WarmRetroCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "38°C dry • Tips optimized",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSaathiTeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. IMPACT OVERVIEW STATS (STREAK, POINTS, CO2 REDUCTION) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // CO2 Card
            Card(
                colors = CardDefaults.cardColors(containerColor = LeafGreen.copy(alpha = 0.15f)),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.3f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Eco, contentDescription = "CO2", tint = DeepSaathiTeal, modifier = Modifier.size(28.dp))
                    Text(
                        text = String.format("%.2f kg", profile.totalCo2Saved),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = DeepSaathiTeal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "CO2 Saved (Real)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Streak Card
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmRetroCoral.copy(alpha = 0.15f)),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Streak", tint = WarmRetroCoral, modifier = Modifier.size(28.dp))
                    Text(
                        text = "${profile.dailyStreak} Days",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = WarmRetroCoral,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Daily Streak",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Points Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SunGlowYellow.copy(alpha = 0.15f)),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "Points", tint = SunGlowYellow, modifier = Modifier.size(28.dp))
                    Text(
                        text = "${profile.currentPoints} pts",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = DeepSaathiTeal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Karma Pts",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. CUSTOM DAILY ECO CHALLENGES ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, DeepSaathiTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Daily Eco Challenges",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = DeepSaathiTeal
                        )
                        Text(
                            text = "Specially optimized for your segment",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                    }

                    // Small Fire tracker
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarmRetroCoral)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+15 Pts Each",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                challenges.forEach { challenge ->
                    val isDone = completedChallenges.contains(challenge.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDone) LeafGreen.copy(alpha = 0.15f) else SoftSand)
                            .border(
                                1.dp,
                                if (isDone) LeafGreen else DeepSaathiTeal.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isDone) {
                                viewModel.completeDailyChallenge(challenge)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.FiberManualRecord,
                            contentDescription = "Status",
                            tint = if (isDone) LeafGreen else MutedSlate,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = challenge.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DeepSaathiTeal
                            )
                            Text(
                                text = challenge.description,
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = challenge.characterContext,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmRetroCoral
                            )
                        }
                        // Budget savings circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DeepSaathiTeal)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "-${challenge.co2Saved} kg",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 4. MANUAL CARBON TRACKER TOOL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, DeepSaathiTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bolo Saathi, Quick Tracker",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = DeepSaathiTeal
                )
                Text(
                    text = "Log your instant carbon reduction activities to claim points!",
                    fontSize = 11.sp,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable category select chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Transport", "Energy", "Food", "Farming").forEach { cat ->
                        // Don't show farming if selected user role is kids, stay custom!
                        if (cat == "Farming" && (profile.userRole.contains("Kids") || profile.userRole.contains("Teens"))) return@forEach

                        val isSel = trackingCategory == cat
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) DeepSaathiTeal else SoftSand)
                                .clickable { trackingCategory = cat }
                                .border(1.dp, DeepSaathiTeal, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else DeepSaathiTeal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Activity Spinner Selection Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftSand)
                        .border(1.dp, DeepSaathiTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Selected Eco Practice:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Vertical column mimicking spinner of activity options
                        activityOptions.forEach { opt ->
                            val isChosen = selectedActivity == opt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedActivity = opt }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isChosen,
                                    onClick = { selectedActivity = opt },
                                    colors = RadioButtonDefaults.colors(selectedColor = DeepSaathiTeal)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = opt,
                                    fontSize = 12.sp,
                                    color = DeepSaathiTeal,
                                    fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity Slider
                val isFarming = trackingCategory == "Farming"
                val sliderMin = 1f
                val sliderMax = if (isFarming) 10f else 30f
                val sliderLabel = if (isFarming) "Acres / Cycles / Tons" else "Distance (km) / Time (Hours)"

                Text(
                    text = "$sliderLabel: ${quantitySliderValue.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = DeepSaathiTeal
                )
                Slider(
                    value = quantitySliderValue,
                    onValueChange = { quantitySliderValue = it },
                    valueRange = sliderMin..sliderMax,
                    colors = SliderDefaults.colors(
                        thumbColor = WarmRetroCoral,
                        activeTrackColor = DeepSaathiTeal,
                        inactiveTrackColor = SoftSand
                    ),
                    modifier = Modifier.testTag("carbon_tracker_slider")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dynamic saved display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarmRetroCoral.copy(alpha = 0.15f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Net CO2 Saved:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSaathiTeal
                    )
                    Text(
                        text = String.format("%.2f kg CO2", computedCo2Saved),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WarmRetroCoral
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.trackAction(
                            category = trackingCategory,
                            activity = selectedActivity,
                            quantity = quantitySliderValue.toDouble(),
                            co2Saved = computedCo2Saved
                        )
                        onNavigateToChat() // navigate directly to Chat tab to see interactive analysis
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSaathiTeal),
                    shape = RoundedCornerShape(8.getAbsoluteValue()?.let { 8.dp } ?: 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Color.Black, RoundedCornerShape(8.dp))
                        .testTag("submit_tracking_button")
                ) {
                    Text(
                        text = "Track Step & Ask Buddy 🌿",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// Simple Helper Extension to get absolute value
fun Int.getAbsoluteValue() = java.lang.Math.abs(this)

// --- CHAT SCREEN (CONVERSATIONAL RETRO BUBBLES AND STRUCTURAL RENDER CARDS) ---
@Composable
fun ChatScreen(
    profile: UserProfile,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    onSendMessage: (String, Boolean) -> Unit,
    onQuickChallengeAccepted: (DailyEcoChallenge) -> Unit,
    onClearHistory: () -> Unit,
    viewModel: CarbonViewModel
) {
    val context = LocalContext.current
    var rawInputText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom whenever messages list grows
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                lazyListState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val quickDoubts = remember(profile.userRole) {
        when {
            profile.userRole.contains("Farmer", ignoreCase = true) -> listOf(
                "Parali compost karne se mitti me phosphorus kaise badhta hai?",
                "Organic gobar khaad vs chemical fertilizers me sasta kya hai?",
                "Drip irrigation aur multi-cropping se yield kitna badhta hai?"
            )
            profile.userRole.contains("Teen", ignoreCase = true) || profile.userRole.contains("Student", ignoreCase = true) -> listOf(
                "Hostel phone chargers overnight chorene se kitna carbon badhta hai?",
                "Is buying local street water better than plastic drinking bottles?",
                "Make me a custom eco dare to challenge my collage friends!"
            )
            profile.userRole.contains("Corporate", ignoreCase = true) || profile.userRole.contains("Parent", ignoreCase = true) -> listOf(
                "Set AC at 26 instead of 18 degrees, kitna carbon bachta hai?",
                "Delivery box ka excessive packaging stop karne ko Hinglish email write karo.",
                " балконе kitchen waste recycling saste me kaise karein?"
            )
            else -> listOf(
                "Why should kids turn off lights when playing outdoors?",
                "Seasonal native fruits are better than cold-stored apples?",
                "Explain carbon footprint in simple Hinglish story please."
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
    ) {
        // Chat Header with Clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .border(BorderStroke(1.dp, DeepSaathiTeal.copy(alpha = 0.2f)))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeepSaathiTeal)
                ) {
                    Text("💡", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Bolo Carbon Saathi",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = DeepSaathiTeal
                    )
                    Text(
                        text = "I reply in Hinglish/Hindi • Witty but respectful",
                        fontSize = 10.sp,
                        color = MutedSlate
                    )
                }
            }

            TextButton(
                onClick = onClearHistory,
                colors = ButtonDefaults.textButtonColors(contentColor = WarmRetroCoral)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear Chat", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Vibe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Rolling Conversation Column
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                // Empty state centered
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawCircle(color = SunGlowYellow, radius = size.minDimension / 2.5f)
                        drawCircle(color = DeepSaathiTeal, radius = size.minDimension / 3.5f)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bolo Saathi! Koi carbon doubt hai?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = DeepSaathiTeal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Post any question about transport, crop residue parali alternatives, lights, or local items in Hinglish, English, or simple Hindi! I will give you brutal witty answers.",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        if (msg.role == "user") {
                            UserChatBubble(msg.content, msg.isVoice)
                        } else {
                            SaathiStructuredReply(msg.content, onAcceptChallenge = { title, cat, co2 ->
                                val fakeChallenge = DailyEcoChallenge(
                                    id = "c_${System.currentTimeMillis()}",
                                    title = title,
                                    category = cat,
                                    co2Saved = co2,
                                    points = 15,
                                    description = "Accepted from Chat!",
                                    characterContext = "Sourced dynamically from Carbon Saathi guide"
                                )
                                onQuickChallengeAccepted(fakeChallenge)
                            })
                        }
                    }

                    if (isGenerating) {
                        item {
                            SaathiThinkingBubble()
                        }
                    }
                }
            }
        }

        // Suggested Doubt Micro Chips Scroll
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            quickDoubts.forEach { doubt ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftSand)
                        .clickable { onSendMessage(doubt, false) }
                        .border(1.dp, DeepSaathiTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = doubt,
                        fontSize = 11.sp,
                        color = DeepSaathiTeal,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Input Tray Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Micro record transcription simulator
                IconButton(
                    onClick = {
                        val simulatedText = viewModel.getRandomSpeechSimulation(profile.ageGroup)
                        onSendMessage(simulatedText, true)
                        Toast.makeText(context, "🎤 Hinglish Speech Transcribed!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(WarmRetroCoral)
                        .size(44.dp)
                        .testTag("voice_mic_button")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice Input", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Input Text
                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = { rawInputText = it },
                    placeholder = { Text("Pucho, Carbon doubt kya hai?", fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepSaathiTeal,
                        unfocusedBorderColor = SoftSand,
                        unfocusedContainerColor = SoftSand
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 120.dp)
                        .testTag("chat_input_textfield"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // SEND button with retro circle
                IconButton(
                    onClick = {
                        if (rawInputText.isNotBlank()) {
                            onSendMessage(rawInputText, false)
                            rawInputText = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DeepSaathiTeal)
                        .size(44.dp)
                        .testTag("chat_send_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Send Message", tint = Color.White)
                }
            }
        }
    }
}

// --- SUB-VIEWS FOR CHAT SCREEN ---

@Composable
fun UserChatBubble(text: String, isVoice: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp))
                .background(DeepSaathiTeal)
                .padding(12.dp)
        ) {
            Column {
                if (isVoice) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Mic, contentDescription = "V", tint = WarmRetroCoral, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Transcribed Hinglish Audio", style = MaterialTheme.typography.labelSmall, color = SoftSand)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SaathiThinkingBubble() {
    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE2E8F0))
                .padding(12.dp)
        ) {
            Text(
                text = "Carbon Saathi is typing witty calculations... 🌿",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepSaathiTeal
            )
        }
    }
}

@Composable
fun SaathiStructuredReply(rawContent: String, onAcceptChallenge: (title: String, category: String, co2: Double) -> Unit) {
    val parsedResponse = remember(rawContent) { ParsedSaathiResponse.parse(rawContent) }
    var deepDiveExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Render 3-5 custom stacked vertically scrollable retro cards!
        
        // CARD 1: Summary Card (Header)
        Card(
            colors = CardDefaults.cardColors(containerColor = SoftSand),
            border = BorderStroke(2.dp, DeepSaathiTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CARBON SAATHI SUMMARY",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = MutedSlate,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = parsedResponse.summary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepSaathiTeal,
                    lineHeight = 20.sp
                )
            }
        }

        // CARD 2: Metric Highlight Card (if exists)
        if (parsedResponse.metric.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarmRetroCoral.copy(alpha = 0.15f)),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(WarmRetroCoral)
                    ) {
                        Icon(Icons.Filled.Eco, contentDescription = "M", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ESTIMATED IMPACT / ECO BALANCE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MutedSlate
                        )
                        Text(
                            text = parsedResponse.metric.trimStart('*').trimEnd('*'),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = DeepSaathiTeal
                        )
                    }
                }
            }
        }

        // CARD 3: Analysis with Analogy/Story
        if (parsedResponse.analysis.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Gehri Baat / Story comparison",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = WarmRetroCoral
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Soft light green background layout for paragraph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftSand)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = parsedResponse.analysis,
                            fontSize = 12.sp,
                            color = DeepSaathiTeal,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // CARD 4: Clear Steps Checklist with Action
        if (parsedResponse.actions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Recommended Eco Action Dares:",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = DeepSaathiTeal
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    parsedResponse.actions.forEachIndexed { index, act ->
                        // Parse simple CO2 estimation from string
                        val co2Value = remember(act) {
                            val match = Regex("""(\d+(\.\d+)?)""").find(act)
                            match?.value?.toDoubleOrNull() ?: 1.2
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, DeepSaathiTeal.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable {
                                    val cleanedName = act.take(45) + "..."
                                    onAcceptChallenge(cleanedName, "Chat", co2Value)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Act", tint = WarmRetroCoral, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = act,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepSaathiTeal,
                                modifier = Modifier.weight(1f)
                            )
                            // Click cue chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(WarmRetroCoral)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Accept Challenge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // CARD 5: Deep Dive Collapsible Accordion
        if (parsedResponse.deepDive.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, DeepSaathiTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deepDiveExpanded = !deepDiveExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔬 Science Deep Dive (For Geeks)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                        Icon(
                            imageVector = if (deepDiveExpanded) Icons.Filled.WbSunny else Icons.Filled.Eco,
                            contentDescription = "Expand",
                            tint = DeepSaathiTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (deepDiveExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                        .background(SoftSand)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = parsedResponse.deepDive,
                                fontSize = 11.sp,
                                color = MutedText,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- IMPACT LOG SCREEN (ANALYTICS, COMPLETED BADGES & HISTORY LOG) ---
@Composable
fun ImpactLogScreen(
    profile: UserProfile,
    logs: List<com.example.data.TrackingLog>,
    completedCount: Int,
    onResetStats: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Mock category aggregation for Chart rendering
    val transportCsaved = remember(logs) { logs.filter { it.category == "Transport" }.sumOf { it.co2Saved }.toFloat() }
    val energyCsaved = remember(logs) { logs.filter { it.category == "Energy" }.sumOf { it.co2Saved }.toFloat() }
    val foodCsaved = remember(logs) { logs.filter { it.category == "Food" }.sumOf { it.co2Saved }.toFloat() }
    val farmingCsaved = remember(logs) { logs.filter { it.category == "Farming" }.sumOf { it.co2Saved }.toFloat() }

    val defaultSum = 1.0f
    val totalSum = (transportCsaved + energyCsaved + foodCsaved + farmingCsaved).coerceAtLeast(defaultSum)

    val aggregateMap = listOf(
        Pair("Commute", transportCsaved),
        Pair("Bulbs/AC", energyCsaved),
        Pair("Food/Bag", foodCsaved),
        Pair("Agri Residue", farmingCsaved)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftSand)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // IMPACT SUMMARY TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aapka Green Score",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = DeepSaathiTeal
                )
                Text(
                    text = "Historical tracking & climate reviews",
                    fontSize = 11.sp,
                    color = MutedSlate
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, WarmRetroCoral, RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .clickable { onResetStats() }
                    .padding(8.dp)
            ) {
                Text("Reset Score", color = WarmRetroCoral, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NATIVE GRAPHICAL BAR CHART DRAWING
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, DeepSaathiTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CO2 Distribution (kg saved)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepSaathiTeal
                )
                Text(
                    text = "Visualizing your custom carbon savings split",
                    fontSize = 11.sp,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bar charts layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    aggregateMap.forEach { (cat, savedValue) ->
                        val fraction = savedValue / totalSum
                        val displayPerc = (fraction * 100).toInt()

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Eco, contentDescription = "E", tint = DeepSaathiTeal, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepSaathiTeal)
                                }
                                Text(
                                    text = String.format("%.2f kg (%d%%)", savedValue, displayPerc),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = WarmRetroCoral
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Horizontal Bar representing progress
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(SoftSand)
                                    .border(1.dp, DeepSaathiTeal.copy(alpha = 0.2f), RoundedCornerShape(7.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction.coerceIn(0.04f, 1.0f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(DeepSaathiTeal, WarmRetroCoral)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CO2 COUNTER RETRO BADGES CONGRUENCY
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, DeepSaathiTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Climate Badges Unlocked 🏅",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = DeepSaathiTeal
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badge1Eligible = true // always unlocked onboarding
                    val badge2Eligible = profile.totalCo2Saved >= 5.0
                    val badge3Eligible = completedCount >= 1

                    // Badge 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftSand)
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(44.dp)) {
                            drawCircle(color = if (badge1Eligible) LeafGreen else MutedSlate)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("CO2 Hero", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepSaathiTeal, textAlign = TextAlign.Center)
                    }

                    // Badge 2
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (badge2Eligible) SoftSand else SoftSand.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(44.dp)) {
                                drawCircle(color = if (badge2Eligible) SunGlowYellow else MutedSlate.copy(alpha = 0.6f))
                            }
                            if (!badge2Eligible) Icon(Icons.Filled.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp), tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Neem King", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepSaathiTeal, textAlign = TextAlign.Center)
                    }

                    // Badge 3
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (badge3Eligible) SoftSand else SoftSand.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(44.dp)) {
                                drawCircle(color = if (badge3Eligible) WarmRetroCoral else MutedSlate.copy(alpha = 0.6f))
                            }
                            if (!badge3Eligible) Icon(Icons.Filled.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp), tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Eco Champ", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = DeepSaathiTeal, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // WITTY CONTEXTUAL REVIEW CARD
        val reviewVibeText = remember(profile.userRole, profile.totalCo2Saved) {
            when {
                profile.userRole.contains("Corporate") && profile.totalCo2Saved < 4.0 -> {
                    "Arey Meera! AC 26 degree pe chalao bhaiya, chill rehna zaruri hai par wallet me garmi chalegi kya? Commutes me single rider cabs discard karo and metro board karo!"
                }
                profile.userRole.contains("Farmer") && profile.totalCo2Saved < 4.0 -> {
                    "Sanjay bhai! Kachi parali jalane se dhuwan hi nahi mitti ka nitrogen bhi urr jata hai. Composting controller test karke mitti ki upjau shakti bacha lo!"
                }
                profile.userRole.contains("Senior") && profile.totalCo2Saved < 4.0 -> {
                    "Dadu, aapka shadow garden chalna best parampariq tarika hai. Kitchen water pulse recycling speed up karke streak badhaiye!"
                }
                profile.userRole.contains("Teen") && profile.totalCo2Saved < 4.0 -> {
                    "Ravi/Priya, college hostel board charger se vampire electricity draw stop karo! Skip cab scrolling, take pedometer pedestrian steps instead."
                }
                profile.totalCo2Saved >= 10.0 -> {
                    "Gazab Saathi! You are saving massive carbon units like a real climatic postman. Doston se bhi app share karo taaki sab carbon reduction path seekh sakein!"
                }
                else -> {
                    "Aapka carbon graph slow hai bhaiya, but remember - small daily drops create the ocean. Click on checklist dares to increment score!"
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SunGlowYellow.copy(alpha = 0.15f)),
            border = BorderStroke(2.dp, DeepSaathiTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🚨 Carbon Saathi Brutal Vibe Check Review:",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = WarmRetroCoral
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reviewVibeText,
                    fontSize = 12.sp,
                    color = DeepSaathiTeal,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HISTORICAL ACTION LOGS
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, DeepSaathiTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Action History Log",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = DeepSaathiTeal
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (logs.isEmpty()) {
                    Text(
                        text = "Aapne abhi tak koi tracked log record nahi kiya. Try tracking your commute/bulbs on the Home tab!",
                        fontSize = 12.sp,
                        color = MutedSlate
                    )
                } else {
                    logs.take(15).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SoftSand)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = log.activityName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DeepSaathiTeal
                                )
                                Text(
                                    text = "Category: ${log.category} • Qty: ${log.quantity.toInt()}",
                                    fontSize = 10.sp,
                                    color = MutedSlate
                                )
                            }
                            Text(
                                text = String.format("-%.2f kg", log.co2Saved),
                                fontWeight = FontWeight.ExtraBold,
                                color = LeafGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
