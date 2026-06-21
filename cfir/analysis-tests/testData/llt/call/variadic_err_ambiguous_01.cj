

func ff1(a: Int64, arr: Array<Int64>) {}

func ff1(arr: Array<Int64>) {}

var ad = <!AMBIGUOUS_FUNCTION_CALL!>ff1<!>(1, 2, 3)

main() {}
