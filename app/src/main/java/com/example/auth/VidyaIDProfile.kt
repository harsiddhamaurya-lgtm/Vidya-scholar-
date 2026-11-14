package com.example.auth

data class VidyaIDProfile(
    val uid: String = "",
    val vidyaHandle: String = "", // e.g. "@vidya:arjun108"
    val displayName: String = "",
    val eloRating: Int = 1200,
    val rankTier: String = "Knight",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "vidyaHandle" to vidyaHandle,
        "displayName" to displayName,
        "eloRating" to eloRating,
        "rankTier" to rankTier,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): VidyaIDProfile {
            return VidyaIDProfile(
                uid = map["uid"] as? String ?: "",
                vidyaHandle = map["vidyaHandle"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                eloRating = (map["eloRating"] as? Number)?.toInt() ?: 1200,
                rankTier = map["rankTier"] as? String ?: "Knight",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
