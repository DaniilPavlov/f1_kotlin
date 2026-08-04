package com.example.f1_kotlin.data.repository

import com.example.f1_kotlin.domain.predictor.PredictorSeason
import com.example.f1_kotlin.domain.predictor.PredictorStore
import com.example.f1_kotlin.domain.predictor.PredictorWeekendPrediction
import com.example.f1_kotlin.util.AppLogger
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Firestore-хранилище прогнозов сезона (`users/{uid}/seasons`).
 * Mutex + memory по uid; UI offline-кэш не трогает.
 */
@Singleton
class PredictorRepository @Inject constructor(
    private val authRepository: IAuthRepository,
) : IPredictorRepository {
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val mutex = Mutex()

    @Volatile private var memory: PredictorStore? = null
    @Volatile private var memoryUid: String? = null

    private fun seasonsCol(uid: String) =
        firestore.collection("users").document(uid).collection("seasons")

    override suspend fun load(): PredictorStore = mutex.withLock {
        val uid = authRepository.currentUser?.uid
        val cached = memory
        if (cached != null && memoryUid == uid && uid != null) return cached
        if (uid == null) {
            clearMemoryCacheUnlocked()
            return PredictorStore.empty()
        }
        return try {
            val snap = seasonsCol(uid).get().await()
            val seasons = snap.documents.associate { doc ->
                doc.id to PredictorSeason.fromFirestoreMap(doc.id, doc.data.orEmpty())
            }
            val store = PredictorStore(seasons = seasons)
            memory = store
            memoryUid = uid
            store
        } catch (e: Exception) {
            AppLogger.e(TAG, "load failed", e)
            clearMemoryCacheUnlocked()
            throw e
        }
    }

    override suspend fun saveWeekend(year: String, weekend: PredictorWeekendPrediction): PredictorStore =
        mutex.withLock {
            val uid = authRepository.currentUser?.uid
                ?: error("PredictorRepository.saveWeekend requires signed-in user")
            val current = memory?.takeIf { memoryUid == uid } ?: loadUnlocked(uid)
            val next = current.upsertWeekend(year, weekend)
            persistSeason(uid, year, next.season(year)!!)
            memory = next
            memoryUid = uid
            next
        }

    override suspend fun replace(store: PredictorStore): PredictorStore = mutex.withLock {
        val uid = authRepository.currentUser?.uid
            ?: error("PredictorRepository.replace requires signed-in user")
        for ((year, season) in store.seasons) {
            persistSeason(uid, year, season)
        }
        memory = store
        memoryUid = uid
        store
    }

    override fun clearMemoryCache() {
        memory = null
        memoryUid = null
    }

    private fun clearMemoryCacheUnlocked() {
        memory = null
        memoryUid = null
    }

    private suspend fun loadUnlocked(uid: String): PredictorStore {
        val snap = seasonsCol(uid).get().await()
        val seasons = snap.documents.associate { doc ->
            doc.id to PredictorSeason.fromFirestoreMap(doc.id, doc.data.orEmpty())
        }
        return PredictorStore(seasons = seasons)
    }

    private suspend fun persistSeason(uid: String, year: String, season: PredictorSeason) {
        seasonsCol(uid).document(year).set(
            season.toFirestoreMap() + ("updatedAt" to FieldValue.serverTimestamp()),
            SetOptions.merge(),
        ).await()
    }

    companion object {
        private const val TAG = "PredictorRepository"
    }
}
