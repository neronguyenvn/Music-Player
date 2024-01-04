package org.hyperskill.musicplayer.helper

import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun formatMilliseconds(milliseconds: Int): String {
    val duration = milliseconds.toDuration(DurationUnit.MILLISECONDS)
    val minutes = duration.inWholeMinutes
    val seconds = duration.minus(minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds
    return "%02d:%02d".format(minutes, seconds)
}