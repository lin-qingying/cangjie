


internal import std.collection.*

main() {
    for (num in <!EXPR_IN_FORIN_MUST_HAS_ITERATOR!>10<!>) {
        println(num)
    }
    return 0
}
