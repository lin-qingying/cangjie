// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

struct RA<T> {
    public let a = 10
    public init(a: Int32) {
        this.a = a
    }
}

main() {
    var ra = RA<Int64>(5)
    print("${ra.a}\n")
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
