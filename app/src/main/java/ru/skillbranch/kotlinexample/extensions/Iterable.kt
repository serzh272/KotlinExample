package ru.skillbranch.kotlinexample.extensions

fun <E> List<E>.dropLastUntil(predicate: (E) -> Boolean): List<E> {
    if (!isEmpty()) {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            val pr = iterator.previous()
            if (predicate(pr)) {
                return take(iterator.nextIndex())
            }
        }
    }
    return emptyList()
}