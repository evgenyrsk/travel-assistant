package com.travelassistant.backend.application.assistant

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@JvmInline
value class ConfirmedSearchExecutionIdempotencyKey(val value: String) {

    companion object {
        private const val PREFIX = "confirmed-search-execution"

        fun from(
            commandPlan: ConfirmedSearchCreationCommandPlan.CommandReady,
        ): ConfirmedSearchExecutionIdempotencyKey {
            val command = commandPlan.command
            val criteria = command.criteria
            val source = listOf(
                PREFIX,
                command.sessionId.value,
                criteria.destination.trim(),
                criteria.checkInDate.toString(),
                criteria.checkOutDate.toString(),
                criteria.guests.adults.toString(),
                criteria.guests.children.toString(),
                criteria.guests.childrenAges.sorted().joinToString(separator = ","),
                criteria.rooms?.toString().orEmpty(),
            ).joinToString(separator = "|")

            return ConfirmedSearchExecutionIdempotencyKey(
                "$PREFIX-${source.sha256Hex()}",
            )
        }

        private fun String.sha256Hex(): String {
            val digest = MessageDigest
                .getInstance("SHA-256")
                .digest(toByteArray(StandardCharsets.UTF_8))

            return digest.joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(length = 2, padChar = '0')
            }
        }
    }
}
