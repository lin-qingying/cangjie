

internal import std.{collection.ArrayList, collection.map}

main() {
    let result = ArrayList([1, 2, 3]) |> map {
        item => item * item
    }
}
