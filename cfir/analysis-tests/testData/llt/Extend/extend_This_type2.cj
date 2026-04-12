// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface ToStr {
    func toStr(): String
}

extend Int32 <: ToStr {
    public func toStr(): String {
        return this.toString()
    }
}

extend Int64 <: ToStr {
    public func toStr(): String {
        return this.toString()
    }
}

main() {
    let a: Int32 = 1
    print(a.toStr())
    0
}
