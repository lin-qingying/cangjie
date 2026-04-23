// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

import std.collection.HashMap

class A {
    static init() {
        C.fun()
    }
}

class C {
    static let MAP = HashMap<String, String>()
    public static func fun(): Int64 {
        <!USED_BEFORE_INITIALIZATION!>MAP<!>.size
    }
}

main(): Int64 {
    0
}
