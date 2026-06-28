package com.example.f1kotlin.data.local

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Преобразует Kotlin-объекты в JSON для Room и обратно.
 *
 * Moshi не умеет напрямую `List<RaceModel>` без указания generic-типа —
 * для списков используем [Types.newParameterizedType].
 */
@Singleton
class CacheJsonMapper @Inject constructor(
    private val moshi: Moshi,
) {

    fun <T> toJson(value: T, clazz: Class<T>): String =
        moshi.adapter(clazz).toJson(value)

    fun <T> fromJson(json: String, clazz: Class<T>): T? =
        moshi.adapter(clazz).fromJson(json)

    fun <T> toJsonList(items: List<T>, itemClass: Class<T>): String =
        listAdapter(itemClass).toJson(items)

    fun <T> fromJsonList(json: String, itemClass: Class<T>): List<T>? =
        listAdapter(itemClass).fromJson(json)

    private fun <T> listAdapter(itemClass: Class<T>): JsonAdapter<List<T>> {
        val type = Types.newParameterizedType(List::class.java, itemClass)
        return moshi.adapter(type)
    }
}
