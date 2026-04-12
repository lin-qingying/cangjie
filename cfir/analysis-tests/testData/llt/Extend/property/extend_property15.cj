// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    B<Int32>().a
    var a = [1, 2]
    a.size = 1
    return 0
}

class A {}

extend A {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
}

extend A {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
}

class B<T> {}

extend B<Int32> {}

extend B<Int64> {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
}

// ASSERT: scan error: 'a' is not a member of class 'B<Int32>'
// ASSERT: scan  ==> extend_property15.cj:10:16:
// ASSERT: scan error: cannot assign to immutable value
// ASSERT: scan  ==> extend_property15.cj:12:5:
// ASSERT: scan error: extend member 'a' is not allowed to shadow members of 'extend A'
// ASSERT: scan   ==> extend_property15.cj:19:17:
// ASSERT: scan error: extend member 'a' is not allowed to shadow members of 'extend A'
// ASSERT: scan   ==> extend_property15.cj:27:17:
// ASSERT: scan 4 errors generated, 4 errors printed.
