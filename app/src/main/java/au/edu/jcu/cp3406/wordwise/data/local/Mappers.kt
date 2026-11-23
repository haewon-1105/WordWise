package au.edu.jcu.cp3406.wordwise.data.local

import au.edu.jcu.cp3406.wordwise.data.local.entity.CardProgressEntity
import au.edu.jcu.cp3406.wordwise.data.local.entity.WordEntity
import au.edu.jcu.cp3406.wordwise.data.local.entity.WordWithProgress
import au.edu.jcu.cp3406.wordwise.model.CardProgress
import au.edu.jcu.cp3406.wordwise.model.DeckEntry
import au.edu.jcu.cp3406.wordwise.model.Flashcard
import au.edu.jcu.cp3406.wordwise.model.Word
import au.edu.jcu.cp3406.wordwise.model.WordSource

/* Conversions between Room entities and domain models, so the UI never sees Room types. */

fun WordEntity.toModel() = Word(
    id = id,
    term = term,
    partOfSpeech = partOfSpeech,
    definition = definition,
    example = example,
    level = level,
    source = source,
    phonetic = phonetic,
    audioUrl = audioUrl,
)

fun CardProgressEntity.toModel() = CardProgress(
    wordId = wordId,
    box = box,
    dueEpochDay = dueEpochDay,
    correctCount = correctCount,
    incorrectCount = incorrectCount,
    lastReviewedEpochDay = lastReviewedEpochDay,
)

fun CardProgress.toEntity() = CardProgressEntity(
    wordId = wordId,
    box = box,
    dueEpochDay = dueEpochDay,
    correctCount = correctCount,
    incorrectCount = incorrectCount,
    lastReviewedEpochDay = lastReviewedEpochDay,
)

fun WordWithProgress.toModel() = Flashcard(
    word = word.toModel(),
    progress = progress?.toModel(),
)

fun DeckEntry.toEntity(createdEpochDay: Long) = WordEntity(
    term = term,
    partOfSpeech = partOfSpeech,
    definition = definition,
    example = example,
    level = level,
    source = WordSource.DECK,
    createdEpochDay = createdEpochDay,
)
