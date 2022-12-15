package tororo1066.man10realestatev2.listeners

import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import tororo1066.man10realestatev2.Man10RealEstateV2
import tororo1066.man10realestatev2.data.LikeData
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.annotation.SEventHandler

class PlayerLikeListener {

    @SEventHandler
    fun join(e: PlayerJoinEvent){
        Thread {
            val result = SJavaPlugin.mysql.sQuery("select * from like_data where uuid = '${e.player.uniqueId}'")
            Man10RealEstateV2.likeData[e.player.uniqueId] = LikeData.fromDB(result)?:return@Thread
        }.start()
    }

    @SEventHandler
    fun quit(e: PlayerQuitEvent){
        Man10RealEstateV2.likeData.remove(e.player.uniqueId)
    }
}