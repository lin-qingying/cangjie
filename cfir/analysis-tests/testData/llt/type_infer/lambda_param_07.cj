// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// disambiguate member access by non-type info

let f18 = {
    x =>
    let y = x + 1
    let z = x.toString()
    "${y}" + z
}

open class A19 {
    func foo19(x: Int) {
        x
    }
}

open class A19_ {
    func foo19(x: Int) {
        x
    }
}

class B19 <: A19_ {}

func getB19(x: B19) {}

let f19 = {
    x, y =>
    getB19(x)
    x.foo19(y)
}

class A20 {
    func foo20() {}
}

class B20 {
    var v20: Int = 0
}

let f20 = {x => x.foo20()}
let f21 = {x => x.v20}
let f22 = {x => x.toString()}

class A23 {}

extend A23 {
    func foo23(x: Int) {}
}

let f23 = {x, y => x.foo23(y)}

interface I24 {
    func bar24() {}
}

class A24 {}

extend A24 {
    func foo24(x: Int) {}
}

extend A24 <: I24 {}

let f24 = {
    x, y =>
    x.foo24(y)
    x.bar24()
}

class A25 {
    func foo25() {}
}

class B25 {
    func foo25(x: Int) {}
}

let f25 = {x => x.foo25(1)}

main() {
    println(f19(B19(), 19))
    println(f20(A20()))
    println(f21(B20()))
    println(f22(123))
    println(f23(A23(), 1))
    println(f24(A24(), 1))
    println(f25(B25()))
}
