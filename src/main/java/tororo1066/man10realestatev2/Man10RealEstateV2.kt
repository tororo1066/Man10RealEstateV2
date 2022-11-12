package tororo1066.man10realestatev2

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import red.man10.man10bank.BankAPI
import tororo1066.man10realestatev2.data.CityData
import tororo1066.man10realestatev2.data.RegionData
import tororo1066.man10realestatev2.data.enumData.TaxCycle
import tororo1066.tororopluginapi.SInput
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.SStr
import tororo1066.tororopluginapi.utils.toCalender
import java.io.File
import java.util.Calendar
import java.util.Date

class Man10RealEstateV2: SJavaPlugin(UseOption.MySQL,UseOption.SConfig,UseOption.Vault) {

    companion object{
        lateinit var plugin: Man10RealEstateV2
        lateinit var bank: BankAPI
        lateinit var sInput: SInput
        val cityData = HashMap<String,CityData>()
        val prefix = SStr("&4[&dMan10RealEstate&eV2&4]&r").toString()

        fun inRegion(loc: Location, start: Triple<Int,Int,Int>, end: Triple<Int,Int,Int>): Boolean{
            fun contains(start: Int, end: Int, loc: Int) = IntProgression.fromClosedRange(start,end,if (start - end >= 0) -1 else 1).contains(loc)
            return contains(start.first,end.first,loc.blockX) && contains(start.second,end.second,loc.blockY) && contains(start.third,end.third,loc.blockZ)
        }

        fun CommandSender.sendPrefixMsg(str: SStr){
            this.sendMessage(prefix + str)
        }

    }

    override fun onStart() {
        saveDefaultConfig()
        plugin = this
        sInput = SInput(this)
        bank = BankAPI(this)
        createTables()

        File(dataFolder.path + "/city/").listFiles()?.forEach {
            if (it.extension != "yml")return@forEach
            cityData[it.nameWithoutExtension] = CityData.loadFromYml(it)
        }

        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            val now = Date()
            cityData.values.forEach {
                it.regions.values.forEach second@ { region ->
                    if (region.ownerUUID == null)return@second
                    //Rent
                    //ここで賃料を払う必要があるか確認する
                    val rentInfo = when(region.rentCycle){
                        RegionData.RentCycle.NONE->false
                        RegionData.RentCycle.DAY_1-> now.time - region.lastRent * 1000 > 1440000
                        RegionData.RentCycle.DAY_7-> now.time - region.lastRent * 1000 > 1440000 * 7
                        RegionData.RentCycle.DAY_15-> now.time - region.lastRent * 1000 > 1440000 * 15
                        RegionData.RentCycle.DAY_30-> now.time - region.lastRent * 1000 > 1440000 * 30
                        RegionData.RentCycle.MONTH_CHANGED-> now.toCalender()[Calendar.MONTH] != Date(region.lastRent * 1000).toCalender()[Calendar.MONTH]
                    }

                    if (rentInfo){
                        //全員に賃料を支払わせる
                        region.players.values.forEach third@ { user ->
                            if (user.rent == 0.0)return@third
                            if (bank.withdraw(user.uuid,user.rent,"mrev2 payment rent","賃料の支払い(${region.includeName})")){
                                bank.deposit(region.ownerUUID!!,user.rent,"mrev2 receive rent","賃料の受け取り(${region.includeName})")
                            } else {
                                if (mysql.asyncExecute("delete from user_data where region_id = '${region.includeName}' and uuid = '${user.uuid}'")){
                                    region.players.remove(user.uuid)
                                }
                            }

                        }
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.MILLISECOND,0)
                        calendar.set(Calendar.SECOND,0)
                        calendar.set(Calendar.MINUTE,0)
                        calendar.set(Calendar.HOUR_OF_DAY,0)

                        //最後に払った賃料の時間を変数に保存させる
                        val lastRentValue = if (region.rentCycle == RegionData.RentCycle.MONTH_CHANGED) calendar.time.time / 1000 else region.lastRent + region.rentCycle.amount
                        if (mysql.asyncExecute("update region_data set lastRent = $lastRentValue where includeName = '${region.includeName}'")){
                            region.lastRent = lastRentValue
                        }
                    }

                    //Tax
                    fun mustTax(taxCycle: TaxCycle) = when(taxCycle){
                        TaxCycle.NONE->null
                        TaxCycle.DAY_1-> now.time - region.lastTax * 1000 > 1440000
                        TaxCycle.DAY_7-> now.time - region.lastTax * 1000 > 1440000 * 7
                        TaxCycle.DAY_15-> now.time - region.lastTax * 1000 > 1440000 * 15
                        TaxCycle.DAY_30-> now.time - region.lastTax * 1000 > 1440000 * 30
                        TaxCycle.MONTH_CHANGED-> now.toCalender()[Calendar.MONTH] != Date(region.lastTax * 1000).toCalender()[Calendar.MONTH]
                    }

                    var taxInfo = mustTax(region.taxCycle)

                    if (taxInfo == null){
                        taxInfo = mustTax(it.taxCycle)?:return@second
                    }

                    if (taxInfo){
                        if (region.state == RegionData.State.LOCK){
                            if (mysql.asyncExecute("update region_data set price = ${region.defaultPrice}, state = '${RegionData.State.ONSALE}' where region_id = '${region.includeName}'")){
                                region.price = region.defaultPrice
                                region.state = RegionData.State.ONSALE
                            }
                        }
                        fun lock(){
                            if (mysql.asyncExecute("update region_data set price = ${region.defaultPrice * 3}, state = '${RegionData.State.LOCK}' where region_id = '${region.includeName}'")){
                                region.price = if (region.tax != null) region.tax!! * 3 else it.tax * 3
                                region.state = RegionData.State.LOCK
                            }
                        }
                        if (region.tax != null){
                            if (region.tax != 0.0){
                                if (!bank.withdraw(region.ownerUUID!!,region.tax!!,"mrev2 tax","税金の支払い(${region.includeName})")){
                                    lock()
                                }
                            }
                        } else {
                            if (it.tax != 0.0){
                                if (!bank.withdraw(region.ownerUUID!!,it.tax,"mrev2 tax","税金の支払い(${region.includeName})")){
                                    lock()
                                }
                            }
                        }
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.MILLISECOND,0)
                        calendar.set(Calendar.SECOND,0)
                        calendar.set(Calendar.MINUTE,0)
                        calendar.set(Calendar.HOUR_OF_DAY,0)

                        val lastTaxValue = if (region.tax != null){
                            if (region.taxCycle == TaxCycle.MONTH_CHANGED) calendar.time.time / 1000 else region.lastTax + region.taxCycle.amount
                        } else {
                            if (it.taxCycle == TaxCycle.MONTH_CHANGED) calendar.time.time / 1000 else region.lastTax + it.taxCycle.amount
                        }

                        if (mysql.asyncExecute("update region_data set lastTax = $lastTaxValue where includeName = '${region.includeName}'")){
                            region.lastTax = lastTaxValue
                        }
                    }
                }
            }
        },0,1200)
    }

    fun createTables(){
        mysql.asyncExecute("CREATE TABLE IF NOT EXISTS `region_data` (\n" +
                "\t`id` INT(10) NOT NULL AUTO_INCREMENT,\n" +
                "\t`city` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`includeName` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`displayName` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`tax` DOUBLE NULL DEFAULT NULL,\n" +
                "\t`userLimit` INT(10) NULL DEFAULT NULL,\n" +
                "\t`buyScore` INT(10) NULL DEFAULT NULL,\n" +
                "\t`liveScore` INT(10) NULL DEFAULT NULL,\n" +
                "\t`defaultPrice` DOUBLE NULL DEFAULT NULL,\n" +
                "\t`startLoc` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`endLoc` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`price` DOUBLE NULL DEFAULT NULL,\n" +
                "\t`ownerUUID` VARCHAR(36) NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`state` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`rentCycle` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`lastTax` BIGINT(19) NULL DEFAULT NULL,\n" +
                "\t`taxCycle` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`lastRent` BIGINT(19) NULL DEFAULT NULL,\n" +
                "\t`teleportLoc` TEXT NULL DEFAULT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\tPRIMARY KEY (`id`) USING BTREE\n" +
                ")\n" +
                "COLLATE='utf8mb4_0900_ai_ci'\n" +
                "ENGINE=InnoDB\n" +
                ";\n")
        mysql.asyncExecute("CREATE TABLE IF NOT EXISTS `user_data` (\n" +
                "\t`id` INT(10) NOT NULL AUTO_INCREMENT,\n" +
                "\t`region_id` TEXT NOT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`uuid` VARCHAR(36) NOT NULL DEFAULT '' COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`mcid` VARCHAR(16) NOT NULL DEFAULT '' COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`perms` TEXT NOT NULL COLLATE 'utf8mb4_0900_ai_ci',\n" +
                "\t`rent` DOUBLE NULL DEFAULT NULL,\n" +
                "\tPRIMARY KEY (`id`) USING BTREE\n" +
                ")\n" +
                "COLLATE='utf8mb4_0900_ai_ci'\n" +
                "ENGINE=InnoDB\n" +
                ";")
    }
}