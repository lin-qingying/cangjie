// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// exec: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface containeri {
    interface i1 {                        // expected: regex interface type must be toplevel
        default foo1(): Int32 { return 1 }
    }

    interface i2<t> {                     // expected: regex interface type must be toplevel
        default foo2(): Int32 { return 2 }
    }

    interface i3 <: i1 & i2<Int64> {  // expected: regex interface type must be toplevel
        override default foo1(): Int32 { return 3 }
        override default foo2(): Int32 { return 3 }
    }
}

main(): Int64 {
    return 0
}

// assert: regex-not core dumped
// assert: regex-not stack trace
