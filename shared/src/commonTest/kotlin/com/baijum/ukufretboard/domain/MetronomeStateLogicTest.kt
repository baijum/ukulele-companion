package com.baijum.ukufretboard.domain

import com.baijum.ukufretboard.data.BeatType
import kotlin.test.Test
import kotlin.test.assertEquals

class MetronomeStateLogicTest {
    @Test
    fun clampBpmWithinRange() {
        assertEquals(100, MetronomeStateLogic.clampBpm(100))
        assertEquals(30, MetronomeStateLogic.clampBpm(30))
        assertEquals(300, MetronomeStateLogic.clampBpm(300))
    }

    @Test
    fun clampBpmBelowMinimum() {
        assertEquals(30, MetronomeStateLogic.clampBpm(0))
        assertEquals(30, MetronomeStateLogic.clampBpm(-10))
        assertEquals(30, MetronomeStateLogic.clampBpm(29))
    }

    @Test
    fun clampBpmAboveMaximum() {
        assertEquals(300, MetronomeStateLogic.clampBpm(301))
        assertEquals(300, MetronomeStateLogic.clampBpm(1000))
    }

    @Test
    fun parseTimeSignatureSixEight() {
        val result = MetronomeStateLogic.parseTimeSignature("6/8")
        assertEquals(true, result.isCompound)
        assertEquals(2, result.beatsPerMeasure)
        assertEquals(3, result.subdivision)
    }

    @Test
    fun parseTimeSignatureTwelveEight() {
        val result = MetronomeStateLogic.parseTimeSignature("12/8")
        assertEquals(true, result.isCompound)
        assertEquals(4, result.beatsPerMeasure)
        assertEquals(3, result.subdivision)
    }

    @Test
    fun parseTimeSignatureFourFour() {
        val result = MetronomeStateLogic.parseTimeSignature("4/4")
        assertEquals(false, result.isCompound)
        assertEquals(4, result.beatsPerMeasure)
        assertEquals(1, result.subdivision)
    }

    @Test
    fun parseTimeSignatureThreeFour() {
        val result = MetronomeStateLogic.parseTimeSignature("3/4")
        assertEquals(false, result.isCompound)
        assertEquals(3, result.beatsPerMeasure)
        assertEquals(1, result.subdivision)
    }

    @Test
    fun parseTimeSignatureSevenFour() {
        val result = MetronomeStateLogic.parseTimeSignature("7/4")
        assertEquals(false, result.isCompound)
        assertEquals(7, result.beatsPerMeasure)
        assertEquals(1, result.subdivision)
    }

    @Test
    fun parseTimeSignatureClampsBeatsHigh() {
        val result = MetronomeStateLogic.parseTimeSignature("9/4")
        assertEquals(7, result.beatsPerMeasure)
    }

    @Test
    fun parseTimeSignatureClampsBeatsLow() {
        val result = MetronomeStateLogic.parseTimeSignature("1/4")
        assertEquals(2, result.beatsPerMeasure)
    }

    @Test
    fun parseTimeSignatureInvalidDefaultsFourBeats() {
        val result = MetronomeStateLogic.parseTimeSignature("abc")
        assertEquals(false, result.isCompound)
        assertEquals(4, result.beatsPerMeasure)
        assertEquals(1, result.subdivision)
    }

    @Test
    fun cycleBeatTypeAccentToNormal() {
        assertEquals(BeatType.NORMAL, MetronomeStateLogic.cycleBeatType(BeatType.ACCENT))
    }

    @Test
    fun cycleBeatTypeNormalToMute() {
        assertEquals(BeatType.MUTE, MetronomeStateLogic.cycleBeatType(BeatType.NORMAL))
    }

    @Test
    fun cycleBeatTypeMuteToAccent() {
        assertEquals(BeatType.ACCENT, MetronomeStateLogic.cycleBeatType(BeatType.MUTE))
    }

    @Test
    fun cycleBeatTypeFullLoop() {
        var current = BeatType.ACCENT
        current = MetronomeStateLogic.cycleBeatType(current)
        assertEquals(BeatType.NORMAL, current)
        current = MetronomeStateLogic.cycleBeatType(current)
        assertEquals(BeatType.MUTE, current)
        current = MetronomeStateLogic.cycleBeatType(current)
        assertEquals(BeatType.ACCENT, current)
    }

    @Test
    fun defaultAccentPatternFourBeats() {
        val pattern = MetronomeStateLogic.defaultAccentPattern(4)
        assertEquals(4, pattern.size)
        assertEquals(BeatType.ACCENT, pattern[0])
        assertEquals(BeatType.NORMAL, pattern[1])
        assertEquals(BeatType.NORMAL, pattern[2])
        assertEquals(BeatType.NORMAL, pattern[3])
    }

    @Test
    fun defaultAccentPatternTwoBeats() {
        val pattern = MetronomeStateLogic.defaultAccentPattern(2)
        assertEquals(2, pattern.size)
        assertEquals(BeatType.ACCENT, pattern[0])
        assertEquals(BeatType.NORMAL, pattern[1])
    }

    @Test
    fun defaultAccentPatternOneBeats() {
        val pattern = MetronomeStateLogic.defaultAccentPattern(1)
        assertEquals(1, pattern.size)
        assertEquals(BeatType.ACCENT, pattern[0])
    }

    @Test
    fun clampSubdivisionWithinRange() {
        assertEquals(1, MetronomeStateLogic.clampSubdivision(1))
        assertEquals(2, MetronomeStateLogic.clampSubdivision(2))
        assertEquals(4, MetronomeStateLogic.clampSubdivision(4))
    }

    @Test
    fun clampSubdivisionBelowMinimum() {
        assertEquals(1, MetronomeStateLogic.clampSubdivision(0))
        assertEquals(1, MetronomeStateLogic.clampSubdivision(-5))
    }

    @Test
    fun clampSubdivisionAboveMaximum() {
        assertEquals(4, MetronomeStateLogic.clampSubdivision(5))
        assertEquals(4, MetronomeStateLogic.clampSubdivision(100))
    }
}
