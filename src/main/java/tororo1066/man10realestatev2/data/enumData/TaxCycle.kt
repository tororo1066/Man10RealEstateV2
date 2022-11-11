package tororo1066.man10realestatev2.data.enumData

enum class TaxCycle(val amount: Long){
    NONE(0),
    DAY_1(1440000),
    DAY_7(1440000 * 7),
    DAY_15(1440000 * 15),
    DAY_30(1440000 * 30),
    MONTH_CHANGED(0)
}