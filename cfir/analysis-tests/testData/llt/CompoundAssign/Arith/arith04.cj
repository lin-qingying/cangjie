// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class Data {
    public static var a: Int64 = 24
    public static var b: Int64 = 3
    public var m: Int32 = 1
    public init() {
        Data.a /= 7
        Data.b **= 4
    }
}

main(): Int64 {
    Data().m++
    if (Data.a == 3 && Data.b == 81) {
        return 0
    }
    return 1
}
