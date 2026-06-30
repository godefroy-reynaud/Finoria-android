package com.finoria.app.data.local.entity

import com.finoria.app.data.model.Account
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
    )

fun WidgetShortcutEntity.toDomain(): WidgetShortcut =
    WidgetShortcut(
        id = UUID.fromString(id),
        amount = amount,
        comment = comment,
        type = type,
        category = category,
    )

fun WidgetShortcut.toEntity(accountId: UUID): WidgetShortcutEntity =
    WidgetShortcutEntity(
        id = id.toString(),
        accountId = accountId.toString(),
        amount = amount,
        comment = comment,
        type = type,
        category = category,
    )
