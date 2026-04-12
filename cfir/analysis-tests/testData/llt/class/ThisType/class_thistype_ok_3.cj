// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
var aa = 0

class Data<T> {
    public Data(var a!: Int64 = 6) {
        aa++
    }
    public func f2(): This {
        this
    }
    public func f1(a: T): This {
        if (true) {
            return f2()
        } else {
            return this
        }
    }
}

main(): Int64 {
    var data = Data<Int64>(a: 8)
    if (data.f1(3).a == 8 && aa == 1) {
        return 0
    }
    return 1
}
