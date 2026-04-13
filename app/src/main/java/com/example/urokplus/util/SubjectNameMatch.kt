package com.example.urokplus.util

import java.util.Locale

/** Сопоставление названия урока из расписания с полем subject в заданиях (разный стиль строк). */
fun subjectNamesMatch(lessonSubject: String, assignmentSubject: String): Boolean {
    fun norm(s: String) = s.trim().lowercase(Locale("ru")).removeSuffix(" язык").trim()
    val a = norm(lessonSubject)
    val b = norm(assignmentSubject)
    if (a.isEmpty() || b.isEmpty()) return false
    if (a == b) return true
    if (a.startsWith(b) || b.startsWith(a)) return true
    if (a.contains(b) || b.contains(a)) return true
    return false
}
