package test.patterns.symbols

func takeNumber(value: Int64): Int64 { value }
func takeFlag(value: Bool): Bool { value }
func transform<T>(value: T, action: (T) -> Int64): Int64 { action(value) }

main() {
    let result = transform((1, true), { value =>
        match (value) {
            case (number, flag) =>
                takeNumber(<caret_numberUse>number) + if (takeFlag(<caret_flagUse>flag)) { 1 } else { 0 }
        }
    })
    println(result)
}
