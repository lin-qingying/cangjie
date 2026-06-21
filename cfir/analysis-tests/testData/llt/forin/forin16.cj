


import std.collection.*

main() {
    for (f in [1, 2, 3, 4] |> filter {f => f > 0}) {
        break
    }
}
