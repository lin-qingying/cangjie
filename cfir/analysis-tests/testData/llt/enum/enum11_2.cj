// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func getTime<V>(x: V, z: V): V {
    return x
}

func getTime<V>(x: V): V {
    return x
}

func getTime(x: Int32): Int32 {
    return x
}

enum Time<T> {
    Day(T)
}

func run(): Int64 {
    var time = Time.Day(getTime)
    var res: Int32 = match (time) {
        case Day(x) => x(3)
    }
    if (res == 3) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}
