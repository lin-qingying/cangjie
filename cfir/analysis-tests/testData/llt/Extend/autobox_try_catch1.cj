// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    func f() {
        println("In f of interface!");
        return 1
    }
}

extend Int64 <: I {
    public func f() {
        println("In f of extend Int64!");
        return 1
    }
}

func g(): I {
    let y: I = 9
    try {
        return 2
    } catch (e: Exception) {
        println("Exception caught here")
        y
    } finally {
        return 11
    }

    return 0
}

main() {
    let x = g()

    x.f()
    return 0
}
