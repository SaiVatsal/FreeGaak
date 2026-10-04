package com.saivatsal.soundorbit.core.source

import com.saivatsal.soundorbit.core.model.SourceId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRegistry @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards MusicSource>
) {
    private val sourceMap: Map<SourceId, MusicSource> = sources.associateBy { it.id }

    fun allSources(): List<MusicSource> = sourceMap.values.toList()

    fun getSource(id: SourceId): MusicSource? = sourceMap[id]

    fun isAvailable(id: SourceId): Boolean = sourceMap.containsKey(id)
}
