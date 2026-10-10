package com.unciv.logic.city

import com.unciv.models.ruleset.INonPerpetualConstruction
import com.unciv.models.stats.Stat

/** 正常金币购买；固定处理确认时的队列，不购买原版自动补入的项目。 */
object BulkPurchase {
    data class Entry(val name: String, val purchased: Boolean, val cost: Int, val reason: String)
    data class Result(val spent: Int, val entries: List<Entry>, val message: String) {
        val bought: Int get() = entries.count { it.purchased }
    }

    fun execute(city: City, expectedQueue: List<String>): Result {
        val constructions = city.cityConstructions
        val snapshot = constructions.constructionQueue.toList()
        if (snapshot != expectedQueue)
            return Result(0, emptyList(), "队列已变化，请重新打开购买窗口。")
        if (city.civ.gameInfo.gameParameters.godMode)
            return Result(0, emptyList(), "请先关闭神模式，批量购买要求正常扣除金币。")

        val entries = ArrayList<Entry>()
        val initialGold = city.getStatReserve(Stat.Gold)
        for (name in snapshot) {
            val index = constructions.constructionQueue.indexOf(name)
            if (index < 0) {
                entries.add(Entry(name, false, 0, "已被原版队列校验移除"))
                continue
            }
            val item = constructions.getConstruction(name)
            if (item !is INonPerpetualConstruction) {
                entries.add(Entry(name, false, 0, "持续项目不能购买"))
                continue
            }
            val cost = item.getStatBuyCost(city, Stat.Gold)
            if (cost == null || !item.canBePurchasedWithStat(city, Stat.Gold)) {
                entries.add(Entry(name, false, 0, "不支持金币购买"))
                continue
            }
            if (!constructions.isConstructionPurchaseAllowed(item, Stat.Gold, cost)) {
                val reason = when {
                    city.getStatReserve(Stat.Gold) < cost -> "金币不足"
                    constructions.isConstructionPurchaseBlockedByUnit(item) -> "单位没有可放置的位置"
                    else -> "城市状态、资源或前置条件不满足"
                }
                entries.add(Entry(name, false, 0, reason))
                continue
            }
            val before = city.getStatReserve(Stat.Gold)
            val success = constructions.purchaseConstruction(item, index, false, Stat.Gold, null)
            entries.add(Entry(name, success, before - city.getStatReserve(Stat.Gold),
                if (success) "已购买" else "原版购买未完成"))
        }
        return Result(initialGold - city.getStatReserve(Stat.Gold), entries.toList(), "批量购买完成")
    }
}
