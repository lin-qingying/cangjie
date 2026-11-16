public enum ColumnType {
    | String
    | Other

    public static func of(value: Bool): ColumnType {
        match (value) {
            case true => String
            case false => Other
        }
    }
}

func String(value: Int64): ColumnType {
    ColumnType.of(value > 0)
}

main() {
    let _ = ColumnType.of(true)
}
