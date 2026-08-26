package com.thiago.transcribetranslate.export

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object SrtExporter {
    fun export(cues: List<SubtitleCue>): String =
        cues.mapIndexed { index, cue ->
            "${index + 1}\n${format(cue.startMs)} --> ${format(cue.endMs)}\n${cue.text}\n"
        }.joinToString("\n")

    private fun format(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1_000
        val millis = ms % 1_000
        return "%02d:%02d:%02d,%03d".format(h, m, s, millis)
    }
}
