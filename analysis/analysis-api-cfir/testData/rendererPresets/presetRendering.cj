package sample.renderer.presets

interface Base {
}

class User <: Base {
}

class Box<T> {
}

class Holder {
    prop state: Int64 {
        get() {
            1
        }
        set(value) {
        }
    }
}

enum Result {
    | Error(String)
    | Ok(Int64)

    func code(): Int64 {
        return 0
    }
}

func greet<T>(value: T, fallback!: Int64 = 0): T where T <: Base {
    return value
}

func sideEffect(flag: Bool): Unit {
}

func intValue(): Int32 {
    return 0
}

func floatValue(): Float64 {
    return 0.0
}

func renderEntry(user: User, holder: Holder, result: Result): Base {
    let cachedValue: Int64 = 42
    func cachedTransform(value: Int64): Int64 {
        return value + cachedValue
    }
    let cache = cachedValue
    let transformed = cachedTransform(cache)
    let current = holder.state
    let keep = cachedValue
    let used = transformed
    return greet(user)
}
