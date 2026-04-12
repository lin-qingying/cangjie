// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

// Was failed, should pass
interface I1 {
    func f(): Bool
}

interface I2 {
    func f(): Rune
}

interface I3 <: I1 & I2 {
    func f(): Nothing
}

interface I4 <: I3 {
    func f(): Nothing
}

class E <: Exception {}

class A <: I1 & I2 {
    public func f(): Nothing {
        throw E()
    }
}

main() {}
