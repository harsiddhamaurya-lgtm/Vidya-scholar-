package com.example.auth

import android.content.Context
import com.example.data.VidyaIdentityCard
import com.example.data.repository.ScholarIdentityRepository
import kotlinx.coroutines.flow.firstOrNull

object VidyaIDManager {
    suspend fun lookupProfileByVidyaId(
        context: Context,
        vidyaIdQuery: String
    ): VidyaIdentityCard? {
        val repo = ScholarIdentityRepository(context)
        return repo.lookupScholarByVidyaId(vidyaIdQuery).getOrNull()
    }

    suspend fun getCurrentIdentityCard(
        context: Context,
        uid: String
    ): VidyaIdentityCard? {
        val repo = ScholarIdentityRepository(context)
        val profile = repo.observeUserProfile(uid).firstOrNull() ?: return null
        if (profile.vidyaId.isBlank()) return null
        return VidyaIdentityCard(
            uid = profile.uid,
            vidyaId = profile.vidyaId,
            vidyaHandle = profile.vidyaHandle,
            vidyaDiscriminator = profile.vidyaDiscriminator,
            displayName = profile.name,
            level = profile.level,
            avatarMode = profile.avatarMode,
            avatarPresetId = profile.avatarPresetId,
            avatarCrestId = profile.avatarCrestId,
            avatarFrameId = profile.avatarFrameId,
            avatarToneId = profile.avatarToneId,
            focusRealm = profile.focusRealm
        )
    }
}
