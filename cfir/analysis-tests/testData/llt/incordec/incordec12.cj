// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class Data {
    public static var n: Int32 = 1
    public var m: Int32 = 1
    public init(x: Int32) {
        Data.n++;
        n++
    }
    public func get(): Int32 {
        Data.n++;
        n++;
        return n
    }
}

main(): Int64 {
    let a: Int32 = 1
    Data(a).m++
    if (Data.n != 3) {
        return 1
    }
    return 0
}
