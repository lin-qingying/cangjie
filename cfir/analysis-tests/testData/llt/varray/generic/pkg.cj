

package pkg

public func A<T>(): Int64 {
    var v: VArray<Int64, $1> = [1]
    let b = VArray<Int64, $5>({i => i})
    return v[0] + b[1]
}
