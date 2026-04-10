package com.resilience.app.data.ai

/**
 * Contract for the offline survival knowledge base (RAG layer).
 *
 * When the Survival data module is ready, replace [StubSurvivalKnowledgeBase]
 * with a real implementation that:
 *   1. Embeds the query with a local sentence-transformer (e.g. MiniLM via ONNX Runtime).
 *   2. Runs a cosine-similarity search over the embedded playbook / guide corpus.
 *   3. Feeds the top-k retrieved chunks + query into a local LLM (e.g. Phi-3-mini,
 *      Gemma-2B, or Llama-3.2-1B via MediaPipe LLM Inference API).
 *   4. Returns the generated response as plain text for TTS.
 *
 * The interface is deliberately simple so the OfflineVoiceAgent and AiChatViewModel
 * don't need to change at all when the real RAG is swapped in.
 */
interface SurvivalKnowledgeBase {
    /**
     * Query the knowledge base with [userText] and return the AI-generated response.
     * This is a suspend function so the real implementation can run inference
     * on a background thread without blocking the UI.
     */
    suspend fun query(userText: String): String
}

/**
 * Stub implementation — returns helpful placeholder responses until the
 * Survival data corpus and local LLM model files are bundled with the app.
 *
 * Replace this binding in [AiModule] (or your Hilt module) with the real
 * RAG implementation when ready.
 */
class StubSurvivalKnowledgeBase @javax.inject.Inject constructor() : SurvivalKnowledgeBase {

    private val responses = mapOf(
        listOf("water", "drink", "purif") to
            "To purify water: boil for at least 1 minute (3 min at altitude). " +
            "If fire unavailable, use water purification tablets or a portable filter. " +
            "Avoid drinking untreated water from unknown sources.",

        listOf("fire", "warm", "heat", "shelter") to
            "For emergency warmth: insulate from the ground first — cold ground pulls heat 25x faster than air. " +
            "Layer dry leaves or debris under you. Build a debris shelter using branches and leaves for wind protection.",

        listOf("first aid", "bleed", "wound", "injur") to
            "For bleeding: apply firm, direct pressure with a clean cloth for 10-15 minutes. " +
            "Do not remove the cloth — add more on top. Elevate the limb if possible. " +
            "Seek medical attention as soon as it is safe to do so.",

        listOf("food", "eat", "hungry") to
            "The human body can survive 3 weeks without food but only 3 days without water. " +
            "Prioritize water first. For food, avoid unknown berries or mushrooms. " +
            "Focus on finding insects (high protein) or edible plants you can identify with certainty.",

        listOf("signal", "rescue", "help", "sos") to
            "Signal rescuers using the universal SOS pattern: 3 short, 3 long, 3 short signals " +
            "(light, sound, or smoke). A signal mirror can be seen 10+ miles away in daylight. " +
            "Create ground-to-air signals: large X or SOS with rocks or logs in an open area.",

        listOf("navigate", "direction", "lost", "north") to
            "Without a compass: find north using a stick shadow method. Place a stick vertically. " +
            "Mark the tip of the shadow, wait 15 minutes, mark again — the line from first to second " +
            "mark points roughly east. Or locate the North Star (Polaris) at night.",
    )

    override suspend fun query(userText: String): String {
        val lower = userText.lowercase()
        for ((keywords, response) in responses) {
            if (keywords.any { lower.contains(it) }) return response
        }
        return "[SURVIVAL AI — DATA LOADING]\n" +
               "Survival knowledge module is initializing. " +
               "Consult your offline playbooks for immediate guidance. " +
               "Remember: Stay calm, conserve energy, and prioritize shelter, water, and signalling."
    }
}
