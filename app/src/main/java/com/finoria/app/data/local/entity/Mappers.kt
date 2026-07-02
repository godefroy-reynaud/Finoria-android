package com.finoria.app.data.local.entity

import com.finoria.app.data.model.Account
import com.finoria.app.data.model.CustomCategory
import com.finoria.app.data.model.RecurringTransaction
import com.finoria.app.data.model.Transaction
import com.finoria.app.data.model.WidgetShortcut
import java.util.UUID

/**
 * Conversions entité Room ↔ modèle de domaine.
 *
 * Les modèles de domaine (consommés par l'UI/ViewModel) restent inchangés ; seules
 * les entités connaissent `accountId` et la représentation String des UUID.
 */

fun AccountEntity.toDomain(): Account =
    Account(
        id = UUID.fromString(id),
        name = name,
        detail = detail,
        style = style,
    )

fun Account.toEntity(): AccountEntity =
    AccountEntity(
        id = id.toString(),
        name = name,
        detail = detail,
        style = style,
    )

fun TransactionEntity.toDomain(): Transaction =
    Transaction(
        id = UUID.fromString(id),
        amount = amount,
        comment = comment,
        potentiel = potentiel,
        date = date,
        category = category,
        recurringTransactionId = sourceRecurringId?.let(UUID::fromString),
        customCategoryId = customCategoryId?.let(UUID::fromString),
        importedCategoryName = importedCategoryName,
    )

fun Transaction.toEntity(accountId: UUID): TransactionEntity =
    TransactionEntity(
        id = id.toString(),
        accountId = accountId.toString(),
        amount = amount,
        comment = comment,
        potentiel = potentiel,
        date = date,
        category = category,
        sourceRecurringId = recurringTransactionId?.toString(),
        customCategoryId = customCategoryId?.toString(),
        importedCategoryName = importedCategoryName,
    )

fun RecurringTransactionEntity.toDomain(): RecurringTransaction =
    RecurringTransaction(
        id = UUID.fromString(id),
        amount = amount,
        comment = comment,
        type = type,
        category = category,
        frequency = frequency,
        startDate = startDate,
        lastGeneratedDate = lastGeneratedDate,
        isPaused = isPaused,
        customCategoryId = customCategoryId?.let(UUID::fromString),
    )

fun RecurringTransaction.toEntity(accountId: UUID): RecurringTransactionEntity =
    RecurringTransactionEntity(
        id = id.toString(),
        accountId = accountId.toString(),
        amount = amount,
        comment = comment,
        type = type,
        category = category,
        frequency = frequency,
        startDate = startDate,
        lastGeneratedDate = lastGeneratedDate,
        isPaused = isPaused,
        customCategoryId = customCategoryId?.toString(),
    )

fun WidgetShortcutEntity.toDomain(): WidgetShortcut =
    WidgetShortcut(
        id = UUID.fromString(id),
        amount = amount,
        comment = comment,
        type = type,
        category = category,
        customCategoryId = customCategoryId?.let(UUID::fromString),
    )

fun WidgetShortcut.toEntity(accountId: UUID): WidgetShortcutEntity =
    WidgetShortcutEntity(
        id = id.toString(),
        accountId = accountId.toString(),
        amount = amount,
        comment = comment,
        type = type,
        category = category,
        customCategoryId = customCategoryId?.toString(),
    )

fun CustomCategoryEntity.toDomain(): CustomCategory =
    CustomCategory(
        id = UUID.fromString(id),
        name = name,
        symbol = symbol,
        colorHex = colorHex,
    )

fun CustomCategory.toEntity(accountId: UUID): CustomCategoryEntity =
    CustomCategoryEntity(
        id = id.toString(),
        accountId = accountId.toString(),
        name = name,
        symbol = symbol,
        colorHex = colorHex,
    )
