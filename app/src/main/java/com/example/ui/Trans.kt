package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

object Trans {
    @Composable
    fun ts(key: String): String {
        val isArabic by AppConfig.isArabic.collectAsState()
        return t(key, isArabic)
    }

    fun t(key: String, isArabic: Boolean): String {
        return if (isArabic) arabicMap[key] ?: key else key
    }

    private val arabicMap = mapOf(
        "CORE TERMINAL OFFLINE" to "المحطة الأساسية غير متصلة",
        "AWAITING UPLINK INITIALIZATION" to "في انتظار تهيئة الاتصال",
        "No intelligence node active. Select a provider profile to begin." to "لا توجد عقدة ذكاء نشطة. حدد ملف مزود للبدء.",
        "SECURE CONNECTION" to "اتصال آمن",
        "INITIALIZE NEW CORE" to "تهيئة نظام جديد",
        "SYSTEM CONSOLES" to "وحدات تحكم النظام",
        "No historical uplinks found." to "لم يتم العثور على أي اتصالات تاريخية.",
        "Ren ai" to "رين آي",
        "REN AI WORKSPACES" to "مساحات عمل رين آي",
        "UPLINKS & NODES" to "الاتصالات والعقد",
        "DIRECTIVE SETTINGS" to "إعدادات التوجيه",
        "API KEYS MATRIX" to "مصفوفة مفاتيح API",
        "MOUNT AI CORE MODULE" to "تركيب وحدة الذكاء الاصطناعي",
        "SESSION PROTOCOL IDENTITY" to "هوية بروتوكول الجلسة",
        "Title (e.g., Code Assistant, Creative Writer)" to "العنوان (مثل: مساعد البرمجة، الكاتب الإبداعي)",
        "Core Channel Alias" to "الاسم المستعار للقناة",
        "INTELLIGENCE PROVIDER" to "مزود الذكاء",
        "Provider Node" to "عقدة المزود",
        "Model Type" to "نوع النموذج",
        "Active Custom Model" to "النموذج المخصص النشط",
        "Enter custom model identifier" to "أدخل معرف النموذج المخصص",
        "Active AI Model" to "نموذج الذكاء الاصطناعي النشط",
        "SYSTEM PERSONA CONFIGURATION" to "تكوين الشخصية النظامية",
        "System Persona Prompt" to "توجيه الشخصية النظامية",
        "COGNITIVE PARAMETERS" to "المعلمات الإدراكية",
        "Temperature (" to "درجة الحرارة (",
        "Max Output Tokens" to "الحد الأقصى للرموز",
        "CONTEXT WINDOW MANAGEMENT" to "إدارة نافذة السياق",
        "Fixed Token Window" to "نافذة رموز ثابتة",
        "Infinite Stream" to "بث لا نهائي",
        "Retains last N messages" to "يحتفظ بآخر N رسائل",
        "Appends all, summarizes if limits exceeded" to "يضم الكل، ويلخص إذا تم تجاوز الحدود",
        "Message Window Bound (N)" to "حد نافذة الرسائل (N)",
        "CANCEL" to "إلغاء",
        "INITIALIZE" to "تهيئة",
        "ACTIVE CORE: " to "النظام النشط: ",
        "UPDATE DIRECTIVE" to "تحديث التوجيه",
        "API SECURE KEYS MATRIX" to "مصفوفة مفاتيح API الآمنة",
        "USER:" to "المستخدم:",
        "REN AI:" to "رين آي:",
        "REN AI STREAMING..." to "رين آي يبث...",
        "SYSTEM TELEMETRY & DECKS" to "قياس النظام والمنصات",
        "MEMORY DATABANK UPLOADS (RAG)" to "تحميلات بنك الذاكرة المطلق",
        "LINK CONSOLE ESTABLISHED" to "تم إنشاء اتصال وحدة التحكم",
        "Waiting for telemetry string packet..." to "في انتظار حزمة القياس عن بعد...",
        "Terminal Sessions" to "جلسات طرفية",
        "GLOBAL APP PREFERENCES" to "تفضيلات التطبيق العامة",
        "Language" to "اللغة",
        "System Language Overlay" to "تغطية لغة النظام",
        "Error Logs" to "سجلات الأخطاء",
        "API Network Exceptions" to "استثناءات شبكة API",
        ">> INCOMING AI TELEMETRY STRING BYTES..." to ">> حزم القياس الواردة من الذكاء الاصطناعي...",
        "No terminal sessions registered." to "لا توجد جلسات مسجلة."
    )
}
