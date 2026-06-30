package com.finoria.app.data.local

import androidx.room.TypeConverter
import com.finoria.app.data.model.AccountStyle
import com.finoria.app.data.model.RecurrenceFrequency
import com.finoria.app.data.model.TransactionCategory
import com.finoria.app.data.model.TransactionType
import java.time.LocalDate

/**
 * TypeConverters Room.
 *
 * - Les enums sont stockés par leur **`name` Kotlin** (équivalent du `rawValue` iOS) :
 *   stable dans le temps, jamais l'ordinal. Ne jamais renommer/supprimer un cas publié.
 * - Les `LocalDate` sont stockés en **epoch day** (Long), compact et stable.
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromAccountStyle(value: AccountStyle): String = value.name

    @TypeConverter
    fun toAccountStyle(value: String): AccountStyle = AccountStyle.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromTransactionCategory(value: TransactionCategory): String = value.name

    @TypeConverter
    fun toTransactionCategory(value: String): TransactionCategory =
        TransactionCategory.valueOf(value)

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency =
        RecurrenceFrequency.valueOf(value)
}
