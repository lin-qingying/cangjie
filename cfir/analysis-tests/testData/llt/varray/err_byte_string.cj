

func f(_: VArray<Byte, $3>) {}

main() {
    f(<!ARGUMENT_TYPE_MISMATCH!>"123".toArray()<!>)
}
