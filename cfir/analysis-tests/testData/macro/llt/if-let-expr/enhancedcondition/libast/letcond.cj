

import std.ast.*

main() {
    let aa = [quote(let a <- 1), quote(let 1|2|3 <- b), quote(let Some(a) | None <- None)]
    for (b in aa) {
        let input = LetPatternExpr(b)
        input.toTokens()
    }
}
