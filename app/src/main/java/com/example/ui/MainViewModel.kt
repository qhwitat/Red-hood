package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.AIApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val dao = database.playgroundDao()
    private val apiKeyManager = ApiKeyManager(application)
    val repository = ChatRepository(dao, apiKeyManager)
    private val apiService = AIApiService(apiKeyManager)

    // --- State Injections ---
    val allSessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPersonas: StateFlow<List<PersonaEntity>> = repository.allPersonas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessionId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSession: StateFlow<ChatSessionEntity?> = activeSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else {
                allSessions.map { list -> list.find { it.id == id } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<MessageEntity>> = activeSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMessagesForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Custom UI Model Listing ---
    val groqModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "gemma2-9b-it",
        "mixtral-8x7b-32768"
    )

    val openRouterModels = listOf(
        "anthropic/claude-3.5-sonnet",
        "google/gemini-2.5-flash",
        "openai/gpt-4o-mini",
        "meta-llama/llama-3.3-70b-instruct"
    )

    val geminiModels = listOf(
        "gemini-1.5-flash",
        "gemini-1.5-pro",
        "gemini-2.5-flash"
    )

    // --- Session Configuration Input States ---
    val configTitle = MutableStateFlow("")
    val configProvider = MutableStateFlow("gemini") // "gemini", "groq", "openrouter", "custom"
    val configModel = MutableStateFlow("gemini-1.5-flash")
    val configPersonaId = MutableStateFlow("template_coder")
    
    val configMaxTokens = MutableStateFlow(1000)
    val configTemp = MutableStateFlow(0.7f)
    val configMemoryType = MutableStateFlow("fixed_window") // "fixed_window", "infinite"
    val configFixedLimit = MutableStateFlow(10)

    // --- API Input Key States ---
    val groqKeyInput = MutableStateFlow("")
    val openRouterKeyInput = MutableStateFlow("")
    val geminiKeyInput = MutableStateFlow("")
    val customKeyInput = MutableStateFlow("")
    val customUrlInput = MutableStateFlow("")

    // --- New Persona Creation ---
    val newPersonaName = MutableStateFlow("")
    val newPersonaInstructions = MutableStateFlow("")

    // --- Streaming State Variables ---
    val isGenerating = MutableStateFlow(false)
    val currentStreamContent = MutableStateFlow("")

    // --- Telemetry Diagnostics ---
    val currentLatencyMs = MutableStateFlow(0L)
    val currentTTFTMs = MutableStateFlow(0L)
    val currentTokensPerSec = MutableStateFlow(0.0)
    val latencyHistory = MutableStateFlow(listOf<Long>(140, 290, 210, 170, 380, 240, 320, 215))

    // --- Interactive Control Settings ---
    val soundEnabled = MutableStateFlow(true)
    val hapticsEnabled = MutableStateFlow(true)

    // --- local Secure File Databank ---
    private val docsDir = java.io.File(application.filesDir, "documents").apply { mkdirs() }
    val uploadedDocuments = MutableStateFlow<List<AppDocument>>(emptyList())

    init {
        // Core diagnostic sound & haptic boot values
        soundEnabled.value = apiKeyManager.isSoundEnabled()
        hapticsEnabled.value = apiKeyManager.isHapticsEnabled()
        TacticalSoundController.isSoundEnabled = soundEnabled.value

        // Seed default template instructions representation
        viewModelScope.launch {
            try {
                repository.seedDefaultPersonasIfEmpty()
                repository.seedDefaultSessionIfEmpty()
                // Pull stored keys from SharedPreferences into screen memory
                groqKeyInput.value = apiKeyManager.getGroqKey()
                openRouterKeyInput.value = apiKeyManager.getOpenRouterKey()
                geminiKeyInput.value = apiKeyManager.getRawUserGeminiKey()
                customKeyInput.value = apiKeyManager.getCustomKey()
                customUrlInput.value = apiKeyManager.getCustomUrl()

                // Load any local databank files
                loadStoredDocuments()

                // Pre-select first session if one exists
                // Wait until we get a non-empty list of sessions (e.g. after seeding), then activate it
                repository.allSessions.filter { it.isNotEmpty() }.first().firstOrNull()?.let { session ->
                    if (activeSessionId.value == null) {
                        activeSessionId.value = session.id
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Launcher initialization failed safely", e)
            }
        }

        // Keep local state in check
        viewModelScope.launch {
            try {
                configProvider.collect { provider ->
                    configModel.value = when (provider) {
                        "groq" -> groqModels.first()
                        "openrouter" -> openRouterModels.first()
                        "gemini" -> geminiModels.first()
                        "custom" -> "gpt-4o"
                        else -> "gemini-1.5-flash"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Config Provider stream collection failed safely", e)
            }
        }
    }

    // --- System Telemetry Recorder ---
    fun addNewTelemetryRecord(durationMs: Long, ttftMs: Long, charCount: Int) {
        currentLatencyMs.value = durationMs
        currentTTFTMs.value = ttftMs
        val durationSec = if (durationMs > 0) durationMs / 1000.0 else 1.0
        val estTokens = charCount / 4.0
        val tps = estTokens / durationSec
        currentTokensPerSec.value = if (tps.isFinite()) Math.round(tps * 100.0) / 100.0 else 12.5

        val currentList = latencyHistory.value.toMutableList()
        currentList.add(durationMs)
        if (currentList.size > 14) {
            currentList.removeAt(0)
        }
        latencyHistory.value = currentList
    }

    // --- Settings Switch Handlers ---
    fun toggleSoundState(enabled: Boolean) {
        soundEnabled.value = enabled
        apiKeyManager.setSoundEnabled(enabled)
        TacticalSoundController.isSoundEnabled = enabled
    }

    fun toggleHapticsState(enabled: Boolean) {
        hapticsEnabled.value = enabled
        apiKeyManager.setHapticsEnabled(enabled)
    }

    // --- local Secure File Databank Operations ---
    fun loadStoredDocuments() {
        viewModelScope.launch {
            try {
                val list = mutableListOf<AppDocument>()
                val metaFiles = docsDir.listFiles { _, name -> name.endsWith("_meta.properties") } ?: emptyArray()
                for (metaFile in metaFiles) {
                    val id = metaFile.name.substringBefore("_meta.properties")
                    val props = java.util.Properties()
                    metaFile.inputStream().use { props.load(it) }

                    val name = props.getProperty("name", "Unknown File")
                    val sizeBytes = props.getProperty("size", "0").toLongOrNull() ?: 0L
                    val charCount = props.getProperty("chars", "0").toIntOrNull() ?: 0
                    val importedAt = props.getProperty("imported", "0").toLongOrNull() ?: 0L
                    val attachedCSV = props.getProperty("attached", "")
                    val attachedSet = if (attachedCSV.isBlank()) emptySet() else attachedCSV.split(",").toSet()

                    // read matching content
                    val contentFile = java.io.File(docsDir, "${id}_content.txt")
                    val text = if (contentFile.exists()) contentFile.readText() else ""

                    list.add(AppDocument(id, name, text, sizeBytes, charCount, importedAt, attachedSet))
                }
                uploadedDocuments.value = list.sortedBy { it.importedAt }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to deserialize local files databank", e)
            }
        }
    }

    fun importNewDocument(name: String, content: String) {
        viewModelScope.launch {
            try {
                val id = UUID.randomUUID().toString()
                val bytes = content.toByteArray(Charsets.UTF_8).size.toLong()
                val chars = content.length

                // save content
                val contentFile = java.io.File(docsDir, "${id}_content.txt")
                contentFile.writeText(content)

                // save meta properties
                val metaFile = java.io.File(docsDir, "${id}_meta.properties")
                val props = java.util.Properties()
                props.setProperty("name", name)
                props.setProperty("size", bytes.toString())
                props.setProperty("chars", chars.toString())
                props.setProperty("imported", System.currentTimeMillis().toString())
                props.setProperty("attached", "")

                metaFile.outputStream().use { props.store(it, "File Node Decrypted") }

                loadStoredDocuments()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to encrypt/write file to database sandbox", e)
            }
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            try {
                java.io.File(docsDir, "${id}_content.txt").delete()
                java.io.File(docsDir, "${id}_meta.properties").delete()
                loadStoredDocuments()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failure to securely shred file asset", e)
            }
        }
    }

    fun toggleDocumentAttachment(documentId: String, sessionId: String) {
        viewModelScope.launch {
            try {
                val metaFile = java.io.File(docsDir, "${documentId}_meta.properties")
                if (metaFile.exists()) {
                    val props = java.util.Properties()
                    metaFile.inputStream().use { props.load(it) }
                    val currentAttached = props.getProperty("attached", "")
                    val attachedSet = if (currentAttached.isBlank()) mutableSetOf() else currentAttached.split(",").toMutableSet()
                    if (attachedSet.contains(sessionId)) {
                        attachedSet.remove(sessionId)
                    } else {
                        attachedSet.add(sessionId)
                    }
                    props.setProperty("attached", attachedSet.filter { it.isNotBlank() }.joinToString(","))
                    metaFile.outputStream().use { props.store(it, "Updated Secure Attachments") }
                    loadStoredDocuments()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Attachment state toggle crash", e)
            }
        }
    }

    // --- Actions ---

    fun selectActiveSession(sessionId: String) {
        activeSessionId.value = sessionId
    }

    fun saveConfigKeys() {
        apiKeyManager.saveGroqKey(groqKeyInput.value)
        apiKeyManager.saveOpenRouterKey(openRouterKeyInput.value)
        apiKeyManager.saveGeminiKey(geminiKeyInput.value)
        apiKeyManager.saveCustomKey(customKeyInput.value)
        apiKeyManager.saveCustomUrl(customUrlInput.value)
    }

    fun purgeKeys() {
        apiKeyManager.clearAllKeys()
        groqKeyInput.value = ""
        openRouterKeyInput.value = ""
        geminiKeyInput.value = ""
        customKeyInput.value = ""
        customUrlInput.value = "https://api.openai.com/v1/chat/completions"
    }

    fun createSessionFromConfig() {
        viewModelScope.launch {
            val titleText = configTitle.value.ifBlank { "Session [${configModel.value.substringAfter("/")}]" }
            val newSession = repository.createNewSession(
                title = titleText,
                personaId = configPersonaId.value,
                provider = configProvider.value,
                modelName = configModel.value,
                maxTokens = configMaxTokens.value,
                temperature = configTemp.value,
                memoryType = configMemoryType.value,
                maxTokensToRemember = 4000,
                fixedWindowLimit = configFixedLimit.value
            )
            // Select immediately
            activeSessionId.value = newSession.id
            // Reset fields
            configTitle.value = ""
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (activeSessionId.value == sessionId) {
                activeSessionId.value = allSessions.value.find { it.id != sessionId }?.id
            }
        }
    }

    fun createCustomPersona() {
        val name = newPersonaName.value
        val instructions = newPersonaInstructions.value
        if (name.isNotBlank() && instructions.isNotBlank()) {
            viewModelScope.launch {
                repository.addCustomPersona(name, instructions)
                newPersonaName.value = ""
                newPersonaInstructions.value = ""
            }
        }
    }

    fun deletePersona(id: String) {
        viewModelScope.launch {
            repository.deleteCustomPersona(id)
            if (configPersonaId.value == id) {
                configPersonaId.value = "template_coder"
            }
        }
    }

    fun sendMessage(userPrompt: String) {
        val sessionId = activeSessionId.value ?: return
        val session = activeSession.value ?: return
        if (userPrompt.isBlank() || isGenerating.value) return

        viewModelScope.launch {
            // 1. Add User prompt to database
            repository.addMessage(sessionId, "user", userPrompt)

            // 2. Fetch system persona
            val persona = repository.getPersonaById(session.personaId)
            var systemInstruction = persona?.systemInstruction ?: ""

            // --- local Secure File Databank Context Injection ---
            val attachedDocs = uploadedDocuments.value.filter { it.attachedSessions.contains(sessionId) }
            if (attachedDocs.isNotEmpty()) {
                val docContextBuilder = StringBuilder()
                docContextBuilder.append("\n\n[SYSTEM ATTACHED DATABANK - SECURE MEMORY INJECTION]\n")
                docContextBuilder.append("The operator has mounted database files to this active terminal. Refer to this data to fulfill the user prompt:\n")
                attachedDocs.forEach { doc ->
                    docContextBuilder.append("\n--- START SECURE NODE: ${doc.name.uppercase()} (${doc.charCount} CHARS) ---\n")
                    docContextBuilder.append(doc.text)
                    docContextBuilder.append("\n--- END SECURE NODE: ${doc.name.uppercase()} ---\n")
                }
                docContextBuilder.append("\nReference these sandboxed materials above to assist the user query.\n")
                systemInstruction += docContextBuilder.toString()
            }

            // 3. Fetch full historic sequence
            val messageHistory = repository.getMessagesForSessionSync(sessionId)

            // 4. Trigger streaming AI execution
            isGenerating.value = true
            currentStreamContent.value = ""

            val startTime = System.currentTimeMillis()
            var firstByteTime = 0L
            var charCount = 0

            try {
                apiService.streamChat(
                    provider = session.provider,
                    modelName = session.modelName,
                    systemInstruction = systemInstruction,
                    history = messageHistory,
                    maxTokens = session.maxTokens,
                    temperature = session.temperature,
                    memoryType = session.memoryType,
                    fixedWindowLimit = session.fixedWindowLimit
                ).collect { chunk ->
                    if (firstByteTime == 0L) {
                        firstByteTime = System.currentTimeMillis()
                    }
                    charCount += chunk.length
                    if (chunk.startsWith("Error")) {
                        currentStreamContent.value += "\n$chunk"
                        TacticalSoundController.playError()
                    } else {
                        currentStreamContent.value += chunk
                        TacticalSoundController.playTick() // Microsecond scan ticker beep!
                    }
                }

                val endTime = System.currentTimeMillis()
                val totalDuration = endTime - startTime
                val ttft = if (firstByteTime > 0L) (firstByteTime - startTime) else totalDuration
                
                // Save statistical metrics
                addNewTelemetryRecord(totalDuration, ttft, charCount)
                TacticalSoundController.playSuccess() // Neon confirm sound effect
            } catch (e: Exception) {
                Log.e("MainViewModel", "API connection packet drop", e)
                currentStreamContent.value += "\n[CONNECTION PACKET DROP: ${e.message}]"
                TacticalSoundController.playError()
            }

            // 5. Stream finished - save final output to DB and restore generating status
            if (currentStreamContent.value.isNotEmpty()) {
                repository.addMessage(sessionId, "assistant", currentStreamContent.value)
            }
            currentStreamContent.value = ""
            isGenerating.value = false
        }
    }

    fun clearMessages() {
        val sessionId = activeSessionId.value ?: return
        viewModelScope.launch {
            repository.clearMessagesForSession(sessionId)
        }
    }
}
