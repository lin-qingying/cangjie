// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

struct Aoo {
    public var a: Int32 = 1
    public let b: Int32 = a
    public var c = if (true) {
        func foo2() {
            <!USED_BEFORE_INITIALIZATION!>a<!>
        }
    }
    public func foo3() {
        a
    }
}
