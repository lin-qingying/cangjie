// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-auto cannot assign to immutable value
// ASSERT: regex-auto cannot assign to immutable value
// ASSERT: regex-auto cannot assign to immutable value
// ASSERT: regex-auto cannot assign to immutable value
// ASSERT: regex-auto cannot assign to immutable value

struct WorldRecord {
    public var foo: Int64 = 0
    public let bar: Int64 = 1
    public let lambda: (Unit) -> Int64 = {=> foo}
    public func baz() {
        foo + 1
        bar = bar + 1
    }
}

func F4() {
    var police = 0
    let thief = 1
    let f4 = {=> police + thief}

    func f5() {
        return police + thief
    }

    var arrh = [1, 2, 3, 4]
    for (i in arrh) {
        i = i + 1
    }

    let a = match (police) {
        case b => {
            =>
            b = b + 1
            b
        }
    }
}

func F5(a: Int64) {
    a = a + 1
    let b: Int64
    while (true) {
        b = 9
    }
    a
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
