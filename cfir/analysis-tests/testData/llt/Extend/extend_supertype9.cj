// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output 

abstract class C {
    public func f(): Int64
}

interface I {
    func f(): Int64
}

extend C <: I {} // OK

class D <: C {
    public func f(): Int64 {
        11
    }
}

main(): Int64 {
    if (D().f() != 11) {
        return 1
    }
    return 0
}
