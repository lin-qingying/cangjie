// A C struct may not be implicitly boxed when passed to a non-identical parameter type.
@C
struct NativeRecord {}

func acceptsAny(value: Any): Unit {}

main() {
    let value = NativeRecord()
    acceptsAny(<!CSTRUCT_CANNOT_AUTOBOX!>value<!>)
}
