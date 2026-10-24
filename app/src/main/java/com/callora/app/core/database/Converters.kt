package com.callora.app.core.database

import androidx.room.TypeConverter

/** Room converters are name-based so schema changes never silently corrupt rows. */
class CalloraConverters {

    @TypeConverter
    fun csvToStrings(value: String?): List<String> =
        value?.takeIf { it.isNotBlank() }?.split(CSV_SEP) ?: emptyList()

    @TypeConverter
    fun stringsToCsv(values: List<String>?): String =
        values?.joinToString(CSV_SEP).orEmpty()

    private companion object {
        const val CSV_SEP = ","
    }
}
