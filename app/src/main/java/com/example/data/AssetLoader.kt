package com.example.data

import android.content.Context
import com.example.model.AllahName
import com.example.model.AzkharItem
import com.example.model.District
import com.example.model.ReminderPhrase
import org.json.JSONArray
import org.json.JSONObject

object AssetLoader {

    fun loadDistricts(context: Context): List<District> {
        return runCatching {
            val jsonString = context.assets.open("districts_bd.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonString)
            val list = mutableListOf<District>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    District(
                        id = obj.getString("id"),
                        nameEn = obj.getString("nameEn"),
                        nameBn = obj.getString("nameBn"),
                        lat = obj.getDouble("lat"),
                        lng = obj.getDouble("lng")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun loadAzkhar(context: Context, filename: String): List<AzkharItem> {
        return runCatching {
            val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
            val array = JSONArray(jsonString)
            val list = mutableListOf<AzkharItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AzkharItem(
                        id = obj.getString("id"),
                        order = obj.optInt("order", i + 1),
                        titleEn = obj.getString("titleEn"),
                        titleBn = obj.getString("titleBn"),
                        arabic = obj.getString("arabic"),
                        transliteration = obj.getString("transliteration"),
                        translationEn = obj.getString("translationEn"),
                        translationBn = obj.getString("translationBn"),
                        target = obj.getInt("target")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun loadAsmaulHusna(context: Context): List<AllahName> {
        return runCatching {
            val jsonString = context.assets.open("asmaul_husna.json").bufferedReader().use { it.readText() }
            val array = JSONArray(jsonString)
            val list = mutableListOf<AllahName>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AllahName(
                        number = obj.getInt("number"),
                        arabic = obj.getString("arabic"),
                        transliteration = obj.getString("transliteration"),
                        meaningEn = obj.getString("meaningEn"),
                        meaningBn = obj.getString("meaningBn")
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    fun loadReminderPhrases(context: Context): Pair<List<ReminderPhrase>, List<ReminderPhrase>> {
        return runCatching {
            val jsonString = context.assets.open("reminder_phrases.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val istigfarArr = root.optJSONArray("istigfar") ?: JSONArray()
            val durudArr = root.optJSONArray("durud") ?: JSONArray()

            val istigfarList = mutableListOf<ReminderPhrase>()
            for (i in 0 until istigfarArr.length()) {
                val obj = istigfarArr.getJSONObject(i)
                istigfarList.add(
                    ReminderPhrase(
                        arabic = obj.getString("arabic"),
                        transliteration = obj.getString("transliteration"),
                        en = obj.getString("en"),
                        bn = obj.getString("bn")
                    )
                )
            }

            val durudList = mutableListOf<ReminderPhrase>()
            for (i in 0 until durudArr.length()) {
                val obj = durudArr.getJSONObject(i)
                durudList.add(
                    ReminderPhrase(
                        arabic = obj.getString("arabic"),
                        transliteration = obj.getString("transliteration"),
                        en = obj.getString("en"),
                        bn = obj.getString("bn")
                    )
                )
            }

            Pair(istigfarList, durudList)
        }.getOrDefault(Pair(emptyList(), emptyList()))
    }
}
