package com.unciv.logic.city

import com.unciv.models.stats.Stat
import com.unciv.testing.BaseTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(BaseTestRunner::class)
class BulkPurchaseTest {
    private fun fixture(): Pair<TestGame, City> {
        val game = TestGame()
        game.makeHexagonalMap(5, "Grassland")
        val civ = game.addCiv(isPlayer = true)
        civ.tech.addTechnology("Pottery")
        civ.tech.addTechnology("Writing")
        val city = game.addCity(civ, game.getTile(0, 0), initialPopulation = 8)
        civ.addGold(100000 - civ.gold)
        return Pair(game, city)
    }

    private fun queue(city: City, vararg names: String) {
        city.cityConstructions.constructionQueue.clear()
        city.cityConstructions.constructionQueue.addAll(names)
    }

    private fun buy(city: City) = BulkPurchase.execute(city, city.cityConstructions.constructionQueue.toList())

    @Test fun purchasesBuildingsAndUnitsWithActualGold() {
        val (_, city) = fixture()
        queue(city, "Granary", "Library", "Worker", "Warrior", "Worker")
        val before = city.civ.gold
        val result = buy(city)
        assertEquals(result.toString(), 4, result.bought)
        assertEquals(before - city.civ.gold, result.spent)
        assertTrue(result.spent > 0)
        assertTrue(city.cityConstructions.isBuilt("Granary"))
        assertTrue(city.cityConstructions.isBuilt("Library"))
        assertEquals(2, city.civ.units.getCivUnits().count())
        assertTrue(city.cityConstructions.constructionQueue.contains("Worker"))
    }

    @Test fun staleQueueDoesNotCharge() {
        val (_, city) = fixture()
        queue(city, "Granary")
        val before = city.civ.gold
        val result = BulkPurchase.execute(city, listOf("Library"))
        assertEquals(0, result.bought)
        assertEquals(before, city.civ.gold)
    }

    @Test fun insufficientGoldPreservesQueue() {
        val (_, city) = fixture()
        city.civ.addGold(-city.civ.gold)
        queue(city, "Library", "Worker")
        assertEquals(0, buy(city).bought)
        assertEquals(0, city.civ.gold)
        assertEquals(listOf("Library", "Worker"), city.cityConstructions.constructionQueue)
    }

    @Test fun spendsAvailableGoldAndSkipsRemainder() {
        val (game, city) = fixture()
        val cost = game.ruleset.buildings["Granary"]!!.getStatBuyCost(city, Stat.Gold)!!
        city.civ.addGold(cost - city.civ.gold)
        queue(city, "Granary", "Library")
        val result = buy(city)
        assertEquals(1, result.bought)
        assertEquals(cost, result.spent)
        assertEquals(0, city.civ.gold)
        assertTrue(city.cityConstructions.constructionQueue.contains("Library"))
    }

    @Test fun skipsWonderAndPerpetualProject() {
        val (_, city) = fixture()
        queue(city, "Great Library", "Granary", "Gold")
        val result = buy(city)
        assertEquals(1, result.bought)
        assertFalse(city.cityConstructions.isBuilt("Great Library"))
        assertTrue(city.cityConstructions.isBuilt("Granary"))
    }

    @Test fun respectsPuppetCityRestrictions() {
        val (_, city) = fixture()
        city.isPuppet = true
        queue(city, "Library")
        assertEquals(0, buy(city).bought)
    }

    @Test fun godModeDoesNotPretendToPayGold() {
        val (game, city) = fixture()
        game.gameInfo.gameParameters.godMode = true
        queue(city, "Library")
        val before = city.civ.gold
        assertEquals(0, buy(city).bought)
        assertEquals(before, city.civ.gold)
    }

    @Test fun blockedUnitDoesNotStopLaterBuilding() {
        val (_, city) = fixture()
        queue(city, "Modern Armor", "Library")
        assertEquals(1, buy(city).bought)
        assertTrue(city.cityConstructions.isBuilt("Library"))
    }
}
