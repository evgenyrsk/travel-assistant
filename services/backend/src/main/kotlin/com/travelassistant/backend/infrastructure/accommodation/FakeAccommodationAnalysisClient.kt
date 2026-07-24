package com.travelassistant.backend.infrastructure.accommodation

import com.travelassistant.backend.application.accommodation.AccommodationAnalysisClient
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisRequest
import com.travelassistant.backend.application.accommodation.AccommodationAnalysisResult
import com.travelassistant.backend.domain.hotel.AccommodationConcept
import com.travelassistant.backend.domain.hotel.AccommodationEvidenceSource
import com.travelassistant.backend.domain.hotel.AccommodationMatchVerdict
import java.text.Normalizer
import java.util.Locale

class FakeAccommodationAnalysisClient : AccommodationAnalysisClient {

    override suspend fun analyze(
        request: AccommodationAnalysisRequest,
    ): AccommodationAnalysisResult.Completed =
        AccommodationAnalysisResult.Completed(
            decisions = request.candidates.map { candidate ->
                analyzeCandidate(request.concept, candidate)
            },
        )

    private fun analyzeCandidate(
        concept: AccommodationConcept,
        candidate: AccommodationAnalysisRequest.Candidate,
    ): AccommodationAnalysisResult.Decision =
        when (concept) {
            AccommodationConcept.GLAMPING -> analyzeGlamping(candidate)
        }

    private fun analyzeGlamping(
        candidate: AccommodationAnalysisRequest.Candidate,
    ): AccommodationAnalysisResult.Decision {
        val evidence = linkedSetOf<AccommodationAnalysisResult.Evidence>()
        val name = candidate.hotelName.normalized()
        val descriptions = candidate.descriptions.map { description -> description.normalized() }
        val amenities = candidate.amenities.map { amenity -> amenity.normalized() }

        addEvidence(name, AccommodationEvidenceSource.NAME, evidence)
        descriptions.forEach { description ->
            addEvidence(description, AccommodationEvidenceSource.DESCRIPTION, evidence)
        }
        amenities.forEach { amenity ->
            if (GLAMPING_AMENITY.containsMatchIn(amenity)) {
                evidence += AccommodationAnalysisResult.Evidence(
                    AccommodationEvidenceSource.AMENITIES,
                    AccommodationAnalysisResult.Signal.GLAMPING_AMENITY,
                )
            }
        }

        val positiveSignals = evidence
            .filter { item -> item.signal.positive }
            .map { item -> item.signal }
            .toSet()
        val explicit = AccommodationAnalysisResult.Signal.EXPLICIT_GLAMPING_LABEL in
            positiveSignals
        val negative = evidence.any { item -> !item.signal.positive }
        val verdict = when {
            negative && positiveSignals.isNotEmpty() -> AccommodationMatchVerdict.PROBABLE
            explicit || positiveSignals.size >= MIN_SIGNALS_FOR_MATCH ->
                AccommodationMatchVerdict.MATCH
            positiveSignals.size == 1 -> AccommodationMatchVerdict.PROBABLE
            negative -> AccommodationMatchVerdict.NO_MATCH
            else -> AccommodationMatchVerdict.UNKNOWN
        }

        return AccommodationAnalysisResult.Decision(
            ephemeralCandidateId = candidate.ephemeralCandidateId,
            verdict = verdict,
            evidence = evidence,
        )
    }

    private fun addEvidence(
        text: String,
        source: AccommodationEvidenceSource,
        evidence: MutableSet<AccommodationAnalysisResult.Evidence>,
    ) {
        SIGNAL_PATTERNS.forEach { (signal, pattern) ->
            if (pattern.containsMatchIn(text)) {
                evidence += AccommodationAnalysisResult.Evidence(source, signal)
            }
        }
    }

    private fun String.normalized(): String =
        Normalizer.normalize(this, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace(WHITESPACE, " ")
            .trim()

    private companion object {
        const val MIN_SIGNALS_FOR_MATCH = 2
        val WHITESPACE = Regex("""\s+""")
        val GLAMPING_AMENITY = Regex(
            """(?:костров|fire\s?pit|уличн[а-я]*\s+купел|outdoor\s+hot\s+tub|""" +
                """панорамн[а-я]*\s+окн|panoramic\s+window)""",
        )
        val SIGNAL_PATTERNS = listOf(
            AccommodationAnalysisResult.Signal.EXPLICIT_GLAMPING_LABEL to
                Regex("""(?<![\p{L}\p{N}])(?:гл[еэ]мпинг[\p{L}]*|glamping)(?![\p{L}\p{N}])"""),
            AccommodationAnalysisResult.Signal.GLAMPING_STRUCTURE to
                Regex(
                    """(?<![\p{L}\p{N}])(?:купол[\p{L}]*|д[оё]м[\p{L}]*|юрта|юрты|""" +
                        """сафари[ -]тент|tiny house|safari tent|yurt|dome|equipped tent)""",
                ),
            AccommodationAnalysisResult.Signal.NATURE_SETTING to
                Regex(
                    """(?<![\p{L}\p{N}])(?:лес[\p{L}]*|озер[\p{L}]*|гор[а-я]*|""" +
                        """природ[\p{L}]*|forest|lake|mountain|nature)(?![\p{L}\p{N}])""",
                ),
            AccommodationAnalysisResult.Signal.STANDARD_HOTEL_FORMAT to
                Regex(
                    """(?<![\p{L}\p{N}])(?:обычн[а-я]*\s+отел[\p{L}]*|""" +
                        """standard hotel|hostel)(?![\p{L}\p{N}])""",
                ),
            AccommodationAnalysisResult.Signal.APARTMENT_BLOCK_FORMAT to
                Regex("""(?<![\p{L}\p{N}])(?:апарт-?отел|апартамент|apartment block)(?![\p{L}\p{N}])"""),
            AccommodationAnalysisResult.Signal.EMPTY_CAMPING_PITCH to
                Regex("""(?:место\s+под\s+палатку|empty camping pitch|tent pitch)"""),
            AccommodationAnalysisResult.Signal.ORDINARY_COTTAGE to
                Regex("""(?:обычн[а-я]*\s+коттедж|standard cottage)"""),
        )
    }
}
