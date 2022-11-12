package tororo1066.man10realestatev2.data.enumData

enum class TaxCycle(val amount: Long){
    NONE(0),
    DAY_1(1440),
    DAY_7(1440 * 7),
    DAY_15(1440 * 15),
    DAY_30(1440 * 30),
    MONTH_CHANGED(0)
}