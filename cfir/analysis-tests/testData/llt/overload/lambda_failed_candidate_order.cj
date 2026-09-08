class MinusOwner {
    operator func -() { MinusOwner() }
}

class NotOwner {
    operator func !() { NotOwner() }
}

func forward(_: (MinusOwner) -> MinusOwner) {}
func forward(_: (NotOwner) -> NotOwner) {}

func reverse(_: (NotOwner) -> NotOwner) {}
func reverse(_: (MinusOwner) -> MinusOwner) {}

main() {
    forward({ value => !!<!INVALID_UNARY_EXPR!>!-value<!> })
    reverse({ value => !!!<!INVALID_UNARY_EXPR!>-value<!> })
    0
}
