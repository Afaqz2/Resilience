package com.resilience.app.di

import com.resilience.app.data.ai.StubSurvivalKnowledgeBase
import com.resilience.app.data.ai.SurvivalKnowledgeBase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the [SurvivalKnowledgeBase] implementation.
 *
 * To switch from the stub to a real RAG implementation:
 *   1. Create `RagSurvivalKnowledgeBase : SurvivalKnowledgeBase` that embeds queries
 *      and runs inference against your bundled corpus + local LLM.
 *   2. Replace `StubSurvivalKnowledgeBase::class` with `RagSurvivalKnowledgeBase::class` below.
 *   No other files need to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindSurvivalKnowledgeBase(
        stub: StubSurvivalKnowledgeBase
    ): SurvivalKnowledgeBase
}
