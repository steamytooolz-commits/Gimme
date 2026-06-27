package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LawClause(
    val id: String,
    val text: String
)

@JsonClass(generateAdapter = true)
data class LegalStatute(
    val id: String,
    val title: String,
    val description: String,
    val clauses: List<LawClause>
)

@JsonClass(generateAdapter = true)
data class NewspaperArticle(
    val id: String,
    val headline: String,
    val content: String,
    val dayPublished: Int,
    val publicSentimentShift: Float
)
