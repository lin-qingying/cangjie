// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class Data {
    public static var a = 1
    public var b = 1
    public init() {
        Data.a++
    }
    public func f(): Int64 {
        Data.a++
        var x = 1
        return x
    }
}

main(): Int64 {
    var data = Data()
    Data().b++
    var b = Data().f()
    if (Data.a == 5) {
        return 0
    }
    return 1
}
