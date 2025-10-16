package com.classroom.quizmaster.agents

import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.MatchingItem
import com.classroom.quizmaster.domain.model.MatchingPair
import com.classroom.quizmaster.domain.model.MultipleChoiceItem
import com.classroom.quizmaster.domain.model.NumericItem
import com.classroom.quizmaster.domain.model.TrueFalseItem
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ItemBankAgentImpl : ItemBankAgent {
    private val items = ConcurrentHashMap<String, Item>().apply {
        fun add(item: Item) { put(item.id, item) }
        add(
            MultipleChoiceItem(
                id = "lo1-mc-1",
                objective = "LO1",
                prompt = "Ano ang future value ng ₱10,000 na may 8% interest quarterly sa loob ng 2 taon?",
                choices = listOf("₱11,083.20", "₱11,716.59", "₱12,000.00", "₱11,500.00"),
                correctIndex = 1,
                explanation = "A = P(1 + r/m)^{mt}"
            )
        )
        add(
            NumericItem(
                id = "lo2-num-1",
                objective = "LO2",
                prompt = "Kung ang simple interest ay ₱1,800 mula sa ₱12,000 sa loob ng 3 taon, ano ang rate?",
                answer = 0.05,
                tolerance = 0.005,
                explanation = "I = Prt ⇒ r = I/(Pt)"
            )
        )
        add(
            TrueFalseItem(
                id = "lo3-tf-1",
                objective = "LO3",
                prompt = "Ang present value ay laging mas mababa kaysa future value kung may positibong interes.",
                answer = true,
                explanation = "Positive interest grows money over time"
            )
        )
        add(
            MatchingItem(
                id = "lo3-match-1",
                objective = "LO3",
                prompt = "Ipares ang termino sa kahulugan",
                pairs = listOf(
                    MatchingPair("Principal", "Paunang halaga"),
                    MatchingPair("Interest", "Kita sa pera"),
                    MatchingPair("Maturity", "Petsa ng pagbayad")
                ),
                explanation = "Basic finance vocabulary"
            )
        )
    }

    override fun query(objectives: List<String>, limit: Int): List<Item> {
        val objectiveSet = objectives.toSet()
        return items.values.filter { it.objective in objectiveSet }.take(limit)
    }

    override fun upsert(items: List<Item>): Result<Unit> {
        items.forEach { item -> this.items[item.id.ifBlank { UUID.randomUUID().toString() }] = item }
        return Result.success(Unit)
    }
}
