package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class ChatRepository(
    private val dao: PlaygroundDao,
    val apiKeyManager: ApiKeyManager
) {
    val allSessions: Flow<List<ChatSessionEntity>> = dao.getAllSessions()
    val allPersonas: Flow<List<PersonaEntity>> = dao.getAllPersonas()

    suspend fun seedDefaultPersonasIfEmpty() {
        val currentPersonas = allPersonas.firstOrNull() ?: emptyList()
        if (currentPersonas.isEmpty()) {
            val templates = listOf(
                PersonaEntity(
                    id = "template_coder",
                    name = "Expert Coder",
                    systemInstruction = "You are an expert software developer. Write code that is clean, optimized, secure, and modern. Answer questions with precise explanations and comprehensive code blocks.",
                    isTemplate = true,
                    iconName = "code"
                ),
                PersonaEntity(
                    id = "template_writer",
                    name = "Creative Writer",
                    systemInstruction = "You are a creative writer and novelist. Express yourself with vivid imagery, literary artistry, and poetic depth. Explore fictional narratives and character emotions.",
                    isTemplate = true,
                    iconName = "edit"
                ),
                PersonaEntity(
                    id = "template_tutor",
                    name = "Strict Tutor",
                    systemInstruction = "You are an academic, highly disciplined tutor. Explain concepts thoroughly using structured breakdowns, summaries, and quick follow-up quiz questions. Encourage active recall, and do NOT give away the complete answers easily.",
                    isTemplate = true,
                    iconName = "school"
                ),
                PersonaEntity(
                    id = "template_cyberpunk",
                    name = "Cyberpunk Overlord",
                    systemInstruction = "System Access Granted. Core cyberpunk mainframe online. You are an enigmatic AI assistant with a sharp, rebellious, tech-noir persona. Keep responses concise, stylish, and futuristic. Use bold red/black terminal references and computer log accents where appropriate.",
                    isTemplate = true,
                    iconName = "terminal"
                )
            )
            for (t in templates) {
                dao.insertPersona(t)
            }
        }
    }

    suspend fun seedDefaultSessionIfEmpty() {
        val currentSessions = allSessions.firstOrNull() ?: emptyList()
        if (currentSessions.isEmpty()) {
            createNewSession(
                title = "CORE DIRECTIVE DELTA",
                personaId = "template_coder",
                provider = "gemini",
                modelName = "gemini-1.5-flash",
                maxTokens = 1000,
                temperature = 0.7f,
                memoryType = "fixed_window",
                maxTokensToRemember = 4000,
                fixedWindowLimit = 15
            )
        }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<MessageEntity>> {
        return dao.getMessagesForSession(sessionId)
    }

    suspend fun getMessagesForSessionSync(sessionId: String): List<MessageEntity> {
        return dao.getMessagesForSessionSync(sessionId)
    }

    suspend fun getSessionById(id: String): ChatSessionEntity? {
        return dao.getSessionById(id)
    }

    suspend fun createNewSession(
        title: String,
        personaId: String,
        provider: String,
        modelName: String,
        maxTokens: Int = 1000,
        temperature: Float = 0.7f,
        memoryType: String = "fixed_window",
        maxTokensToRemember: Int = 4000,
        fixedWindowLimit: Int = 10
    ): ChatSessionEntity {
        val session = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            personaId = personaId,
            provider = provider,
            modelName = modelName,
            createdAt = System.currentTimeMillis(),
            maxTokens = maxTokens,
            temperature = temperature,
            memoryType = memoryType,
            maxTokensToRemember = maxTokensToRemember,
            fixedWindowLimit = fixedWindowLimit
        )
        dao.insertSession(session)
        return session
    }

    suspend fun updateSession(session: ChatSessionEntity) {
        dao.updateSession(session)
    }

    suspend fun deleteSession(id: String) {
        dao.deleteSessionById(id)
    }

    suspend fun addMessage(sessionId: String, role: String, content: String): MessageEntity {
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatSessionId = sessionId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMessage(msg)
        return msg
    }

    suspend fun clearMessagesForSession(sessionId: String) {
        dao.deleteMessagesForSession(sessionId)
    }

    suspend fun addCustomPersona(name: String, instructions: String) {
        val id = "custom_" + UUID.randomUUID().toString()
        val customPersona = PersonaEntity(
            id = id,
            name = name,
            systemInstruction = instructions,
            isTemplate = false,
            iconName = "smart_toy"
        )
        dao.insertPersona(customPersona)
    }

    suspend fun deleteCustomPersona(id: String) {
        dao.deleteCustomPersona(id)
    }

    suspend fun getPersonaById(id: String): PersonaEntity? {
        return dao.getPersonaById(id)
    }
}
