package com.classroom.quizmaster.agents

import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.MatchingPair
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.TrueFalseItem
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Sources for real-world figures referenced in these items:
 * - Bangko Sentral ng Pilipinas Monetary Board policy rate (6.50% RRP, October 2023 - August 2024).
 * - Bureau of the Treasury: Retail Treasury Bond 29 (RTB-29) coupon rate announcement, February 2024.
 * - World Bank Open Data (accessed 2025-10-16) for GDP growth, population, inflation, and female population share.
 */
class ItemBankAgentImpl : ItemBankAgent {
    private val items = ConcurrentHashMap<String, Item>()

    init {
        seededItems().forEach { item ->
            items[item.id] = item
        }
    }

    override fun query(objectives: List<String>, limit: Int): List<Item> {
        val objectiveSet = objectives.toSet()
        return items.values.filter { it.objective in objectiveSet }.take(limit)
    }

    override fun upsert(items: List<Item>): Result<Unit> {
        items.forEach { item -> this.items[item.id.ifBlank { UUID.randomUUID().toString() }] = item }
        return Result.success(Unit)
    }

    private fun seededItems(): List<Item> {
        val list = mutableListOf<Item>()
        fun add(item: Item) = list.add(item)

        // Financial literacy (LO1)
        add(
            MultipleChoiceItem(
                id = "lo1-mc-rtb29",
                objective = "LO1",
                prompt = "The Bureau of the Treasury's RTB-29 pays 6.125% annual interest, compounded semiannually. " +
                    "If you invest Php 50,000 and hold the bond for two full years, about how much will the investment be worth at maturity?",
                choices = listOf("Php 56,200", "Php 56,412", "Php 56,850", "Php 57,500"),
                correctIndex = 1,
                explanation = "A = 50,000(1 + 0.06125/2)^(2*2) is approximately Php 56,412 using compound interest."
            )
        )
        add(
            NumericItem(
                id = "lo1-num-bsp-rrp",
                objective = "LO1",
                prompt = "The Bangko Sentral ng Pilipinas kept the overnight reverse repurchase (RRP) rate at 6.50%. " +
                    "Estimate the simple interest earned on Php 120,000 placed in an RRP-pegged time deposit for 9 months.",
                answer = 120000 * 0.065 * 0.75,
                tolerance = 1.0,
                explanation = "Simple interest I = P*r*t = 120,000 * 0.065 * 0.75 is about Php 5,850."
            )
        )
        add(
            TrueFalseItem(
                id = "lo1-tf-compared",
                objective = "LO1",
                prompt = "A two-year RTB-29 investment at 6.125% compounded semiannually grows faster than a simple-interest deposit " +
                    "priced off the 6.50% RRP rate for the same term.",
                answer = false,
                explanation = "Simple interest at 6.50% over two years yields 50,000 * 0.065 * 2 = Php 6,500, " +
                    "so the maturity is 56,500, slightly higher than the RTB-29's Php 56,412."
            )
        )

        // Sequences & growth modelling (LO2)
        val pop2020 = 112_081_264.0
        val pop2024 = 115_843_670.0
        val annualRatio = Math.pow(pop2024 / pop2020, 0.25)
        add(
            MultipleChoiceItem(
                id = "lo2-mc-pop-growth",
                objective = "LO2",
                prompt = "World Bank population estimates show 112,081,264 Filipinos in 2020 and 115,843,670 in 2024. " +
                    "Assuming geometric growth, what is the approximate annual percent increase?",
                choices = listOf("0.50%", "0.83%", "1.5%", "2.8%"),
                correctIndex = 1,
                explanation = "Annual factor is roughly (115,843,670 / 112,081,264)^(1/4) which is about 1.0083, or about 0.83%."
            )
        )
        add(
            NumericItem(
                id = "lo2-num-pop-projection",
                objective = "LO2",
                prompt = "Using the same geometric growth factor from 2020-2024 World Bank population data, " +
                    "project the 2025 population of the Philippines.",
                answer = pop2024 * annualRatio,
                tolerance = 5_000.0,
                explanation = "P5 is about 115,843,670 * 1.0083 which equals roughly 116,803,842."
            )
        )
        val gdp2022 = 7.58098212785563
        val gdp2023 = 5.51894967862863
        val gdp2024 = 5.69201612823412
        add(
            MultipleChoiceItem(
                id = "lo2-mc-gdp-diff",
                objective = "LO2",
                prompt = "World Bank GDP growth data report 7.58% (2022), 5.52% (2023), and 5.69% (2024) for the Philippines. " +
                    "What is the change in growth rate from 2022 to 2023?",
                choices = listOf("-1.09 percentage points", "-2.06 percentage points", "-2.70 percentage points", "-3.50 percentage points"),
                correctIndex = 1,
                explanation = "5.52 - 7.58 is about -2.06 percentage points."
            )
        )

        // Data analysis & statistics (LO3)
        val inflation2024 = 3.21260487006343
        val inflation2023 = 5.97802515541414
        val inflation2022 = 5.82115811213955
        add(
            NumericItem(
                id = "lo3-num-average-inflation",
                objective = "LO3",
                prompt = "Compute the three-year average of Philippine consumer inflation rates for 2022 (5.82%), 2023 (5.98%), and 2024 (3.21%) from World Bank data.",
                answer = (inflation2022 + inflation2023 + inflation2024) / 3.0,
                tolerance = 0.01,
                explanation = "Average is roughly (5.8212 + 5.9780 + 3.2126) / 3 which is about 5.00%."
            )
        )
        add(
            NumericItem(
                id = "lo3-num-inflation-price",
                objective = "LO3",
                prompt = "A grocery basket cost Php 950 in 2022. Adjust it using the 2023 World Bank inflation rate of 5.98% to estimate the 2023 cost.",
                answer = 950 * (1 + inflation2023 / 100.0),
                tolerance = 0.5,
                explanation = "Adjusted price is 950 * 1.0598 which is close to Php 1,006.79."
            )
        )
        val avgGdp = (gdp2022 + gdp2023 + gdp2024) / 3.0
        add(
            NumericItem(
                id = "lo3-num-mean-gdp-growth",
                objective = "LO3",
                prompt = "Find the mean of the Philippine GDP growth rates for 2022, 2023, and 2024 reported by the World Bank.",
                answer = avgGdp,
                tolerance = 0.01,
                explanation = "Mean is roughly (7.58 + 5.52 + 5.69) / 3 which is about 6.26%."
            )
        )
        val femaleShare2024 = 50.1188912609554
        val femaleProbability = femaleShare2024 / 100.0
        add(
            MultipleChoiceItem(
                id = "lo3-mc-female-prob",
                objective = "LO3",
                prompt = "World Bank estimates that females made up 50.12% of the Philippine population in 2024. " +
                    "If a resident is selected at random, what is the probability the person is female?",
                choices = listOf("0.49", "0.50", "0.55", "0.60"),
                correctIndex = 1,
                explanation = "Probability is about 50.12% / 100 which equals roughly 0.50."
            )
        )
        val femaleCount2024 = pop2024 * femaleProbability
        add(
            NumericItem(
                id = "lo3-num-female-count",
                objective = "LO3",
                prompt = "Estimate the number of females in the Philippines in 2024 using the World Bank population (115,843,670) and female share (50.12%).",
                answer = femaleCount2024,
                tolerance = 10_000.0,
                explanation = "Estimated females are about 115,843,670 * 0.5012 which is close to 58,059,563."
            )
        )
        add(
            MatchingItem(
                id = "lo3-match-data-interpret",
                objective = "LO3",
                prompt = "Match each 2024 indicator to its best interpretation based on World Bank data.",
                pairs = listOf(
                    MatchingPair("GDP growth 5.69%", "Moderate expansion after the 2023 slowdown"),
                    MatchingPair("Inflation 3.21%", "Price pressures eased compared with 2023"),
                    MatchingPair("Female share 50.12%", "Population remains nearly gender-balanced")
                ),
                explanation = "Each description is aligned with the cited 2024 readings."
            )
        )

        // Foundational seed items retained for quick module prototyping
        add(
            MultipleChoiceItem(
                id = "lo1-mc-1",
                objective = "LO1",
                prompt = "Ano ang future value ng Php 10,000 na may 8% interest quarterly sa loob ng 2 taon?",
                choices = listOf("Php 11,083.20", "Php 11,716.59", "Php 12,000.00", "Php 11,500.00"),
                correctIndex = 1,
                explanation = "A = P(1 + r/m)^{mt}"
            )
        )
        add(
            NumericItem(
                id = "lo2-num-1",
                objective = "LO2",
                prompt = "Kung ang simple interest ay Php 1,800 mula sa Php 12,000 sa loob ng 3 taon, ano ang rate?",
                answer = 0.05,
                tolerance = 0.005,
                explanation = "I = Prt -> r = I/(Pt)"
            )
        )
        add(
            TrueFalseItem(
                id = "lo3-tf-1",
                objective = "LO3",
                prompt = "Ang present value ay laging mas mababa kaysa future value kung may positibong interes.",
                answer = true,
                explanation = "Positive interest grows money over time."
            )
        )
        add(
            MatchingItem(
                id = "lo3-match-1",
                objective = "LO3",
                prompt = "Ipares ang termino sa kahulugan.",
                pairs = listOf(
                    MatchingPair("Principal", "Paunang halaga"),
                    MatchingPair("Interest", "Kita sa pera"),
                    MatchingPair("Maturity", "Petsa ng pagbayad")
                ),
                explanation = "Basic finance vocabulary."
            )
        )

        return list
    }
}
