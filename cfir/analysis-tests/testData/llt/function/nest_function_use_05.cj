// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

//Should report circular dependency

internal import std.collection.HashMap

class B {
    func use<T>(v: T, exec: (T) -> Unit) {
        exec(v)
    }
}

class A {
    private static let a = {
        =>
        let map = HashMap<String, (Array<Any>, () -> Any) -> Any>()
        register(B())
        map
    }()

    public static func register(aspect: A) {
        register("test", {v: Array<Any>, fn: () -> Any => ()})
    }
    public static func register(b: B) {
        b.use<A>(A(), register)
    }
    public static func register(name: String, fn: (Array<Any>, () -> Any) -> Any): Unit {
        a[name] = fn
    }
}

main() {}

/* SCAN
error: global/static variable 'a' is used before initialization during initializing 'a'
  ==> nest_function_use_05.cj:34:9:
   | 
34 |         a[name] = fn
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
