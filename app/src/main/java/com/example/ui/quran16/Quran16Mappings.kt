package com.example.ui.quran16

/** Original supplied 1-based PDF pages; never sorted or inferred from page lengths. */
object Quran16Mappings {
    val start = listOf(2,21,39,57,75,93,111,129,147,165,183,201,219,237,255,273,291,309,327,345,363,381,399,417,435,453,471,489,509,529)
    val ruba = listOf(8,25,43,61,79,97,115,132,151,169,187,205,223,241,259,277,295,313,331,349,367,385,403,421,439,457,475,494,513,534)
    val nisf = listOf(12,30,48,66,84,102,120,132,155,174,192,209,227,245,264,282,299,318,336,354,372,389,407,425,443,463,480,498,519,539)
    val salasah = listOf(16,35,52,70,88,106,124,141,160,178,196,214,232,250,268,286,302,322,340,358,377,394,412,430,448,466,484,504,524,544)
    val surahStart = listOf(2,3,46,70,97,112,137,160,149,188,200,213,225,231,236,241,255,265,276,282,291,300,309,316,325,331,340,348,384,365,371,374,377,386,392,397,402,409,413,421,430,435,441,447,449,453,457,461,464,467,469,472,474,476,479,482,485,489,492,496,498,500,501,503,505,507,509,511,513,515,517,519,521,522,524,525,527,529,530,531,533,533,534,535,536,537,538,538,539,540,541,541,542,542,543,543,544,544,545,545,545,546,546,546,547,547,547,547,548,548,548,548,549,549)
    // Visually verified headings AND opening ayahs in the actual 560-page source PDF.
    // SHA256: 1c7e5fb27a7b94b6184eb04fbddfe3658abd328df338d4aa6afc7dd76af3f088
    // Original supplied values remain above for auditability.
    val verifiedSurahStarts = mapOf(9 to 169, 29 to 358)
    val labels = listOf("Read from Start", "Ruba (First 1/4)", "Nisf (Half)", "Salasah (Last 1/4)")
    fun juz(number: Int, position: Int): Int? = listOf(start, ruba, nisf, salasah).getOrNull(position)?.getOrNull(number - 1)
    fun surah(number: Int, position: Int): Int? = if (position == 0) verifiedSurahStarts[number] ?: surahStart.getOrNull(number - 1) else null
    fun valid(page: Int, count: Int) = page in 1..count
    fun juzForPage(page: Int): Int {
        for (i in start.indices.reversed()) {
            if (page >= start[i]) return i + 1
        }
        return 1
    }
}
