// RUN_PIPELINE_TILL: FRONTEND
interface Printable {
    func print(): Unit
}

interface Comparable<T> {
    func compareTo(other: T): Int64
}

