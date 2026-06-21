


main(): Int64 {
    if (String.n != 1) {
        return 1
    }
    return 0
}

extend String {
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public <!EXTEND_IMMUTABLE_MUT_PROPERTY!>mut<!> static prop n: Int32 {
    }<!>
}
