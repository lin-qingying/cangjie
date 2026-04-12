// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

struct GenRec {
    public var b: Int32 = 1
    public init(str: String) {
        this.b = foo()
    }
    private func foo(): Int32 {
        var a = b + 1
        return a
    }
}

main(): Int64 {
    var rec = GenRec("hi")
    var res = rec.b
    print(res.toString())
    if (res == 2) {
        return 0
    }
    return 1
}
