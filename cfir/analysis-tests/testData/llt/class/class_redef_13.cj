// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args

open class Boo<T1> {
    public static func foo<V1>(x: T1, y: V1) {
        return 1
    }
}

class Coo <: Boo<Int32> {
    public static redef func foo<V2>(x: Int32, y: V2) {
        return 2
    }
}

main() {
    var c = Coo.foo<Int64>(0, 0)
    if (c != 2) {
        return 1
    }
    return 0
}
