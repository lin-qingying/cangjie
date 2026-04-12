// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class Data {
    public static var a: Int32 = 1
    public static var b: Int32 = 2
    public static var c: Int32 = 4
    public static var d: Int32 = 9
    public var m: Int32 = 1
    public init() {
        Data.a += 1
        Data.b -= 2
        Data.c *= 4
        Data.d /= 3
    }
}

main(): Int64 {
    Data().m++
    if (Data.a == 2 && Data.b == 0 && Data.c == 16 && Data.d == 3) {
        return 0
    }
    return 1
}
