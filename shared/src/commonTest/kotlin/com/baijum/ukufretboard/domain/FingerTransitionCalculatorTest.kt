package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.domain.FingerTransitionCalculator.MovementType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FingerTransitionCalculatorTest {

    // --- calculateTransition: same voicing ---

    @Test
    fun sameVoicingAllStay() {
        val frets = listOf(0, 0, 0, 3)
        val movements = FingerTransitionCalculator.calculateTransition(frets, frets)
        // Only fingers that are actually used should appear
        val usedMovements = movements.filter { it.type != MovementType.STAY || it.from != null }
        for (m in usedMovements) {
            assertEquals(MovementType.STAY, m.type)
        }
    }

    @Test
    fun allOpenToAllOpenNoMovements() {
        val frets = listOf(0, 0, 0, 0)
        val movements = FingerTransitionCalculator.calculateTransition(frets, frets)
        // No fingers used in either voicing — should be empty
        assertTrue(movements.isEmpty())
    }

    // --- calculateTransition: PLACE ---

    @Test
    fun openToFrettedIsPlace() {
        val from = listOf(0, 0, 0, 0)
        val to = listOf(0, 0, 0, 3)
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        assertTrue(movements.any { it.type == MovementType.PLACE })
    }

    @Test
    fun openToMultipleFrettedPlacesMultipleFingers() {
        val from = listOf(0, 0, 0, 0)
        val to = listOf(2, 0, 1, 0) // F major
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        val places = movements.filter { it.type == MovementType.PLACE }
        assertTrue(places.size >= 2, "Should place at least 2 fingers")
    }

    // --- calculateTransition: LIFT ---

    @Test
    fun frettedToOpenIsLift() {
        val from = listOf(0, 0, 0, 3)
        val to = listOf(0, 0, 0, 0)
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        assertTrue(movements.any { it.type == MovementType.LIFT })
    }

    // --- calculateTransition: SLIDE ---

    @Test
    fun sameStringSlidesToDifferentFret() {
        // Both voicings use string 3 (A), but different frets
        val from = listOf(0, 0, 0, 3) // C major
        val to = listOf(0, 0, 0, 5)
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        // The finger on string 3 should slide
        val slides = movements.filter { it.type == MovementType.SLIDE }
        assertTrue(slides.isNotEmpty(), "Expected a slide movement")
    }

    // --- calculateTransition: MOVE ---

    @Test
    fun fingerMovesToDifferentString() {
        // From: finger on string 0 fret 2; To: finger on string 2 fret 1
        val from = listOf(2, 0, 0, 0)
        val to = listOf(0, 0, 1, 0)
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        // One finger lifts and another places (or a move if same finger number)
        assertTrue(movements.isNotEmpty())
    }

    // --- calculateTransition: mixed movements ---

    @Test
    fun cToAmTransition() {
        val cMajor = listOf(0, 0, 0, 3) // C major: finger 1 on string 3
        val aMinor = listOf(2, 0, 0, 0) // Am: finger 1 on string 0
        val movements = FingerTransitionCalculator.calculateTransition(cMajor, aMinor)
        assertTrue(movements.isNotEmpty(), "C to Am should have finger movements")
        // Both voicings use only finger 1 (single fretted string), so the finger MOVEs
        val moves = movements.filter { it.type == MovementType.MOVE }
        assertTrue(moves.isNotEmpty(), "C to Am should involve a MOVE (finger 1 changes string)")
    }

    @Test
    fun complexTransitionHasValidFingerNumbers() {
        val from = listOf(0, 2, 3, 2) // G major
        val to = listOf(2, 0, 1, 0)   // F major
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        for (m in movements) {
            assertTrue(m.finger in 1..4, "Finger number should be 1-4, got ${m.finger}")
        }
    }

    // --- calculateTransition: from/to positions ---

    @Test
    fun placeMovementHasNullFromAndValidTo() {
        val from = listOf(0, 0, 0, 0)
        val to = listOf(0, 0, 0, 3)
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        val places = movements.filter { it.type == MovementType.PLACE }
        for (p in places) {
            assertNull(p.from, "PLACE should have null from")
            assertNotNull(p.to, "PLACE should have non-null to")
        }
    }

    @Test
    fun liftMovementHasValidFromAndNullTo() {
        val from = listOf(0, 0, 0, 3)
        val to = listOf(0, 0, 0, 0)
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        val lifts = movements.filter { it.type == MovementType.LIFT }
        for (l in lifts) {
            assertNotNull(l.from, "LIFT should have non-null from")
            assertNull(l.to, "LIFT should have null to")
        }
    }

    @Test
    fun stayMovementHasSameFromAndTo() {
        val frets = listOf(0, 0, 0, 3)
        val movements = FingerTransitionCalculator.calculateTransition(frets, frets)
        val stays = movements.filter { it.type == MovementType.STAY }
        for (s in stays) {
            assertNotNull(s.from)
            assertNotNull(s.to)
            assertEquals(s.from!!.fret, s.to!!.fret)
            assertEquals(s.from!!.stringIndex, s.to!!.stringIndex)
        }
    }

    // --- calculateTransition: muted strings ---

    @Test
    fun mutedStringTreatedAsOpen() {
        // Muted (-1) should be treated like open (no finger)
        val from = listOf(ChordVoicing.MUTED, 0, 0, 3)
        val to = listOf(ChordVoicing.MUTED, 0, 0, 3)
        val movements = FingerTransitionCalculator.calculateTransition(from, to)
        // Only finger on string 3 should be STAY
        val stays = movements.filter { it.type == MovementType.STAY }
        assertTrue(stays.isNotEmpty())
    }

    // --- describeMovement() ---

    @Test
    fun describeStayContainsStays() {
        val movement = FingerTransitionCalculator.FingerMovement(
            finger = 1,
            from = FingerTransitionCalculator.FingerPosition(1, 0, 2),
            to = FingerTransitionCalculator.FingerPosition(1, 0, 2),
            type = MovementType.STAY,
        )
        val desc = FingerTransitionCalculator.describeMovement(movement)
        assertTrue(desc.contains("stays"), "Description should contain 'stays': $desc")
        assertTrue(desc.contains("Index"), "Should mention 'Index' finger: $desc")
    }

    @Test
    fun describeSlideContainsSlides() {
        val movement = FingerTransitionCalculator.FingerMovement(
            finger = 2,
            from = FingerTransitionCalculator.FingerPosition(2, 1, 2),
            to = FingerTransitionCalculator.FingerPosition(2, 1, 4),
            type = MovementType.SLIDE,
        )
        val desc = FingerTransitionCalculator.describeMovement(movement)
        assertTrue(desc.contains("slides"), "Description should contain 'slides': $desc")
        assertTrue(desc.contains("Middle"), "Should mention 'Middle' finger: $desc")
        assertTrue(desc.contains("4"), "Should mention target fret: $desc")
    }

    @Test
    fun describeMoveContainsMoves() {
        val movement = FingerTransitionCalculator.FingerMovement(
            finger = 3,
            from = FingerTransitionCalculator.FingerPosition(3, 0, 2),
            to = FingerTransitionCalculator.FingerPosition(3, 2, 1),
            type = MovementType.MOVE,
        )
        val desc = FingerTransitionCalculator.describeMovement(movement)
        assertTrue(desc.contains("moves"), "Description should contain 'moves': $desc")
        assertTrue(desc.contains("Ring"), "Should mention 'Ring' finger: $desc")
    }

    @Test
    fun describeLiftContainsLifts() {
        val movement = FingerTransitionCalculator.FingerMovement(
            finger = 4,
            from = FingerTransitionCalculator.FingerPosition(4, 3, 5),
            to = null,
            type = MovementType.LIFT,
        )
        val desc = FingerTransitionCalculator.describeMovement(movement)
        assertTrue(desc.contains("lifts"), "Description should contain 'lifts': $desc")
        assertTrue(desc.contains("Pinky"), "Should mention 'Pinky' finger: $desc")
    }

    @Test
    fun describePlaceContainsPlaces() {
        val movement = FingerTransitionCalculator.FingerMovement(
            finger = 1,
            from = null,
            to = FingerTransitionCalculator.FingerPosition(1, 2, 3),
            type = MovementType.PLACE,
        )
        val desc = FingerTransitionCalculator.describeMovement(movement)
        assertTrue(desc.contains("places"), "Description should contain 'places': $desc")
        assertTrue(desc.contains("Index"), "Should mention 'Index' finger: $desc")
    }

    @Test
    fun allFingerNamesCorrect() {
        val names = mapOf(1 to "Index", 2 to "Middle", 3 to "Ring", 4 to "Pinky")
        for ((finger, name) in names) {
            val movement = FingerTransitionCalculator.FingerMovement(
                finger = finger,
                from = FingerTransitionCalculator.FingerPosition(finger, 0, 1),
                to = FingerTransitionCalculator.FingerPosition(finger, 0, 1),
                type = MovementType.STAY,
            )
            val desc = FingerTransitionCalculator.describeMovement(movement)
            assertTrue(desc.contains(name), "Finger $finger should be named '$name': $desc")
        }
    }
}
