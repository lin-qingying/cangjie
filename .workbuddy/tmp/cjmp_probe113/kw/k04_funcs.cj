package k04

func common(): Int64 { 1 }
func specific(common: Int64): Int64 { common }

main(): Int64 {
    specific(common())
}
