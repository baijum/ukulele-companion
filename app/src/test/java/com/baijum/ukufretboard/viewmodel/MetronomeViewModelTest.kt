package com.baijum.ukufretboard.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.baijum.ukufretboard.data.BeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MetronomeViewModelTest {

    private lateinit var vm: MetronomeViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        vm = MetronomeViewModel(app)
    }

    @Test
    fun initialStateDefaults() {
        assertEquals(100, vm.bpm.value)
        assertEquals(4, vm.beatsPerMeasure.value)
        assertEquals(1, vm.subdivision.value)
        assertFalse(vm.isPlaying.value)
        assertFalse(vm.isCompound.value)
        assertEquals("4/4", vm.timeSignatureLabel.value)
    }

    @Test
    fun initialAccentPatternFirstBeatAccented() {
        val pattern = vm.accentPattern.value
        assertEquals(4, pattern.size)
        assertEquals(BeatType.ACCENT, pattern[0])
        pattern.drop(1).forEach { assertEquals(BeatType.NORMAL, it) }
    }

    @Test
    fun setBpmCoercesRange() {
        vm.setBpm(10)
        assertEquals(30, vm.bpm.value)

        vm.setBpm(500)
        assertEquals(300, vm.bpm.value)

        vm.setBpm(120)
        assertEquals(120, vm.bpm.value)
    }

    @Test
    fun setTimeSignature68SetsCompoundMode() {
        vm.setTimeSignature("6/8")
        assertTrue(vm.isCompound.value)
        assertEquals(2, vm.beatsPerMeasure.value)
        assertEquals(3, vm.subdivision.value)
        assertEquals("6/8", vm.timeSignatureLabel.value)
    }

    @Test
    fun setTimeSignature128SetsCompoundMode() {
        vm.setTimeSignature("12/8")
        assertTrue(vm.isCompound.value)
        assertEquals(4, vm.beatsPerMeasure.value)
        assertEquals(3, vm.subdivision.value)
    }

    @Test
    fun setTimeSignatureSimpleParsesBeats() {
        vm.setTimeSignature("3/4")
        assertFalse(vm.isCompound.value)
        assertEquals(3, vm.beatsPerMeasure.value)
        assertEquals(1, vm.subdivision.value)
        assertEquals("3/4", vm.timeSignatureLabel.value)
    }

    @Test
    fun setBeatsPerMeasureCoercesAndResetsCompound() {
        vm.setTimeSignature("6/8")
        assertTrue(vm.isCompound.value)

        vm.setBeatsPerMeasure(3)
        assertEquals(3, vm.beatsPerMeasure.value)
        assertFalse("Should reset compound mode", vm.isCompound.value)
        assertEquals("3/4", vm.timeSignatureLabel.value)

        vm.setBeatsPerMeasure(1)
        assertEquals(2, vm.beatsPerMeasure.value)

        vm.setBeatsPerMeasure(10)
        assertEquals(7, vm.beatsPerMeasure.value)
    }

    @Test
    fun setSubdivisionBlockedInCompoundMode() {
        vm.setTimeSignature("6/8")
        assertEquals(3, vm.subdivision.value)
        vm.setSubdivision(2)
        assertEquals("Subdivision change blocked in compound mode", 3, vm.subdivision.value)
    }

    @Test
    fun setSubdivisionCoercesInSimpleMode() {
        vm.setSubdivision(0)
        assertEquals(1, vm.subdivision.value)

        vm.setSubdivision(6)
        assertEquals(4, vm.subdivision.value)

        vm.setSubdivision(2)
        assertEquals(2, vm.subdivision.value)
    }

    @Test
    fun toggleBeatTypeCyclesAccentNormalMute() {
        assertEquals(BeatType.ACCENT, vm.accentPattern.value[0])

        vm.toggleBeatType(0)
        assertEquals(BeatType.NORMAL, vm.accentPattern.value[0])

        vm.toggleBeatType(0)
        assertEquals(BeatType.MUTE, vm.accentPattern.value[0])

        vm.toggleBeatType(0)
        assertEquals(BeatType.ACCENT, vm.accentPattern.value[0])
    }

    @Test
    fun toggleBeatTypeIgnoresOutOfBounds() {
        val before = vm.accentPattern.value.toList()
        vm.toggleBeatType(-1)
        vm.toggleBeatType(99)
        assertEquals(before, vm.accentPattern.value)
    }

    @Test
    fun setBeatsPerMeasureResetsAccentPattern() {
        vm.toggleBeatType(1)
        assertEquals(BeatType.MUTE, vm.accentPattern.value[1])

        vm.setBeatsPerMeasure(5)
        val pattern = vm.accentPattern.value
        assertEquals(5, pattern.size)
        assertEquals(BeatType.ACCENT, pattern[0])
        pattern.drop(1).forEach { assertEquals(BeatType.NORMAL, it) }
    }
}
