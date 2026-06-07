// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func getTime<V>(x: V, z: V): V {
    return x
}

func getTime<V>(x: V): V {
    return x
}

enum Time<T> {
    Day(T)
}

func run(): Int64 {
    var time1 = <!sema_ambiguous_arg_type!>T<!>ime.Day(getTime<Int32>)
    var time = Time.Day(<!sema_generic_type_without_type_argument!>g<!>etTime)
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
