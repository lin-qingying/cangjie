// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt  %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class Base {
    public var a: Int32
    public var b: Float32
    public init() {
        a = 100
        b = 10.0
    }

    public init(data1: Int32) {
        a = data1
        b = 200.0
    }

    public init(data1: Int32, data2: Float32) {
        a = data1
        b = data2
    }
}

class Data <: Base {
    public var c: Int32 = 1
    public init() {
        super(1)
        c = super.a + 300
    }
}

func run(): Int64 {
    var value: Base = Base(1)
    return 0
}

main(): Int64 {
    return run()
}
