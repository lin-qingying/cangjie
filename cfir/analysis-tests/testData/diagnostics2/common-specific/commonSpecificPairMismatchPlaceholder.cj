// RUN_PIPELINE_TILL: FRONTEND
common interface Printable {
    func print(): Unit
}

specific interface Printable {
    func print(): Unit
}

<!NOT_MATCHED, SPECIFIC_HAS_DIFFERENT_KIND!>specific class Printable {}<!>
