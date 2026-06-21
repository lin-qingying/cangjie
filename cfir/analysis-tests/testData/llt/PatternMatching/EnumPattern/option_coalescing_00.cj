

import std.core

enum Option<T> {
    Some(T)
}

main() {
    core.Option.Some(0) ?? 1
}
