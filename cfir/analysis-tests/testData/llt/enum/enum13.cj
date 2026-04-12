// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class Sub <: Base {
    public override func value() {
        2
    }
}

open class Base {
    public open func value() {
        1
    }
}

class Other {
    public var value: A<Base>
    public init(a: A<Base>) {
        value = a
    }
}

enum A<T> {
    Some(T)
}

main(): Int64 {
    var a: A<Base> = Some(Sub())
    let ret1 = match (a) {
        case Some(x) => x.value()
    }
    var b = Other(Some(Sub()))
    let ret2 = match (b.value) {
        case Some(x) => x.value()
    }
    if (ret1 != 2) {
        return 1
    }
    return 0
}
