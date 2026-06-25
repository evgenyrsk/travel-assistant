package com.travelassistant.backend.application.assistant

data class ProceedWithCandidateConfirmationProposal(
    val summary: String,
    val confirmationQuestion: String,
    val displayFields: List<ProceedWithCandidateConfirmationField>,
)
