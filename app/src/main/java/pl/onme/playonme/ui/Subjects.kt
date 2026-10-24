package pl.onme.playonme.ui

/** Nazwy przedmiotów do wyświetlenia. Przedmioty bez paczki pokazujemy jako „wkrótce”. */
object Subjects {
    private val names = linkedMapOf(
        "csharp" to "C#",
        "tsql" to "T-SQL",
    )

    val known: List<String> get() = names.keys.toList()

    fun displayName(subject: String): String = names[subject] ?: subject
}
