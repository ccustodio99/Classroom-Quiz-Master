package com.classroom.quizmaster.ui.util

import com.classroom.quizmaster.domain.model.BrainstormActivity
import com.classroom.quizmaster.domain.model.InteractiveActivity
import com.classroom.quizmaster.domain.model.OpenEndedActivity
import com.classroom.quizmaster.domain.model.PollActivity
import com.classroom.quizmaster.domain.model.PuzzleActivity
import com.classroom.quizmaster.domain.model.QuizActivity
import com.classroom.quizmaster.domain.model.SliderActivity
import com.classroom.quizmaster.domain.model.TypeAnswerActivity
import com.classroom.quizmaster.domain.model.WordCloudActivity
import com.classroom.quizmaster.domain.model.TrueFalseActivity as TrueFalseInteractive

fun InteractiveActivity.typeLabel(): String = when (this) {
    is QuizActivity -> "Quiz"
    is TrueFalseInteractive -> "True / False"
    is TypeAnswerActivity -> "Type Answer"
    is PuzzleActivity -> "Puzzle"
    is SliderActivity -> "Slider"
    is PollActivity -> "Poll"
    is WordCloudActivity -> "Word Cloud"
    is OpenEndedActivity -> "Open-Ended"
    is BrainstormActivity -> "Brainstorm"
}

fun InteractiveActivity.categoryLabel(): String =
    if (isScored) "To Test Knowledge" else "To Gather Opinions"

fun InteractiveActivity.summaryLabel(): String = when (this) {
    is QuizActivity -> "Quiz • ${options.size} opsyon — tamang sagot #${correctAnswers.joinToString { (it + 1).toString() }}"
    is TrueFalseInteractive -> "True/False • ${if (correctAnswer) "Tama" else "Mali"} ang tamang sagot"
    is TypeAnswerActivity -> "Type Answer • key: ${correctAnswer.take(maxCharacters)}"
    is PuzzleActivity -> "Puzzle • ${correctOrder.joinToString(" → ")}".take(80)
    is SliderActivity -> "Slider • ${minValue}-${maxValue}, target ${target}"
    is PollActivity -> "Poll • ${options.joinToString(limit = 4)}"
    is WordCloudActivity -> "Word Cloud • hanggang ${maxWords} salita"
    is OpenEndedActivity -> "Open-Ended • ${maxCharacters} chars limit"
    is BrainstormActivity -> "Brainstorm • ${categories.joinToString()}"
}
