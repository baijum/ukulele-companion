package com.baijum.ukufretboard.domain

data class UkuleleString(
    val name: String,
    val openPitchClass: Int,
    val octave: Int = 4,
)
