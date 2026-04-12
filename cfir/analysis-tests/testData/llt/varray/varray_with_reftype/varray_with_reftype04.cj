// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan 'Struct-S1' directly or indirectly contains an unsupported type

struct S1 {
    var a: S2
    init(a1: S2) {
        a = a1
    }
}

struct S2 {
    var a: S3
    static var b: String = "123" // ok
    init(a1: S3) {
        a = a1
    }
}

struct S3 {
    var a: String = "123" // error
}

main() {
    var a: VArray<S1, $1>
}
