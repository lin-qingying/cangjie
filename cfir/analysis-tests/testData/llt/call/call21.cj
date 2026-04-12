// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct Data<T> {
    public var a: T
    public init(x: T) {
        a = x
    }
    public static var b: Int32 = 2
    public static func coo<V>(x: Data<V>): Data<V> {
        return x
    }
}

class A<T> {
    public var i: T
    public init(a: T, b!: Int64 = 1) {
        i = a
    }
}

func run(): Int64 {
    var data: Data<Int32> = Data<Int32>(5)
    var res1: Data<Int32> = Data<String>.coo<Int32>(data);
    var a = A<Bool>(true)
    if (data.a.toString() != "5" || res1.a.toString() != "5" || Data<Int32>.b.toString() != "2") {
        return 1
    }
    return 0
}

main(): Int64 {
    return run()
}
