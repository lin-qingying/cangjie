// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


abstract class A {
    public static func f<T>() {
        return 11
    }

    func check_instance_func_A() {
        println(f<Unit>())
    }

    static func check_static_func_A(value: Int) {
        println(f<Unit>())
    }
}

class C2 <: A {
    public static redef func f<T>() {
        return 12
    }

    func check_instance_func_C2() {
        println(f<Unit>())
    }

    static func check_static_func_C2() {
        println(f<Unit>())
    }
}

main() {
    let C2_typeA: A = C2()
    println(A.f<Unit>()) // 11
    C2_typeA.check_instance_func_A() // 12
}
