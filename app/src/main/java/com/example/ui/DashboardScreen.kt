package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatSessionEntity
import com.example.data.MessageEntity
import com.example.data.PersonaEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.AppDocument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val personas by viewModel.allPersonas.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("playground") } // "playground", "personas", "connect"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CyberDarkSurface,
                modifier = Modifier.width(320.dp)
            ) {
                DrawerConsoleContent(
                    sessions = sessions,
                    personas = personas,
                    activeSession = activeSession,
                    viewModel = viewModel,
                    onSessionSelected = { sessionId ->
                        viewModel.selectActiveSession(sessionId)
                        scope.launch { drawerState.close() }
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(CyberNeonRed.copy(alpha = alpha), RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Trans.ts("Ren ai"),
                                color = CyberTextHigh,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Historical Console Drawers",
                                tint = CyberTextHigh
                            )
                        }
                    },
                    actions = {
                        val activeProvider = activeSession?.provider?.uppercase() ?: "OFFLINE"
                        val activeModel = activeSession?.modelName?.uppercase()?.take(10) ?: "NONE"
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(CyberTerminalGray, RoundedCornerShape(24.dp))
                                .border(BorderStroke(1.dp, Color(0x33DC2626)), RoundedCornerShape(24.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$activeProvider:",
                                    color = CyberTextDim,
                                    
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeModel,
                                    color = CyberTextHigh,
                                    
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "▼",
                                    color = CyberTextDim,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = CyberBlack,
                        titleContentColor = CyberTextHigh
                    )
                )
            },
            bottomBar = {
                CyberNavigationTabs(
                    activeTab = currentTab,
                    onTabChanged = { currentTab = it }
                )
            },
            containerColor = CyberBlack,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(CyberBlack, CyberDarkSurface)
                        )
                    )
            ) {
                // Top Border Glow Line
                HorizontalDivider(color = CyberDarkCrimson, thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 850.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        when (currentTab) {
                            "playground" -> PlaygroundTab(viewModel, activeSession, activeMessages)
                            "personas" -> PersonasTab(viewModel, personas)
                            "analysis" -> AnalysisTab(viewModel)
                            "connect" -> ConnectTab(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberNavigationTabs(
    activeTab: String,
    onTabChanged: (String) -> Unit
) {
    Surface(
        color = CyberBlack,
        border = BorderStroke(1.dp, CyberDarkCrimson),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            CyberTabItem(
                label = "PLAYGROUND",
                icon = Icons.Default.PlayArrow,
                isActive = activeTab == "playground",
                onClick = { onTabChanged("playground") },
                modifier = Modifier.weight(1f).testTag("tab_playground")
            )
            CyberTabItem(
                label = "PERSONAS",
                icon = Icons.Default.Settings,
                isActive = activeTab == "personas",
                onClick = { onTabChanged("personas") },
                modifier = Modifier.weight(1f).testTag("tab_personas")
            )
            CyberTabItem(
                label = "DECK",
                icon = Icons.Default.Build,
                isActive = activeTab == "analysis",
                onClick = { onTabChanged("analysis") },
                modifier = Modifier.weight(1f).testTag("tab_analysis")
            )
            CyberTabItem(
                label = "CONNECT",
                icon = Icons.Default.Lock,
                isActive = activeTab == "connect",
                onClick = { onTabChanged("connect") },
                modifier = Modifier.weight(1f).testTag("tab_connect")
            )
        }
    }
}

@Composable
fun CyberTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowColor = if (isActive) CyberNeonRed else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) CyberNeonRed else CyberTextDim,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) CyberTextHigh else CyberTextDim,
            
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Under-bar glow strip
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(glowColor)
        )
    }
}

@Composable
fun DrawerConsoleContent(
    sessions: List<ChatSessionEntity>,
    personas: List<PersonaEntity>,
    activeSession: ChatSessionEntity?,
    viewModel: MainViewModel,
    onSessionSelected: (String) -> Unit,
    onCloseDrawer: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                Trans.ts("REN AI WORKSPACES"),
                color = CyberNeonRed,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            IconButton(onClick = onCloseDrawer) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close drawers panel",
                    tint = CyberTextDim
                )
            }
        }

        HorizontalDivider(color = CyberDarkCrimson, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        // Trigger Initialize Core Button
        Button(
            onClick = { showCreateDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = CyberDarkCrimson),
            border = BorderStroke(1.dp, CyberNeonRed),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("initialize_core_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = CyberTextHigh)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                Trans.ts("INITIALIZE NEW CORE"),
                color = CyberTextHigh,
                
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        Text(
            "CHANNELS HISTORY [ROOM]",
            color = CyberTextDim,
            
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "NO CHANNELS ONLINE\n\nTap above to mount an AI Core.",
                    color = CyberTextDim,
                    
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isSelected = session.id == activeSession?.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) CyberDarkSurface else CyberDarkCard,
                                RoundedCornerShape(24.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) CyberNeonRed else CyberDarkCrimson,
                                RoundedCornerShape(24.dp)
                            )
                            .clickable { onSessionSelected(session.id) }
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    session.title,
                                    color = CyberTextHigh,
                                    
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${session.provider.uppercase()} // ${session.modelName}",
                                    color = CyberTextDim,
                                    
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteSession(session.id) },
                                modifier = Modifier.size(24.dp).testTag("delete_session_btn_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Decomission Core",
                                    tint = CyberTextDim,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        var showSettingsDialog by remember { mutableStateOf(false) }
        
        // Settings / Preferences Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSettingsDialog = true }
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = CyberTextDim, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                Trans.ts("GLOBAL APP PREFERENCES"),
                color = CyberTextDim,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Seeding / Information Status Indicator
        Surface(
            color = CyberDarkCard,
            border = BorderStroke(1.dp, CyberDarkCrimson),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    "DATABASE REGISTRY ONLINE",
                    color = CyberTextHigh,
                    
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Nodes synced: ${sessions.size} cores loaded.",
                    color = CyberTextDim,
                    
                    fontSize = 9.sp
                )
            }
        }
        if (showSettingsDialog) {
            SettingsDialog(onDismiss = { showSettingsDialog = false })
        }
    }

    if (showCreateDialog) {
        CreateCoreDialog(
            viewModel = viewModel,
            personas = personas,
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                viewModel.createSessionFromConfig()
                showCreateDialog = false
                onCloseDrawer()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCoreDialog(
    viewModel: MainViewModel,
    personas: List<PersonaEntity>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title by viewModel.configTitle.collectAsStateWithLifecycle()
    val provider by viewModel.configProvider.collectAsStateWithLifecycle()
    val model by viewModel.configModel.collectAsStateWithLifecycle()
    val personaId by viewModel.configPersonaId.collectAsStateWithLifecycle()
    val maxTokens by viewModel.configMaxTokens.collectAsStateWithLifecycle()
    val temp by viewModel.configTemp.collectAsStateWithLifecycle()
    val memoryType by viewModel.configMemoryType.collectAsStateWithLifecycle()
    val fixedLimit by viewModel.configFixedLimit.collectAsStateWithLifecycle()

    var showProviderDropdown by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    var showPersonaDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                Trans.ts("MOUNT AI CORE MODULE"),
                color = CyberNeonRed,
                
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        containerColor = CyberDarkSurface,
        textContentColor = CyberTextHigh,
        modifier = Modifier
            .border(1.dp, CyberNeonRed, RoundedCornerShape(24.dp))
            .testTag("create_core_dialog"),
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        "Configure runtime matrices for localized context routing.",
                        color = CyberTextDim,
                        
                        fontSize = 10.sp
                    )
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.configTitle.value = it },
                        label = { Text(Trans.ts("Core Channel Alias"), ) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberDarkCrimson,
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        singleLine = true,
                        placeholder = { Text("e.g. Secret Lab Channel", color = CyberTextDim) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("core_alias_input")
                    )
                }

                // Provider Select
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = provider.uppercase(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Trans.ts("Provider Node"), ) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberNeonRed,
                                unfocusedBorderColor = CyberDarkCrimson,
                                focusedTextColor = CyberTextHigh,
                                unfocusedTextColor = CyberTextHigh
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showProviderDropdown = !showProviderDropdown }) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = CyberNeonRed)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProviderDropdown = !showProviderDropdown }
                                .testTag("provider_dropdown_field")
                        )
                        DropdownMenu(
                            expanded = showProviderDropdown,
                            onDismissRequest = { showProviderDropdown = false },
                            modifier = Modifier.background(CyberDarkSurface).border(1.dp, CyberDarkCrimson)
                        ) {
                            listOf("gemini", "groq", "openrouter", "custom").forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov.uppercase(), color = CyberTextHigh, ) },
                                    onClick = {
                                        viewModel.configProvider.value = prov
                                        showProviderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Model Select
                item {
                    if (provider == "custom") {
                        OutlinedTextField(
                            value = model,
                            onValueChange = { viewModel.configModel.value = it },
                            label = { Text(Trans.ts("Active Custom Model"), ) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberNeonRed,
                                unfocusedBorderColor = CyberDarkCrimson,
                                focusedTextColor = CyberTextHigh,
                                unfocusedTextColor = CyberTextHigh,
                                focusedLabelColor = CyberNeonRed,
                                unfocusedLabelColor = CyberTextDim
                            ),
                            placeholder = { Text("e.g. gpt-4o", color = CyberTextDim) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("model_custom_field")
                        )
                    } else {
                        val availableModels = when (provider) {
                            "groq" -> viewModel.groqModels
                            "openrouter" -> viewModel.openRouterModels
                            else -> viewModel.geminiModels
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = model,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(Trans.ts("Active AI Model"), ) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberNeonRed,
                                    unfocusedBorderColor = CyberDarkCrimson,
                                    focusedTextColor = CyberTextHigh,
                                    unfocusedTextColor = CyberTextHigh
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { showModelDropdown = !showModelDropdown }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyberNeonRed)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showModelDropdown = !showModelDropdown }
                                    .testTag("model_dropdown_field")
                            )
                            DropdownMenu(
                                expanded = showModelDropdown,
                                onDismissRequest = { showModelDropdown = false },
                                modifier = Modifier.background(CyberDarkSurface).border(1.dp, CyberDarkCrimson)
                            ) {
                                availableModels.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m, color = CyberTextHigh,  fontSize = 11.sp) },
                                        onClick = {
                                            viewModel.configModel.value = m
                                            showModelDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Persona System Instruction selection
                item {
                    val activePersonaName = personas.find { it.id == personaId }?.name ?: "Expert Coder"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = activePersonaName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(Trans.ts("System Persona Prompt"), ) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberNeonRed,
                                unfocusedBorderColor = CyberDarkCrimson,
                                focusedTextColor = CyberTextHigh,
                                unfocusedTextColor = CyberTextHigh
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPersonaDropdown = !showPersonaDropdown }) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = CyberNeonRed)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPersonaDropdown = !showPersonaDropdown }
                                .testTag("persona_dropdown_field")
                        )
                        DropdownMenu(
                            expanded = showPersonaDropdown,
                            onDismissRequest = { showPersonaDropdown = false },
                            modifier = Modifier.background(CyberDarkSurface).border(1.dp, CyberDarkCrimson)
                        ) {
                            personas.forEach { p ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${p.name} ${if (p.isTemplate) "★" else ""}",
                                            color = CyberTextHigh,
                                            
                                            fontSize = 11.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.configPersonaId.value = p.id
                                        showPersonaDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Memory Toggles
                item {
                    Text(
                        "MEMORY CONTEXT SELECTOR",
                        color = CyberNeonRed,
                        
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.configMemoryType.value = "fixed_window" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (memoryType == "fixed_window") CyberDarkCrimson else CyberDarkCard
                            ),
                            border = BorderStroke(1.dp, if (memoryType == "fixed_window") CyberNeonRed else CyberDarkCrimson),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "FIXED N WINDOW",
                                color = CyberTextHigh,
                                
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = { viewModel.configMemoryType.value = "infinite" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (memoryType == "infinite") CyberDarkCrimson else CyberDarkCard
                            ),
                            border = BorderStroke(1.dp, if (memoryType == "infinite") CyberNeonRed else CyberDarkCrimson),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "INFINITE SUMMARY",
                                color = CyberTextHigh,
                                
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Conditionally render N slider limit
                if (memoryType == "fixed_window") {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Fixed Window Buffer (N messages)",
                                    color = CyberTextDim,
                                    
                                    fontSize = 10.sp
                                )
                                Text(
                                    "$fixedLimit Msgs",
                                    color = CyberNeonRed,
                                    
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = fixedLimit.toFloat(),
                                onValueChange = { viewModel.configFixedLimit.value = it.toInt() },
                                valueRange = 2f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyberNeonRed,
                                    activeTrackColor = CyberNeonRed,
                                    inactiveTrackColor = CyberTerminalGray
                                ),
                                modifier = Modifier.testTag("fixed_limit_slider")
                            )
                        }
                    }
                }

                // Gen Threshold max tokens limit
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Max Completion Tokens",
                                color = CyberTextDim,
                                
                                fontSize = 10.sp
                            )
                            Text(
                                "$maxTokens Tokens",
                                color = CyberNeonRed,
                                
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = maxTokens.toFloat(),
                            onValueChange = { viewModel.configMaxTokens.value = it.toInt() },
                            valueRange = 128f..4096f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberNeonRed,
                                activeTrackColor = CyberNeonRed,
                                inactiveTrackColor = CyberTerminalGray
                            ),
                            modifier = Modifier.testTag("max_tokens_slider")
                        )
                    }
                }

                // Temperature Slider
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Inference Temperature",
                                color = CyberTextDim,
                                
                                fontSize = 10.sp
                            )
                            Text(
                                String.format("%.2f", temp),
                                color = CyberNeonRed,
                                
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = temp,
                            onValueChange = { viewModel.configTemp.value = it },
                            valueRange = 0.0f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberNeonRed,
                                activeTrackColor = CyberNeonRed,
                                inactiveTrackColor = CyberTerminalGray
                            ),
                            modifier = Modifier.testTag("temperature_slider")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonRed),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("confirm_mount_core_btn")
            ) {
                Text(
                    "COMPILE & MOUNT",
                    color = CyberTextHigh,
                    
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "ABORT",
                    color = CyberTextDim,
                    
                    fontSize = 11.sp
                )
            }
        }
    )
}

@Composable
fun PlaygroundTab(
    viewModel: MainViewModel,
    activeSession: ChatSessionEntity?,
    messages: List<MessageEntity>
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val liveStreamContent by viewModel.currentStreamContent.collectAsStateWithLifecycle()

    var userPromptText by remember { mutableStateOf("") }

    // Scroll automatically whenever messages list changes or streaming output updates
    LaunchedEffect(messages.size, liveStreamContent, isGenerating) {
        if (messages.isNotEmpty() || liveStreamContent.isNotEmpty()) {
            val totalItems = messages.size + if (isGenerating && liveStreamContent.isNotEmpty()) 1 else 0
            if (totalItems > 0) {
                scrollState.scrollToItem(totalItems - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        if (activeSession == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyberNeonRed,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        Trans.ts("CORE TERMINAL OFFLINE"),
                        color = CyberNeonRed,
                        
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Swipe open the left drawer or press the menu button to compile and mount an active AI processing node. Be sure to paste your API credentials in the CONNECT panel.",
                        color = CyberTextDim,
                        
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // Header Channel & Memory Diagnostics in Immersive UI Theme layout
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, Color(0x33DC2626)), // red-900/30
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Status Info
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(CyberTerminalGreen, RoundedCornerShape(50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE CORE: ${activeSession.title.uppercase()}",
                                color = CyberTextHigh,
                                
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Compact Clear Button
                        IconButton(
                            onClick = { viewModel.clearMessages() },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("clear_channel_messages_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear memory channel",
                                tint = CyberNeonRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Memory Buffer Progress Stats
                    val memoryTypeLabel = if (activeSession.memoryType == "fixed_window") {
                        "FIXED SLIDING WINDOW: ${activeSession.fixedWindowLimit} MSG"
                    } else {
                        "INFINITE SYSTEM COMPRESSION INDEX"
                    }
                    val percentFill = if (activeSession.memoryType == "fixed_window") {
                        if (activeSession.fixedWindowLimit > 0) {
                            (messages.size.toFloat() / activeSession.fixedWindowLimit.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                    } else {
                        0.67f // default visually appealing filled level for infinite context
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = memoryTypeLabel,
                            color = CyberTextDim,
                            
                            fontSize = 9.sp
                        )
                        Text(
                            text = if (activeSession.memoryType == "fixed_window") {
                                "${messages.size} / ${activeSession.fixedWindowLimit} Msgs"
                            } else {
                                "4096 TOKENS CAP"
                            },
                            color = CyberNeonRed,
                            
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Customized immersive style linear progress indicator
                    LinearProgressIndicator(
                        progress = { percentFill },
                        color = CyberNeonRed,
                        trackColor = CyberTerminalGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "NODE RELAY: ${activeSession.provider.uppercase()} // ${activeSession.modelName}",
                            color = CyberTextDim,
                            
                            fontSize = 8.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "API_SECURE • LATENCY: 42ms",
                                color = CyberTerminalGreen,
                                
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Scrollable Console Messages
            LazyColumn(
                state = scrollState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && liveStreamContent.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    Trans.ts("LINK CONSOLE ESTABLISHED"),
                                    color = CyberTerminalGreen,
                                    
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    Trans.ts("Waiting for telemetry string packet..."),
                                    color = CyberTextDim,
                                    
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        ConsoleChatMessageCard(msg = msg)
                    }

                    // Continuous Stream Token Printout row
                    if (isGenerating && liveStreamContent.isNotEmpty()) {
                        item {
                            ConsoleStreamingOutputCard(content = liveStreamContent)
                        }
                    }
                }
            }

            // Generating Progress Loading Visualizer
            if (isGenerating) {
                LinearProgressIndicator(
                    color = CyberNeonRed,
                    trackColor = CyberTerminalGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
                Text(
                    Trans.ts(">> INCOMING AI TELEMETRY STRING BYTES..."),
                    color = CyberTerminalGreen,
                    
                    fontSize = 8.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Controller Send Dock Input in Immersive UI style (Layout, pill container, beautiful tactile send button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .background(CyberDarkCard, RoundedCornerShape(24.dp))
                        .border(
                            BorderStroke(1.dp, Color(0x33DC2626)),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 12.dp)
                ) {
                    OutlinedTextField(
                        value = userPromptText,
                        onValueChange = { userPromptText = it },
                        placeholder = {
                            Text(
                                "Query Neural Core...",
                                color = CyberTextDim,
                                
                                fontSize = 11.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            
                            fontSize = 12.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_textfield"),
                        maxLines = 4
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (userPromptText.isNotBlank() && !isGenerating) CyberNeonRed else CyberTerminalGray
                        )
                        .clickable(enabled = userPromptText.isNotBlank() && !isGenerating) {
                            viewModel.sendMessage(userPromptText)
                            userPromptText = ""
                            focusManager.clearFocus()
                        }
                        .testTag("chat_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send packet uplink",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConsoleChatMessageCard(msg: MessageEntity) {
    val isUser = msg.role.lowercase() == "user"
    val alignArrangement = if (isUser) Arrangement.End else Arrangement.Start
    val bgColor = if (isUser) CyberNeonRed else CyberDarkSurface
    val textColor = if (isUser) Color.White else CyberTextHigh

    Row(
        horizontalArrangement = alignArrangement,
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CyberNeonRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "AI Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bgColor, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Text(
                text = msg.content,
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ConsoleStreamingOutputCard(content: String) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(CyberNeonRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "AI Avatar",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(CyberDarkSurface, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Text(
                text = content + "█",
                color = CyberTextHigh,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PersonasTab(viewModel: MainViewModel, personas: List<PersonaEntity>) {
    val newName by viewModel.newPersonaName.collectAsStateWithLifecycle()
    val newInstructions by viewModel.newPersonaInstructions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "AI PERSONA DIRECTIVES MODULE",
                color = CyberNeonRed,
                
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Configure core prompts for systemic AI reasoning presets.",
                color = CyberTextDim,
                
                fontSize = 10.sp
            )
        }

        // Section: Compile New Directive Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, Color(0x33DC2626)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "COMPILE NEW CUSTOM DIRECTIVE",
                        color = CyberTextHigh,
                        
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { viewModel.newPersonaName.value = it },
                        label = { Text("Directive Profile Alias", ) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberTerminalGray,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        singleLine = true,
                        placeholder = { Text("e.g. Strict Security Guard", color = CyberTextDim) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_persona_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newInstructions,
                        onValueChange = { viewModel.newPersonaInstructions.value = it },
                        label = { Text("System Context Instructions", ) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberTerminalGray,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        placeholder = { Text("Compile system prompt directives...", color = CyberTextDim) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("new_persona_prompt_input"),
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.createCustomPersona() },
                        enabled = newName.isNotBlank() && newInstructions.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberNeonRed,
                            disabledContainerColor = CyberTerminalGray
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("compile_persona_btn")
                    ) {
                        Text(
                            "COMPILE DIRECTIVE",
                            color = CyberTextHigh,
                            
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Section: Active Persona Directories list
        item {
            Text(
                "SYNTAX DIRECTIVES DATABASE REGISTRY",
                color = CyberNeonRed,
                
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        items(personas, key = { it.id }) { persona ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, if (persona.isTemplate) Color(0x33DC2626) else CyberNeonRed),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (persona.isTemplate) Icons.Default.Star else Icons.Default.Person,
                                contentDescription = null,
                                tint = CyberNeonRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                persona.name.uppercase(),
                                color = CyberTextHigh,
                                
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (!persona.isTemplate) {
                            IconButton(
                                onClick = { viewModel.deletePersona(persona.id) },
                                modifier = Modifier.size(24.dp).testTag("delete_persona_btn_${persona.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Decompile Directive",
                                    tint = CyberTextDim,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Text(
                                "[SYSTEM_CORE]",
                                color = CyberTextDim,
                                
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        persona.systemInstruction,
                        color = CyberTextDim,
                        
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectTab(viewModel: MainViewModel) {
    val groqKey by viewModel.groqKeyInput.collectAsStateWithLifecycle()
    val openRouterKey by viewModel.openRouterKeyInput.collectAsStateWithLifecycle()
    val geminiKey by viewModel.geminiKeyInput.collectAsStateWithLifecycle()
    val customKey by viewModel.customKeyInput.collectAsStateWithLifecycle()
    val customUrl by viewModel.customUrlInput.collectAsStateWithLifecycle()

    var maskKeyInputValues by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "CORE NETWORK ROUTER CREDENTIALS",
                color = CyberNeonRed,
                
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Integrate custom personal API keys. Credentials reside strictly inside local SQLite/SharedPreferences storage buffers, never touching unauthorized cloud telemetry nodes.",
                color = CyberTextDim,
                
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }

        // Section API Key Fields Surface
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, Color(0x33DC2626)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PROVIDER AUTHORIZATION PRESETS",
                            color = CyberTextHigh,
                            
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        IconButton(
                            onClick = { maskKeyInputValues = !maskKeyInputValues },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (maskKeyInputValues) Icons.Default.Lock else Icons.Default.Info,
                                contentDescription = if (maskKeyInputValues) "Unmask variables" else "Mask variables",
                                tint = CyberNeonRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Groq Credential
                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = { viewModel.groqKeyInput.value = it },
                        label = { Text("Groq API Key Matrix", ) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberTerminalGray,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        placeholder = { Text("gsk_...", color = CyberTextDim) },
                        visualTransformation = if (maskKeyInputValues) PasswordVisualTransformation() else VisualTransformation.None,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_groq_key")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. OpenRouter Credential
                    OutlinedTextField(
                        value = openRouterKey,
                        onValueChange = { viewModel.openRouterKeyInput.value = it },
                        label = { Text("OpenRouter Key Matrix", ) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberTerminalGray,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        placeholder = { Text("sk-or-...", color = CyberTextDim) },
                        visualTransformation = if (maskKeyInputValues) PasswordVisualTransformation() else VisualTransformation.None,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_openrouter_key")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Optional Gemini Credential
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { viewModel.geminiKeyInput.value = it },
                        label = { Text("Gemini Custom Override Matrix", ) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberTerminalGray,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        placeholder = { Text("Leave blank to utilize system bundle key", color = CyberTextDim, fontSize = 9.sp) },
                        visualTransformation = if (maskKeyInputValues) PasswordVisualTransformation() else VisualTransformation.None,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_gemini_key")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Custom API Credential
                    OutlinedTextField(
                        value = customKey,
                        onValueChange = { viewModel.customKeyInput.value = it },
                        label = { Text("Custom Provider API Key Matrix", ) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberTerminalGray,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        placeholder = { Text("Optional for local providers e.g. Ollama", color = CyberTextDim, fontSize = 9.sp) },
                        visualTransformation = if (maskKeyInputValues) PasswordVisualTransformation() else VisualTransformation.None,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_key")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 5. Custom API URL Endpoint
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { viewModel.customUrlInput.value = it },
                        label = { Text("Custom Provider Endpoint URL", ) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextHigh,
                            unfocusedTextColor = CyberTextHigh,
                            focusedBorderColor = CyberNeonRed,
                            unfocusedBorderColor = CyberTerminalGray,
                            focusedLabelColor = CyberNeonRed,
                            unfocusedLabelColor = CyberTextDim
                        ),
                        placeholder = { Text("https://api.openai.com/v1/chat/completions", color = CyberTextDim, fontSize = 9.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_url")
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save keys Button
                    Button(
                        onClick = { viewModel.saveConfigKeys() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonRed),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_keys_button")
                    ) {
                        Text(
                            "COMMIT NETWORK KEYS",
                            color = CyberTextHigh,
                            
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clear keys Button
                    TextButton(
                        onClick = { viewModel.purgeKeys() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_keys_button")
                    ) {
                        Text(
                            "WIPE STORAGE KEY BUFFER",
                            color = CyberTextDim,
                            
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Diagnostics metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, Color(0x33DC2626)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "UPLINK ROUTER SYSTEM REPORT",
                        color = CyberTerminalGreen,
                        
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Groq Key: ${if (groqKey.isNotEmpty()) "LINKED [GSK]" else "UNBOUNDED"}",
                        color = CyberTextDim,
                        
                        fontSize = 10.sp
                    )
                    Text(
                        "• OpenRouter Key: ${if (openRouterKey.isNotEmpty()) "LINKED [SK-OR]" else "UNBOUNDED"}",
                        color = CyberTextDim,
                        
                        fontSize = 10.sp
                    )
                    Text(
                        "• Gemini Module: ${if (geminiKey.isNotEmpty()) "OVERRIDE LINKED" else "SYSTEM KEY INTEGRATED"}",
                        color = CyberTextDim,
                        
                        fontSize = 10.sp
                    )
                    Text(
                        "• Custom Provider: Key: ${if (customKey.isNotEmpty()) "LINKED" else "VOID (OPTIONAL)"} // Endpoint: $customUrl",
                        color = CyberTextDim,
                        
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// --- HELPER FUNCTION FOR URI RESOLUTION ---
fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path?.substringAfterLast('/')
    }
    return name
}

// --- HELPER FOR BYTES DISPLAY ---
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) {
        return String.format("%.1f KB", kb)
    }
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}

@Composable
fun AnalysisTab(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Collecting states from MainViewModel
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    
    val latencyMs by viewModel.currentLatencyMs.collectAsStateWithLifecycle()
    val ttftMs by viewModel.currentTTFTMs.collectAsStateWithLifecycle()
    val tokensPerSec by viewModel.currentTokensPerSec.collectAsStateWithLifecycle()
    val latencyHistory by viewModel.latencyHistory.collectAsStateWithLifecycle()
    
    val documents by viewModel.uploadedDocuments.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    val resolvedName = getFileNameFromUri(context, uri) ?: "imported_node.txt"
                    viewModel.importNewDocument(resolvedName, content)
                    TacticalSoundController.playUploadAck()
                }
            } catch (e: Exception) {
                Log.e("AnalysisTab", "Failed to resolve file stream", e)
                TacticalSoundController.playError()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TAB HEADER
        item {
            Column {
                Text(
                    "CORE SYSTEM TELEMETRY & DECKS",
                    color = CyberNeonRed,
                    
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.testTag("analysis_title")
                )
                Text(
                    "OPERATIONAL DIAGNOSTIC CONSOLE // LOCAL FILE INTEGRATOR",
                    color = CyberTextDim,
                    
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CyberDarkCrimson, thickness = 1.dp)
            }
        }

        // TELEMETRY SUMMARY CARDS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, Color(0x33DC2626))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("LATENCY (SUM)", color = CyberTextDim, fontSize = 9.sp, )
                        Text("${latencyMs}ms", color = CyberNeonRed, fontSize = 18.sp, fontWeight = FontWeight.Bold, )
                        Text("RTT PACKET", color = CyberTerminalGreen, fontSize = 8.sp, )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, Color(0x33DC2626))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TTFT METRIC", color = CyberTextDim, fontSize = 9.sp, )
                        Text("${ttftMs}ms", color = CyberNeonRed, fontSize = 18.sp, fontWeight = FontWeight.Bold, )
                        Text("FIRST BYTE STREAM", color = CyberTerminalGreen, fontSize = 8.sp, )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, Color(0x33DC2626))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TOKEN SPEED", color = CyberTextDim, fontSize = 9.sp, )
                        Text(String.format("%.1f", tokensPerSec), color = CyberNeonRed, fontSize = 18.sp, fontWeight = FontWeight.Bold, )
                        Text("EST TOKENS/SEC", color = CyberTerminalGreen, fontSize = 8.sp, )
                    }
                }
            }
        }

        // LIVE LATENCY DIAGRAM GRAPH (GLOWING CANVAS)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, CyberDarkCrimson)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "SYSTEM LATENCY TRANSACTION HISTORY",
                        color = CyberTextHigh,
                        
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // The glowing Canvas line graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(CyberBlack)
                            .border(1.dp, Color(0x1ADB2626))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            
                            // 1. Draw horizontal grid lines
                            val gridLines = 4
                            for (i in 0..gridLines) {
                                val y = (height / gridLines) * i
                                drawLine(
                                    color = Color(0x19FF0033),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(width, y),
                                    strokeWidth = 1f
                                )
                            }
                            
                            // 2. Draw vertical grid lines
                            val vertLines = 8
                            for (i in 0..vertLines) {
                                val x = (width / vertLines) * i
                                drawLine(
                                    color = Color(0x19FF0033),
                                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                                    end = androidx.compose.ui.geometry.Offset(x, height),
                                    strokeWidth = 1f
                                )
                            }

                            // 3. Plot the history curve
                            if (latencyHistory.isNotEmpty()) {
                                val maxVal = (latencyHistory.maxOrNull() ?: 500L).coerceAtLeast(300L).toFloat()
                                val pointsCount = latencyHistory.size
                                val stepX = width / (pointsCount - 1).coerceAtLeast(1)
                                
                                val path = Path()
                                val fillPath = Path()
                                
                                latencyHistory.forEachIndexed { idx, value ->
                                    // invert y because Canvas origin (0,0) is top-left
                                    val ratio = value.toFloat() / maxVal
                                    val x = idx * stepX
                                    val y = height - (ratio * height * 0.8f) - (height * 0.1f) // 10% bottom padding
                                    
                                    if (idx == 0) {
                                        path.moveTo(x, y)
                                        fillPath.moveTo(x, height)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }
                                    
                                    if (idx == pointsCount - 1) {
                                        fillPath.lineTo(x, height)
                                        fillPath.close()
                                    }
                                }
                                
                                // Draw fill gradient glow
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0x33FF0033), Color.Transparent)
                                    )
                                )
                                
                                // Draw main line
                                drawPath(
                                    path = path,
                                    color = CyberNeonRed,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                
                                // Draw nodes circular dots
                                latencyHistory.forEachIndexed { idx, value ->
                                    val ratio = value.toFloat() / maxVal
                                    val x = idx * stepX
                                    val y = height - (ratio * height * 0.8f) - (height * 0.1f)
                                    
                                    drawCircle(
                                        color = CyberBlack,
                                        radius = 5.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(x, y)
                                    )
                                    drawCircle(
                                        color = CyberNeonRed,
                                        radius = 3.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(x, y)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("T-14 REQS", color = CyberTextDim, fontSize = 8.sp, )
                        Text("DIAGNOSTICS PULSE RATIO: ACTIVE", color = CyberTerminalGreen, fontSize = 8.sp, )
                        Text("LIVE", color = CyberNeonRed, fontSize = 8.sp,  fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TACTICAL ENGINEERING CONTROLS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, Color(0x33DC2626))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ACOUSTIC & HAPTI-VIBE CALIBRATING",
                        color = CyberTextHigh,
                        
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Sound feed switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleSoundState(!soundEnabled)
                                TacticalSoundController.playClick()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PROCEDURAL SYNTH FEEDBACK", color = CyberTextHigh, fontSize = 11.sp, )
                            Text("Synthesized audio telemetry notifications", color = CyberTextDim, fontSize = 9.sp, )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                viewModel.toggleSoundState(it)
                                TacticalSoundController.playClick()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberNeonRed,
                                uncheckedThumbColor = CyberTextDim,
                                uncheckedTrackColor = CyberDarkSurface
                            ),
                            modifier = Modifier.testTag("sound_switch")
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0x19FF0033), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Haptics switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.toggleHapticsState(!hapticsEnabled)
                                TacticalSoundController.playClick()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TACTICAL HAPTICS SHIELD", color = CyberTextHigh, fontSize = 11.sp, )
                            Text("Physical click feedbacks on UI interactions", color = CyberTextDim, fontSize = 9.sp, )
                        }
                        Switch(
                            checked = hapticsEnabled,
                            onCheckedChange = {
                                viewModel.toggleHapticsState(it)
                                TacticalSoundController.playClick()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyberNeonRed,
                                uncheckedThumbColor = CyberTextDim,
                                uncheckedTrackColor = CyberDarkSurface
                            ),
                            modifier = Modifier.testTag("haptics_switch")
                        )
                    }
                }
            }
        }

        // SECURE DATABANK UPLOADER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberDarkCrimson)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CYBERDECK LOCAL DATA MATRIX",
                                color = CyberTextHigh,
                                
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "LINK CUSTOM DOCUMENT NODES SECURELY",
                                color = CyberTextDim,
                                
                                fontSize = 8.sp
                            )
                        }
                        Button(
                            onClick = {
                                TacticalSoundController.playClick()
                                filePickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberNeonRed),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MOUNT FILE",  fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (documents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CyberBlack)
                                .border(BorderStroke(1.dp, Color(0x33DC2626)), RoundedCornerShape(24.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = CyberTextDim, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "NO STORAGE NODES DETECTED",
                                    color = CyberTextDim,
                                    
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Import .txt/.log/.json documents here.",
                                    color = CyberTextDim,
                                    
                                    fontSize = 9.sp
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            documents.forEach { doc ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                                    border = BorderStroke(1.dp, Color(0x33DC2626))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = CyberNeonRed, modifier = Modifier.size(16.dp))
                                                Column {
                                                    Text(
                                                        doc.name,
                                                        color = CyberTextHigh,
                                                        
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "${formatBytes(doc.sizeBytes)} // ${doc.charCount} chars",
                                                        color = CyberTextDim,
                                                        
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    TacticalSoundController.playError()
                                                    viewModel.deleteDocument(doc.id)
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Shred node", tint = CyberNeonRed, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = Color(0x19FF0033), thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // MOUNT TO SESSION UTILITY
                                        Text(
                                            "LINK NODE ATTACHMENT UPLINK TO ACTIVE CHATS:",
                                            color = CyberTextDim,
                                            
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (sessions.isEmpty()) {
                                            Text(Trans.ts("No terminal sessions registered."), color = CyberTextDim, fontSize = 8.sp, )
                                        } else {
                                            androidx.compose.foundation.lazy.LazyRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(sessions) { sess ->
                                                    val isAttached = doc.attachedSessions.contains(sess.id)
                                                    AssistChip(
                                                        onClick = {
                                                            TacticalSoundController.playClick()
                                                            viewModel.toggleDocumentAttachment(doc.id, sess.id)
                                                        },
                                                        label = {
                                                            Text(
                                                                sess.title,
                                                                fontSize = 9.sp,
                                                                
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        },
                                                        colors = AssistChipDefaults.assistChipColors(
                                                            labelColor = if (isAttached) Color.White else CyberTextDim,
                                                            containerColor = if (isAttached) CyberNeonRed else Color.Transparent
                                                        ),
                                                        border = BorderStroke(1.dp, if (isAttached) CyberNeonRed else CyberDarkCrimson)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val isArabic by AppConfig.isArabic.collectAsState()
    val errorLogs by AppConfig.errorLogs.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDarkCard,
        titleContentColor = CyberTextHigh,
        textContentColor = CyberTextHigh,
        title = {
            Text(Trans.ts("GLOBAL APP PREFERENCES"), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Toggle
                Column {
                    Text(Trans.ts("Language"), color = CyberNeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("English", color = if (!isArabic) CyberTextHigh else CyberTextDim)
                        Switch(
                            checked = isArabic,
                            onCheckedChange = { AppConfig.isArabic.value = it },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlack,
                                checkedTrackColor = CyberNeonRed,
                                uncheckedThumbColor = CyberTextDim,
                                uncheckedTrackColor = CyberDarkCard
                            )
                        )
                        Text("العربية", color = if (isArabic) CyberTextHigh else CyberTextDim)
                    }
                    Text(Trans.ts("System Language Overlay"), fontSize = 10.sp, color = CyberTextDim)
                }

                HorizontalDivider(color = CyberDarkCrimson)

                // Error Logs
                Column {
                    Text(Trans.ts("Error Logs"), color = CyberNeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(Trans.ts("API Network Exceptions"), fontSize = 10.sp, color = CyberTextDim)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (errorLogs.isEmpty()) {
                        Text("No errors tracked.", color = CyberTextDim, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        errorLogs.forEach { log ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(log, color = CyberTextHigh, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Trans.ts("CANCEL"), color = CyberTextHigh)
            }
        }
    )
}
