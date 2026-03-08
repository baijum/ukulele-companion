package com.baijum.ukufretboard.domain

/**
 * Calculates finger movements between two chord voicings.
 *
 * Used by the animated chord diagram to show which fingers move
 * and how they transition between chord shapes.
 */
object FingerTransitionCalculator {

    /**
     * A finger position on the fretboard.
     *
     * @property finger Finger number (0 = not used/open, 1 = index, 2 = middle, 3 = ring, 4 = pinky).
     * @property stringIndex String index (0–3 for G, C, E, A).
     * @property fret Fret number (0 = open).
     */
    data class FingerPosition(
        val finger: Int,
        val stringIndex: Int,
        val fret: Int,
    )

    /**
     * Describes how a single finger moves between two voicings.
     *
     * @property finger Finger number (1–4).
     * @property from Position in the source voicing (null if finger is not used).
     * @property to Position in the target voicing (null if finger is not used).
     * @property type Type of movement.
     */
    data class FingerMovement(
        val finger: Int,
        val from: FingerPosition?,
        val to: FingerPosition?,
        val type: MovementType,
    )

    /**
     * Types of finger movement between voicings.
     */
    enum class MovementType {
        /** Finger stays on the same string and fret. */
        STAY,
        /** Finger stays on the same string but changes fret. */
        SLIDE,
        /** Finger moves to a different string. */
        MOVE,
        /** Finger lifts off (was used, now is not). */
        LIFT,
        /** Finger is placed down (was not used, now is). */
        PLACE,
    }

    /**
     * Calculates finger transitions between two chord voicings.
     *
     * Uses [ChordInfo.suggestFingering] to determine which fingers are on
     * which strings, then compares the two assignments.
     *
     * @param fromFrets Fret positions in the source voicing (4 values).
     * @param toFrets Fret positions in the target voicing (4 values).
     * @return List of finger movements describing the transition.
     */
    fun calculateTransition(
        fromFrets: List<Int>,
        toFrets: List<Int>,
    ): List<FingerMovement> {
        val fromFingering = ChordInfo.suggestFingering(fromFrets)
        val toFingering = ChordInfo.suggestFingering(toFrets)

        // Build maps: finger -> (stringIndex, fret) for each voicing
        val fromPositions = buildFingerMap(fromFrets, fromFingering)
        val toPositions = buildFingerMap(toFrets, toFingering)

        // Determine movements for each finger (1–4)
        val movements = mutableListOf<FingerMovement>()

        for (finger in 1..4) {
            val from = fromPositions[finger]
            val to = toPositions[finger]

            val type = when {
                from == null && to == null -> continue // finger not used in either
                from == null && to != null -> MovementType.PLACE
                from != null && to == null -> MovementType.LIFT
                from != null && to != null -> when {
                    from.stringIndex == to.stringIndex && from.fret == to.fret -> MovementType.STAY
                    from.stringIndex == to.stringIndex -> MovementType.SLIDE
                    else -> MovementType.MOVE
                }
                else -> continue
            }

            movements.add(
                FingerMovement(
                    finger = finger,
                    from = from,
                    to = to,
                    type = type,
                ),
            )
        }

        return movements
    }

    /**
     * Builds a map of finger number -> FingerPosition from frets and fingering.
     */
    private fun buildFingerMap(
        frets: List<Int>,
        fingering: List<Int>,
    ): Map<Int, FingerPosition> {
        val map = mutableMapOf<Int, FingerPosition>()
        frets.forEachIndexed { stringIndex, fret ->
            val finger = fingering[stringIndex]
            if (finger > 0 && fret > 0) {
                // If multiple strings use the same finger (barre), keep the first
                if (finger !in map) {
                    map[finger] = FingerPosition(finger, stringIndex, fret)
                }
            }
        }
        return map
    }

    /**
     * Returns a human-readable instruction for a finger movement.
     */
    fun describeMovement(movement: FingerMovement): String {
        val fingerName = when (movement.finger) {
            1 -> "Index"
            2 -> "Middle"
            3 -> "Ring"
            4 -> "Pinky"
            else -> "Finger ${movement.finger}"
        }
        return when (movement.type) {
            MovementType.STAY -> "$fingerName stays"
            MovementType.SLIDE -> {
                val from = movement.from!!
                val to = movement.to!!
                "$fingerName slides to fret ${to.fret}"
            }
            MovementType.MOVE -> {
                val to = movement.to!!
                "$fingerName moves to string ${to.stringIndex + 1}, fret ${to.fret}"
            }
            MovementType.LIFT -> "$fingerName lifts off"
            MovementType.PLACE -> {
                val to = movement.to!!
                "$fingerName places on string ${to.stringIndex + 1}, fret ${to.fret}"
            }
        }
    }
}
