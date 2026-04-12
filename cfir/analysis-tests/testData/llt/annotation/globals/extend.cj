// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -Woff all

@Annotation[target: [Extension, MemberFunction, MemberProperty]]
public class APILevel {
    public const init() {}
}

extend Int8 {
    @APILevel
    func foo() {
        0
    }

    @APILevel
    static func bar() {
        1
    }

    @APILevel
    prop K: Int {
        get() {
            2
        }
    }

    @APILevel
    static prop K2: Int {
        get() {
            3
        }
    }
}

main() {}
