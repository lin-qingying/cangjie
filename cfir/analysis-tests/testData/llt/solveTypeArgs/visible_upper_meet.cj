interface First {}
interface Second {}
class Both <: First & Second {}

func fromUpper<T>(): Unit where T <: First & Second {}
func fromInput<T>(value: T): Unit where T <: First & Second {}

func uses(): Unit {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>fromUpper<!>()
    fromUpper<Both>()
    fromInput(Both())
}
