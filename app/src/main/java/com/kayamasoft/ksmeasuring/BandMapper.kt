object BandMapper {
    fun getLteBand(earfcn: Int): String = when (earfcn) {
        in 0..599 -> "B1"
        in 600..1199 -> "B2"
        in 1200..1949 -> "B3"
        in 1950..2399 -> "B4"
        in 2400..2649 -> "B5"
        in 2750..3449 -> "B20"
        in 3450..3799 -> "B7"
        in 5850..5999 -> "B18"
        in 6150..6449 -> "B8"
        in 6450..6599 -> "B11"
        in 7500..7699 -> "B19"
        in 7700..8039 -> "B21"
        in 8600..9039 -> "B26"
        in 9200..9650 -> "B28"
        else -> "Unknown"
    }

    fun getNrBand(nrarfcn: Int): String = when (nrarfcn) {
        in 620000..653333 -> "n78"
        in 151600..160600 -> "n28"
        in 2016667..2026666 -> "n77"
        in 2054166..2099165 -> "n257"
        in 2070833..2083333 -> "n258"
        else -> "Unknown"
    }
}