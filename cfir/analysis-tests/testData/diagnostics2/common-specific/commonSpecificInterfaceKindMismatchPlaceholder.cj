// RUN_PIPELINE_TILL: FRONTEND
<!NOT_MATCHED!>common interface Config {
    func ready(): Bool
}<!>

<!NOT_MATCHED, SPECIFIC_HAS_DIFFERENT_KIND!>specific struct Config {}<!>
