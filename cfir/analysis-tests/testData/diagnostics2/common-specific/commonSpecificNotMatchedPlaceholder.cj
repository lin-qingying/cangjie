// RUN_PIPELINE_TILL: FRONTEND
<!NOT_MATCHED!>common interface Printable {
    func print(): Unit
}<!>

<!NOT_MATCHED, SPECIFIC_HAS_DIFFERENT_KIND!>specific class Printable {}<!>
