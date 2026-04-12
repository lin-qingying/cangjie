// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// disambiguate member access by type info

class A29 {
    let v = 29
}

class B29 {
    let v = "hello29"
}

func g29(x: String) {
    x
}

let f29 = {x => g29(x.v)}

class A30 {
    let v30_1 = 30
    let v30_2 = 30
}

class B30 {
    let v30_2 = 30
    let v30_3 = 30
}

class C30 {
    let v30_3 = 30
    let v30_1 = 30
}

let f30 = {
    x =>
    x.v30_2
    x.v30_3
}

class A31<T> {
    func g31_1<R>(_: T, _: R) {
        println("A31")
    }
    func g31_2<R>(_: T, _: R) {}
}

class B31<T> {
    func g31_2<R>(_: T, _: R) {
        println("B31")
    }
    func g31_3<R>(_: T, _: R) {}
}

class C31<T> {
    func g31_3<R>(_: T, _: R) {
        println("C31")
    }
    func g31_1<R>(_: T, _: R) {}
}

let f = {
    x =>
    let y = x
    y.g31_2(2, "hello")
    y.g31_3(3, true)
}

extend Int64 {
    func foo34() {}
}

extend Unit {
    func foo34() {}
}

let f34 = {
    x =>
    x.foo34()
    x + x
}
let f35 = {
    a, b => while (let (Some(x), y) <- (a, b)) {
        y.getOrThrow()
        x + y.getOrThrow() + 1
        break
    }
}

class A36 {
    public operator func +(right: Any) {
        return right
    }
}

let f36 = {a => A36() + (a)}

main() {
    println(f29(B29()))
    println(f30(B30()))
    println(f34(1))
    println(f35(Some(1), Some(2)))
    let v36 = f36(1)
}
