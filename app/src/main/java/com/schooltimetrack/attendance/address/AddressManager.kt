package com.schooltimetrack.attendance.address

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class AddressManager(private val context: Context) {
    private val dbNames = listOf("refBrgy.db", "refCitymun.db", "refProvince.db", "refRegion.db")
    private val databases = mutableMapOf<String, SQLiteDatabase>()

    init {
        setupDatabases()
    }

    private fun setupDatabases() {
        dbNames.forEach { dbName ->
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                copyDatabase(dbName, dbFile)
            }
            databases[dbName] = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
        }
    }

    private fun copyDatabase(dbName: String, dbFile: File) {
        dbFile.parentFile?.mkdirs()
        context.assets.open("databases/$dbName").use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    fun getAllRegions(): List<Region> {
        val db = databases["refRegion.db"] ?: return emptyList()
        val cursor = db.query(
            "refregion",
            null,
            null,
            null,
            null,
            null,
            "regDesc ASC"
        )

        return cursor.use {
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .map {
                    Region(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        psgcCode = it.getString(it.getColumnIndexOrThrow("psgcCode")),
                        regDesc = it.getString(it.getColumnIndexOrThrow("regDesc")).uppercase(Locale.ROOT),
                        regCode = it.getString(it.getColumnIndexOrThrow("regCode"))
                    )
                }.toList()
        }
    }

    fun getProvincesByRegion(regCode: String): List<Province> {
        val db = databases["refProvince.db"] ?: return emptyList()
        val cursor = db.query(
            "refprovince",
            null,
            "regCode = ?",
            arrayOf(regCode),
            null,
            null,
            "provDesc ASC"
        )

        return cursor.use {
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .map {
                    Province(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        psgcCode = it.getString(it.getColumnIndexOrThrow("psgcCode")),
                        provDesc = it.getString(it.getColumnIndexOrThrow("provDesc")).uppercase(Locale.ROOT),
                        regCode = it.getString(it.getColumnIndexOrThrow("regCode")),
                        provCode = it.getString(it.getColumnIndexOrThrow("provCode"))
                    )
                }.toList()
        }
    }

    fun getCityMunsByProvince(provCode: String): List<CityMun> {
        val db = databases["refCitymun.db"] ?: return emptyList()
        val cursor = db.query(
            "refcitymun",
            null,
            "provCode = ?",
            arrayOf(provCode),
            null,
            null,
            "citymunDesc ASC"
        )

        return cursor.use {
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .map {
                    CityMun(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        psgcCode = it.getString(it.getColumnIndexOrThrow("psgcCode")),
                        citymunDesc = it.getString(it.getColumnIndexOrThrow("citymunDesc")).uppercase(Locale.ROOT),
                        regDesc = it.getString(it.getColumnIndexOrThrow("regDesc")),
                        provCode = it.getString(it.getColumnIndexOrThrow("provCode")),
                        citymunCode = it.getString(it.getColumnIndexOrThrow("citymunCode"))
                    )
                }.toList()
        }
    }

    fun getBrgysByCityMun(citymunCode: String): List<Barangay> {
        val db = databases["refBrgy.db"] ?: return emptyList()
        val cursor = db.query(
            "refbrgy",
            null,
            "citymunCode = ?",
            arrayOf(citymunCode),
            null,
            null,
            "brgyDesc ASC"
        )

        return cursor.use {
            generateSequence { if (cursor.moveToNext()) cursor else null }
                .map {
                    Barangay(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        brgyCode = it.getString(it.getColumnIndexOrThrow("brgyCode")),
                        brgyDesc = it.getString(it.getColumnIndexOrThrow("brgyDesc")).uppercase(Locale.ROOT),
                        regCode = it.getString(it.getColumnIndexOrThrow("regCode")),
                        provCode = it.getString(it.getColumnIndexOrThrow("provCode")),
                        citymunCode = it.getString(it.getColumnIndexOrThrow("citymunCode"))
                    )
                }.toList()
        }
    }

    fun close() {
        databases.values.forEach { it.close() }
    }
}