package com.classroom.quizmaster.domain.sample

import com.classroom.quizmaster.domain.model.Assessment
import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.Lesson
import com.classroom.quizmaster.domain.model.LessonSlide
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.MatchingPair
import com.classroom.quizmaster.domain.model.MiniCheck
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.ModuleSettings
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.TrueFalseItem

object GeneralMathSampleCatalog {
    fun modules(): List<Module> {
        return listOf(
            digitalSavingsModule(),
            inflationPulseModule(),
            populationTrendsModule()
        )
    }

    private fun digitalSavingsModule(): Module {
        val objectives = listOf("M11GM-Ic-1", "M11GM-Ic-2", "M11GM-Ic-3")
        val preItems: List<Item> = listOf(
            MultipleChoiceItem(
                id = "mp2-pre-mc-1",
                objective = "M11GM-Ic-1",
                prompt = "Pag-IBIG Fund declared a 7.03% dividend for the MP2 Savings Program in 2023 (Pag-IBIG Fund 2023 Annual Report). If you dropped ₱30,000 in MP2 at the start of 2023 and let it compound annually, how much would you have by the payout?",
                choices = listOf("₱32,109.00", "₱31,500.00", "₱32,700.00", "₱33,000.00"),
                correctIndex = 0,
                explanation = "Future value formula: A = P(1 + r)^t = 30,000 × (1 + 0.0703)^1 = ₱32,109.00 (Pag-IBIG Fund dividend bulletin)."
            ),
            NumericItem(
                id = "mp2-pre-num-1",
                objective = "M11GM-Ic-2",
                prompt = "The Bangko Sentral ng Pilipinas kept the overnight reverse repurchase rate at 6.50% on 21 March 2024 (BSP Monetary Board). For ₱45,000 parked in a time deposit following simple interest for 18 months, how much interest will it earn?",
                answer = 4387.5,
                tolerance = 0.5,
                explanation = "Simple interest: I = Prt = 45,000 × 0.065 × 1.5 = ₱4,387.50 based on the BSP policy rate."
            ),
            TrueFalseItem(
                id = "mp2-pre-tf-1",
                objective = "M11GM-Ic-3",
                prompt = "Philippine Deposit Insurance Corporation (PDIC) guarantees up to ₱500,000 per depositor per bank, protecting digital savings accounts.",
                answer = true,
                explanation = "PDIC insurance coverage caps at ₱500,000 per depositor per bank, including partner digital banks (PDIC primer)."
            ),
            MatchingItem(
                id = "mp2-pre-match-1",
                objective = "M11GM-Ic-1",
                prompt = "Match the money move with what it means.",
                pairs = listOf(
                    MatchingPair("MP2 dividend", "Annual return announced by Pag-IBIG Fund"),
                    MatchingPair("Policy rate", "Benchmark set by BSP for banks"),
                    MatchingPair("Compounding", "Interest earning its own interest")
                ),
                explanation = "These terms appear in Pag-IBIG Fund and BSP advisories."
            )
        )
        val postItems: List<Item> = listOf(
            MultipleChoiceItem(
                id = "mp2-post-mc-1",
                objective = "M11GM-Ic-1",
                prompt = "If you boosted your MP2 stash to ₱25,000 and let the 7.03% 2023 dividend rate ride for two straight years, what future value do you flex?",
                choices = listOf("₱28,638.55", "₱27,757.50", "₱29,100.00", "₱28,000.00"),
                correctIndex = 0,
                explanation = "Compounded annually: A = 25,000 × (1 + 0.0703)^2 = ₱28,638.55 using the Pag-IBIG Fund rate."
            ),
            NumericItem(
                id = "mp2-post-num-1",
                objective = "M11GM-Ic-2",
                prompt = "At the 6.50% BSP policy rate, how much simple interest does ₱18,000 earn after 90 days?",
                answer = 288.49,
                tolerance = 1.0,
                explanation = "I = Prt = 18,000 × 0.065 × (90/365) ≈ ₱288.49 using the BSP benchmark."
            ),
            TrueFalseItem(
                id = "mp2-post-tf-1",
                objective = "M11GM-Ic-3",
                prompt = "If you spread your savings across two PDIC-member digital banks, each account is insured separately up to ₱500,000.",
                answer = true,
                explanation = "PDIC coverage applies per depositor per bank, so splitting across banks duplicates protection (PDIC)."
            ),
            MatchingItem(
                id = "mp2-post-match-1",
                objective = "M11GM-Ic-2",
                prompt = "Pair the formula with when to use it.",
                pairs = listOf(
                    MatchingPair("I = Prt", "Simple interest time deposit"),
                    MatchingPair("A = P(1 + r)^t", "Annual compounding like MP2"),
                    MatchingPair("Real rate = nominal − inflation", "Checking true earning power")
                ),
                explanation = "Connects formulas used in BSP and Pag-IBIG calculators."
            )
        )
        return Module(
            id = "module-digital-savings",
            topic = "Compound Interest Glow-Up",
            objectives = objectives,
            preTest = Assessment(
                id = "assessment-mp2-pre",
                items = preItems,
                timePerItemSec = 75
            ),
            lesson = Lesson(
                id = "lesson-mp2",
                slides = listOf(
                    LessonSlide(
                        id = "mp2-slide-1",
                        title = "Savings Glow-Up Story",
                        content = "Pag-IBIG Fund flexed a 7.03% MP2 dividend in 2023. Source: Pag-IBIG Fund 2023 Annual Report. Track how your cash snowballs when dividends compound.",
                        miniCheck = MiniCheck("What formula turns deposits into future value when interest compounds annually?", "A = P(1 + r)^t")
                    ),
                    LessonSlide(
                        id = "mp2-slide-2",
                        title = "Policy Rate Pulse",
                        content = "As of 21 March 2024, BSP held the policy rate at 6.50%. Source: BSP Monetary Board statement. This anchors bank promos and loan pricing—aka the vibe of your time deposits.",
                        miniCheck = MiniCheck("How do you compute simple interest using the BSP rate?", "I = Prt with r = 0.065")
                    ),
                    LessonSlide(
                        id = "mp2-slide-3",
                        title = "Safety Net Receipts",
                        content = "PDIC insures up to ₱500,000 per depositor per bank. Source: PDIC deposit insurance primer. Digital-first banks ride on that same safety net.",
                        miniCheck = MiniCheck("What’s the peso cap of PDIC coverage per bank?", "₱500,000")
                    )
                )
            ),
            postTest = Assessment(
                id = "assessment-mp2-post",
                items = postItems,
                timePerItemSec = 75
            ),
            settings = ModuleSettings(
                allowLeaderboard = true,
                revealAnswersAfterSection = true,
                timePerItemSeconds = 75
            )
        )
    }

    private fun inflationPulseModule(): Module {
        val objectives = listOf("M11GM-IIa-1", "M11GM-IIa-2", "M11GM-IIa-3")
        val preItems: List<Item> = listOf(
            MultipleChoiceItem(
                id = "inflation-pre-mc-1",
                objective = "M11GM-IIa-1",
                prompt = "The Philippine Statistics Authority (PSA) reported an average inflation of 6.0% in 2023. If an iced latte cost ₱160 in January 2023, what’s the inflated estimate for January 2024 using that average?",
                choices = listOf("₱169.60", "₱166.00", "₱170.50", "₱176.00"),
                correctIndex = 0,
                explanation = "Price × (1 + inflation) = 160 × 1.06 = ₱169.60, using PSA’s 2023 average inflation."
            ),
            NumericItem(
                id = "inflation-pre-num-1",
                objective = "M11GM-IIa-2",
                prompt = "Headline inflation hit 8.7% in January 2023 (PSA). Estimate the new cost of a monthly commute card priced at ₱1,280 before the spike.",
                answer = 1391.36,
                tolerance = 1.0,
                explanation = "New price ≈ 1,280 × 1.087 = ₱1,391.36 from PSA’s January 2023 CPI release."
            ),
            TrueFalseItem(
                id = "inflation-pre-tf-1",
                objective = "M11GM-IIa-3",
                prompt = "By December 2023, PSA logged inflation cooling to 3.9%, meaning prices were still rising but at a slower pace.",
                answer = true,
                explanation = "PSA’s December 2023 report placed inflation at 3.9%; positive inflation still means prices climb."
            ),
            MatchingItem(
                id = "inflation-pre-match-1",
                objective = "M11GM-IIa-1",
                prompt = "Match the data drop to its PSA meaning.",
                pairs = listOf(
                    MatchingPair("Headline inflation", "Overall CPI reported by PSA"),
                    MatchingPair("Base year", "Reference year for price comparisons"),
                    MatchingPair("Purchasing power", "Real value of your peso after inflation")
                ),
                explanation = "PSA releases use these terms every month."
            )
        )
        val postItems: List<Item> = listOf(
            MultipleChoiceItem(
                id = "inflation-post-mc-1",
                objective = "M11GM-IIa-1",
                prompt = "PSA recorded an average inflation of 5.8% in 2022. If a review book was ₱450 in 2021, what’s the 2022 sticker price using that average?",
                choices = listOf("₱476.10", "₱465.80", "₱480.00", "₱472.00"),
                correctIndex = 0,
                explanation = "450 × 1.058 = ₱476.10, aligned with PSA’s 2022 average inflation."
            ),
            NumericItem(
                id = "inflation-post-num-1",
                objective = "M11GM-IIa-2",
                prompt = "With December 2023 inflation at 3.9% (PSA), what is the real value in 2023 pesos of a ₱500 allowance from 2022?",
                answer = 481.23,
                tolerance = 1.0,
                explanation = "Real value ≈ 500 ÷ 1.039 = ₱481.23, adjusting for PSA’s reported inflation."
            ),
            TrueFalseItem(
                id = "inflation-post-tf-1",
                objective = "M11GM-IIa-3",
                prompt = "When inflation dips from 6.0% to 3.9%, the CPI is still above the previous year’s level.",
                answer = true,
                explanation = "Positive inflation means the CPI keeps rising even if the growth rate slows."
            ),
            MatchingItem(
                id = "inflation-post-match-1",
                objective = "M11GM-IIa-2",
                prompt = "Connect the strategy to how Gen Z budgets under inflation.",
                pairs = listOf(
                    MatchingPair("Track CPI updates", "Follow PSA monthly releases"),
                    MatchingPair("Adjust allowance", "Multiply by (1 + inflation)"),
                    MatchingPair("Deflate prices", "Divide by (1 + inflation)")
                ),
                explanation = "Grounds inflation math in PSA data drops."
            )
        )
        return Module(
            id = "module-inflation-pulse",
            topic = "Inflation Pulse Check",
            objectives = objectives,
            preTest = Assessment(
                id = "assessment-inflation-pre",
                items = preItems,
                timePerItemSec = 75
            ),
            lesson = Lesson(
                id = "lesson-inflation",
                slides = listOf(
                    LessonSlide(
                        id = "inflation-slide-1",
                        title = "Receipts from PSA",
                        content = "PSA pegged average inflation at 6.0% in 2023 and 8.7% in January. Sources: PSA CPI monthly releases. We turn those stats into price moves Gen Z actually feels.",
                        miniCheck = MiniCheck("What operation do you use to inflate last year’s price?", "Multiply by (1 + inflation)")
                    ),
                    LessonSlide(
                        id = "inflation-slide-2",
                        title = "Cooling Down",
                        content = "By December 2023 inflation slid to 3.9% (PSA). That’s a slower climb, not a price rollback—key nuance for budgeting.",
                        miniCheck = MiniCheck("Does 3.9% inflation mean prices dropped?", "No, they still rose but slower")
                    ),
                    LessonSlide(
                        id = "inflation-slide-3",
                        title = "Budget Remix",
                        content = "Use PSA data to adjust allowances, subscriptions, and merch wish lists. Real talk: keep the spreadsheet updated with legit stats.",
                        miniCheck = MiniCheck("How do you find real value after inflation?", "Divide the nominal amount by (1 + inflation)")
                    )
                )
            ),
            postTest = Assessment(
                id = "assessment-inflation-post",
                items = postItems,
                timePerItemSec = 75
            ),
            settings = ModuleSettings(
                allowLeaderboard = false,
                revealAnswersAfterSection = true,
                timePerItemSeconds = 75
            )
        )
    }

    private fun populationTrendsModule(): Module {
        val objectives = listOf("M11GM-IIIb-1", "M11GM-IIIb-2", "M11GM-IIIb-3")
        val preItems: List<Item> = listOf(
            MultipleChoiceItem(
                id = "population-pre-mc-1",
                objective = "M11GM-IIIb-1",
                prompt = "PSA’s 2015 Census counted 100,981,437 Filipinos and the 2020 Census logged 109,035,343. What’s the absolute growth?",
                choices = listOf("8,053,906", "7,900,000", "8,503,906", "9,035,343"),
                correctIndex = 0,
                explanation = "Growth = 109,035,343 − 100,981,437 = 8,053,906 per PSA census releases."
            ),
            NumericItem(
                id = "population-pre-num-1",
                objective = "M11GM-IIIb-2",
                prompt = "Compute the average annual growth rate (AAGR) from 2015 to 2020 using PSA census counts.",
                answer = 0.01547,
                tolerance = 0.0005,
                explanation = "AAGR = (109,035,343 / 100,981,437)^(1/5) − 1 ≈ 0.01547 or 1.547%."
            ),
            TrueFalseItem(
                id = "population-pre-tf-1",
                objective = "M11GM-IIIb-3",
                prompt = "PSA’s 2020 Census confirms the Philippines passed the 109 million population mark.",
                answer = true,
                explanation = "The official 2020 Census figure is 109,035,343."
            ),
            MatchingItem(
                id = "population-pre-match-1",
                objective = "M11GM-IIIb-1",
                prompt = "Match the PSA data drop with its number.",
                pairs = listOf(
                    MatchingPair("2015 Census", "100,981,437"),
                    MatchingPair("2020 Census", "109,035,343"),
                    MatchingPair("Absolute growth", "8,053,906")
                ),
                explanation = "Anchors function modeling on PSA census receipts."
            )
        )
        val postItems: List<Item> = listOf(
            MultipleChoiceItem(
                id = "population-post-mc-1",
                objective = "M11GM-IIIb-2",
                prompt = "Project the 2025 population if the 2015–2020 average growth rate of 1.547% holds steady.",
                choices = listOf("117,731,599", "115,500,000", "118,900,000", "120,035,343"),
                correctIndex = 0,
                explanation = "P = 109,035,343 × (1 + 0.01547)^5 ≈ 117,731,599 per exponential growth modeling."
            ),
            NumericItem(
                id = "population-post-num-1",
                objective = "M11GM-IIIb-1",
                prompt = "PSA 2020 data lists CALABARZON at 16,195,042 people and NCR at 13,484,462. How many more people live in CALABARZON?",
                answer = 2710580.0,
                tolerance = 10.0,
                explanation = "Difference = 16,195,042 − 13,484,462 = 2,710,580 per PSA regional tables."
            ),
            TrueFalseItem(
                id = "population-post-tf-1",
                objective = "M11GM-IIIb-3",
                prompt = "PSA 2020 rankings show CALABARZON as the most populated region in the country.",
                answer = true,
                explanation = "CALABARZON tops the PSA 2020 regional population list."
            ),
            MatchingItem(
                id = "population-post-match-1",
                objective = "M11GM-IIIb-2",
                prompt = "Pair the model with its data use.",
                pairs = listOf(
                    MatchingPair("Linear trend", "Approx short-term change"),
                    MatchingPair("Exponential growth", "Use AAGR for projections"),
                    MatchingPair("Logistic model", "Cap growth when resources limit")
                ),
                explanation = "Connects PSA data to the math models in General Mathematics."
            )
        )
        return Module(
            id = "module-population-trends",
            topic = "Population Trend Tracker",
            objectives = objectives,
            preTest = Assessment(
                id = "assessment-population-pre",
                items = preItems,
                timePerItemSec = 75
            ),
            lesson = Lesson(
                id = "lesson-population",
                slides = listOf(
                    LessonSlide(
                        id = "population-slide-1",
                        title = "PSA Census Receipts",
                        content = "PSA logged 100,981,437 Filipinos in 2015 and 109,035,343 in 2020. Source: PSA Census of Population and Housing. That’s our dataset for modeling growth.",
                        miniCheck = MiniCheck("What’s the absolute growth from 2015 to 2020?", "8,053,906")
                    ),
                    LessonSlide(
                        id = "population-slide-2",
                        title = "Glow-Up Rate",
                        content = "Average annual growth clocks in at 1.547% using PSA’s counts. We’ll use that as the growth rate in exponential models.",
                        miniCheck = MiniCheck("What formula pulls out the growth rate?", "(P2/P1)^(1/n) − 1")
                    ),
                    LessonSlide(
                        id = "population-slide-3",
                        title = "Projecting 2025",
                        content = "Project the Philippines hitting about 117.7M people by 2025 if the 1.547% pace holds. That’s the kind of projection policymakers track.",
                        miniCheck = MiniCheck("What model do we use for steady percentage growth?", "Exponential growth")
                    )
                )
            ),
            postTest = Assessment(
                id = "assessment-population-post",
                items = postItems,
                timePerItemSec = 75
            ),
            settings = ModuleSettings(
                allowLeaderboard = true,
                revealAnswersAfterSection = true,
                timePerItemSeconds = 75
            )
        )
    }
}
