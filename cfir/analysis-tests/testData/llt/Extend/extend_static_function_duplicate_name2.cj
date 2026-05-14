// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

class A {
    public var a = 0
    public func foo1() {
        return 0
    }
}

extend A {
    public static func <!INHERIT_MEMBER_KIND_INCONSISTENT, EXTEND_MEMBER_CANNOT_SHADOW!>foo1<!>() {
        return 0
    }
}

extend A {
    public static func <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo1<!>(a: Int32) {
        return 0
    }
}

main(): Int64 {
    return 0
}
