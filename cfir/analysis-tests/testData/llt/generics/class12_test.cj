// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func run(): Int64 {
    var res: Int32 = Foo().opt.t
    if (res == 1) {
        return 0
    }
    return 1
}

class Data<T> {
    public var t: T
    public init(x: T) {
        t = x
    }
}

class Foo {
    public var opt: Data<Int32> = Data<Int32>(1)
}

main(): Int64 {
    return run()
}
