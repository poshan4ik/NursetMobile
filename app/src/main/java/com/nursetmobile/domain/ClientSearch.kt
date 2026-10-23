package com.nursetmobile.domain

import com.nursetmobile.data.entity.ClientEntity

fun phoneDigits(phone: String): String = phone.filter { it in '0'..'9' }

fun ClientEntity.matchesSearch(query: String): Boolean {
    val text = query.trim()
    if (text.isEmpty() || name.contains(text, ignoreCase = true)) return true
    val digits = phoneDigits(text)
    return digits.isNotEmpty() && text.all { it in "0123456789+()- " } &&
        phoneDigits(phone).contains(digits)
}
