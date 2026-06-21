


internal import std.collection.*

main() {
    [1, 2, 3, 4, 5] |> map {x => x - 2} |> filter {x => x > 0} |> forEach(println)
}
