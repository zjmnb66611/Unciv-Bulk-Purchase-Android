package com.unciv.ui.screens.cityscreen

import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.logic.city.BulkPurchase
import com.unciv.models.stats.Stat
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.onClick
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.utils.Concurrency
import com.unciv.ui.components.InputDisabling

/** 安卓可直接编译的城市按钮，不使用反射或 Java Agent。 */
object BulkPurchaseUI {
    fun button(screen: CityScreen): TextButton = "购买队列".toTextButton().apply {
        name = "civic-bulk-purchase"
        isDisabled = screen.cityView.constructions.constructionQueue.isEmpty()
        onClick { if (!isDisabled) confirm(screen) }
    }

    private fun confirm(screen: CityScreen) {
        if (!screen.canChangeState || !screen.cityView.isOwnedByViewer()) return
        @Suppress("DEPRECATION")
        val city = screen.cityView.constructions.getCityConstructions().city
        val queue = city.cityConstructions.constructionQueue.toList()
        val popup = Popup(screen)
        popup.addGoodSizedLabel("购买当前城市队列", 22).row()
        popup.addGoodSizedLabel("现有金币：${city.getStatReserve(Stat.Gold)}\n队列项目：${queue.size}\n" +
            "逐项按原版价格扣费。金币不足、奇观禁购、资源或位置不满足的项目会跳过。\n" +
            "同类单位可能需要先移走已购单位，再购买下一项。只使用金币。").row()
        popup.addCloseButton("取消", KeyboardBinding.Cancel)
        val style = BaseScreen.skin.get("positive", TextButton.TextButtonStyle::class.java)
        popup.addOKButton("确认购买队列", KeyboardBinding.Confirm, style) {
            if (screen.canChangeState && screen.cityView.isOwnedByViewer()) {
                val processor = InputDisabling.disableInput()
                val before = city.getStatReserve(Stat.Gold)
                Concurrency.run("城市队列批量购买") {
                    val outcome = runCatching { BulkPurchase.execute(city, queue) }
                    Concurrency.runOnGLThread {
                        InputDisabling.setInputProcessor(processor)
                        screen.selectedQueueEntry = -1
                        screen.clearSelection()
                        screen.updateAsync()
                        val text = outcome.fold({ result ->
                            buildString {
                                append(result.message)
                                append("\n已购买 ${result.bought} 项，实际扣除 ${result.spent} 金币。")
                                for (entry in result.entries) {
                                    append("\n${entry.name.tr(true)}：")
                                    append(if (entry.purchased) "已购（${entry.cost}金币）" else entry.reason)
                                }
                            }
                        }, {
                            "购买中断，请检查城市队列。已经执行的购买不会回滚。\n" +
                                "实际金币变化：${before - city.getStatReserve(Stat.Gold)}\n${it.javaClass.simpleName}"
                        })
                        Popup(screen).apply {
                            addGoodSizedLabel(text).row()
                            addCloseButton("关闭", KeyboardBinding.Cancel)
                            open(true)
                        }
                    }
                }
            }
        }
        popup.equalizeLastTwoButtonWidths()
        popup.open(true)
    }
}
