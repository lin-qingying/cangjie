// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan Hello, extend in Obj!
// ASSERT: scan f in SubObj!

interface ToObj {
    func toObj(): Object
}

open class Obj {
    public open func f(): Unit {
        print("f in Obj!\n")
    }
}

extend Obj <: ToObj {
    public func toObj(): Obj {
        print("Hello, extend in Obj!\n")
        return this
    }
}

open class SubObj <: Obj {
    public override func f(): Unit {
        print("f in SubObj!\n")
    }
}

main(): Int64 {
    var x = true
    var a = if (x) {
        SubObj()
    } else {
        var b: ToObj = SubObj()
        b
    }

    a.toObj()
    (a as SubObj)?.f()

    return 0
}
