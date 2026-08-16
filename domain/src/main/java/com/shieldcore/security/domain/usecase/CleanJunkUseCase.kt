package com.shieldcore.security.domain.usecase

import com.shieldcore.security.domain.model.CleanSummary
import com.shieldcore.security.domain.model.JunkItem
import com.shieldcore.security.domain.repository.JunkCleanerRepository
import javax.inject.Inject

class CleanJunkUseCase @Inject constructor(
    private val repository: JunkCleanerRepository
) {
    suspend operator fun invoke(items: List<JunkItem>): CleanSummary {
        return repository.cleanJunk(items)
    }
}
