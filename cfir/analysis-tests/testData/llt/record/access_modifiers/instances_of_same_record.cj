// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct C {
    private static var zi: Int64 = 2
    private static func test() {
        return 3
    }
    public func f() {
        return C.zi
    }
    public func f1() {
        return C.test()
    }
}

struct Date {
    private func test() {
        return 2
    }
    public func test1() {
        return 3
    }
    public func f2() {
        return Date().test()
    }
    public func f3() {
        return f().test()
    }
}

func f() {
    return Date()
}

main(): Int64 {
    if (C().f() == 2 && C().f1() == 3 && Date().f2() == 2 && Date().f3() == 2) {
        return 0
    }
    return 1
}
