package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardEntry
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardProfile
import com.example.f1_kotlin.domain.predictor.PredictorLeaderboardResult
import com.example.f1_kotlin.domain.predictor.PredictorNickname
import com.example.f1_kotlin.util.AppLogger
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Join/leave/никнейм/синк очков лидерборда через Firestore-транзакции
 * (уникальность ника в `nicknames/{normalized}`).
 */
@Singleton
class PredictorLeaderboardRepository @Inject constructor(
    private val authRepository: IAuthRepository,
) : IPredictorLeaderboardRepository {
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    override val currentUid: String?
        get() = authRepository.currentUser?.uid

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)
    private fun nicknameDoc(normalized: String) = firestore.collection("nicknames").document(normalized)
    private fun entryDoc(year: String, uid: String) =
        firestore.collection("leaderboards").document(year).collection("entries").document(uid)

    override suspend fun loadProfile(): PredictorLeaderboardProfile {
        val uid = currentUid ?: return PredictorLeaderboardProfile()
        return try {
            val snap = userDoc(uid).get().await()
            PredictorLeaderboardProfile.fromFirestoreMap(snap.data)
        } catch (e: Exception) {
            AppLogger.e(TAG, "loadProfile failed", e)
            PredictorLeaderboardProfile()
        }
    }

    override suspend fun loadLeaderboard(year: String): List<PredictorLeaderboardEntry> {
        return try {
            val snap = firestore.collection("leaderboards").document(year)
                .collection("entries")
                .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val list = snap.documents.map { doc ->
                PredictorLeaderboardEntry.fromFirestoreMap(doc.id, doc.data.orEmpty())
            }.sortedWith(
                compareByDescending<PredictorLeaderboardEntry> { it.totalPoints }
                    .thenBy { it.nickname.lowercase() },
            )
            list.mapIndexed { index, entry -> entry.withRank(index + 1) }
        } catch (e: Exception) {
            AppLogger.e(TAG, "loadLeaderboard failed", e)
            emptyList()
        }
    }

    override suspend fun join(nickname: String, year: String, totalPoints: Int): PredictorLeaderboardResult {
        PredictorNickname.validate(nickname)?.let { return PredictorLeaderboardResult.Fail(it) }
        val uid = currentUid ?: return PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        val trimmed = nickname.trim()
        val normalized = PredictorNickname.normalize(trimmed)
        return try {
            firestore.runTransaction { tx ->
                val userRef = userDoc(uid)
                val nickRef = nicknameDoc(normalized)
                val entryRef = entryDoc(year, uid)
                val profile = PredictorLeaderboardProfile.fromFirestoreMap(tx.get(userRef).data)
                val previousNormalized = profile.nickname?.let(PredictorNickname::normalize)

                val nickSnap = tx.get(nickRef)
                if (nickSnap.exists()) {
                    val owner = nickSnap.getString("uid")
                    if (owner != null && owner != uid) {
                        throw NicknameTakenException()
                    }
                }
                if (previousNormalized != null && previousNormalized != normalized) {
                    tx.delete(nicknameDoc(previousNormalized))
                }
                tx.set(nickRef, mapOf("uid" to uid, "nickname" to trimmed))
                tx.set(
                    userRef,
                    mapOf(
                        "nickname" to trimmed,
                        "nicknameNormalized" to normalized,
                        "leaderboardOptIn" to true,
                        "leaderboardOptInAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                tx.set(
                    entryRef,
                    mapOf(
                        "nickname" to trimmed,
                        "totalPoints" to totalPoints,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                null
            }.await()
            PredictorLeaderboardResult.Ok
        } catch (_: NicknameTakenException) {
            PredictorLeaderboardResult.Fail(ERROR_TAKEN)
        } catch (e: Exception) {
            AppLogger.e(TAG, "join failed", e)
            PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        }
    }

    override suspend fun leave(year: String): PredictorLeaderboardResult {
        val uid = currentUid ?: return PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        return try {
            val batch = firestore.batch()
            batch.set(userDoc(uid), mapOf("leaderboardOptIn" to false), SetOptions.merge())
            batch.delete(entryDoc(year, uid))
            batch.commit().await()
            PredictorLeaderboardResult.Ok
        } catch (e: Exception) {
            AppLogger.e(TAG, "leave failed", e)
            PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        }
    }

    override suspend fun updateNickname(nickname: String, year: String): PredictorLeaderboardResult {
        PredictorNickname.validate(nickname)?.let { return PredictorLeaderboardResult.Fail(it) }
        val uid = currentUid ?: return PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        val trimmed = nickname.trim()
        val normalized = PredictorNickname.normalize(trimmed)
        return try {
            firestore.runTransaction { tx ->
                val userRef = userDoc(uid)
                val nickRef = nicknameDoc(normalized)
                val entryRef = entryDoc(year, uid)
                val profile = PredictorLeaderboardProfile.fromFirestoreMap(tx.get(userRef).data)
                val previousNormalized = profile.nickname?.let(PredictorNickname::normalize)

                val nickSnap = tx.get(nickRef)
                if (nickSnap.exists()) {
                    val owner = nickSnap.getString("uid")
                    if (owner != null && owner != uid) {
                        throw NicknameTakenException()
                    }
                }
                if (previousNormalized != null && previousNormalized != normalized) {
                    tx.delete(nicknameDoc(previousNormalized))
                }
                tx.set(nickRef, mapOf("uid" to uid, "nickname" to trimmed))
                tx.set(
                    userRef,
                    mapOf(
                        "nickname" to trimmed,
                        "nicknameNormalized" to normalized,
                    ),
                    SetOptions.merge(),
                )
                if (profile.leaderboardOptIn) {
                    val entrySnap = tx.get(entryRef)
                    val points = (entrySnap.getLong("totalPoints") ?: 0L).toInt()
                    tx.set(
                        entryRef,
                        mapOf(
                            "nickname" to trimmed,
                            "totalPoints" to points,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                }
                null
            }.await()
            PredictorLeaderboardResult.Ok
        } catch (_: NicknameTakenException) {
            PredictorLeaderboardResult.Fail(ERROR_TAKEN)
        } catch (e: Exception) {
            AppLogger.e(TAG, "updateNickname failed", e)
            PredictorLeaderboardResult.Fail(ERROR_GENERIC)
        }
    }

    override suspend fun syncPoints(year: String, totalPoints: Int) {
        val uid = currentUid ?: return
        try {
            firestore.runTransaction { tx ->
                val userRef = userDoc(uid)
                val profile = PredictorLeaderboardProfile.fromFirestoreMap(tx.get(userRef).data)
                if (!profile.canShowOnLeaderboard) return@runTransaction null
                val nick = profile.nickname ?: return@runTransaction null
                tx.set(
                    entryDoc(year, uid),
                    mapOf(
                        "nickname" to nick,
                        "totalPoints" to totalPoints,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                null
            }.await()
        } catch (e: Exception) {
            AppLogger.e(TAG, "syncPoints failed", e)
        }
    }

    override fun clearMemoryCache() = Unit

    private class NicknameTakenException : RuntimeException()

    companion object {
        private const val TAG = "PredictorLeaderboardRepo"
        const val ERROR_GENERIC = "predictorLeaderboardErrorGeneric"
        const val ERROR_TAKEN = "predictorNicknameErrorTaken"
    }
}
