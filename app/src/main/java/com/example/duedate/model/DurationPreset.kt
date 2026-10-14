package com.example.duedate.model

data class DurationPreset(val duration: Long, val unit: DurationUnit) {
    companion object {
        val ALL: List<DurationPreset> = listOf(
            DurationPreset(1, DurationUnit.DAYS),
            DurationPreset(7, DurationUnit.DAYS),
            DurationPreset(14, DurationUnit.DAYS),
            DurationPreset(30, DurationUnit.DAYS),
            DurationPreset(60, DurationUnit.DAYS),
            DurationPreset(90, DurationUnit.DAYS),
            DurationPreset(1, DurationUnit.WEEKS),
            DurationPreset(1, DurationUnit.MONTHS),
            DurationPreset(3, DurationUnit.MONTHS),
            DurationPreset(6, DurationUnit.MONTHS),
            DurationPreset(1, DurationUnit.YEARS)
        )
    }
}
