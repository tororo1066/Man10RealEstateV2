package tororo1066.man10realestatev2.data.enumData

enum class TaxCycle(val displayName: String, val amount: Long){
    NONE("無し",0),
    DAY_1("1日ごと",1440),
    DAY_7("1週間ごと",1440 * 7),
    DAY_15("15日ごと",1440 * 15),
    DAY_30("30日ごと",1440 * 30),
    MONTH_CHANGED("月の変わり目",0)
}