// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    if (Boo().d != 1) {
        return 1
    }
    return 0
}

class Boo {
    public var d: Int32 = <!TYPE_MISMATCH!>{=> 1}<!>
}
