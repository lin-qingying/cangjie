// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

struct StructA {
    public const init() {}
    public static let A: Array<String> = ["array"]
}

class ClassB {
    public const init() {}
    public static let A: Array<String> = ["array"]
}

class ClassD {
    static const init() {}
    private let A = ["array"]
}

class ClassE {
    static const init() {
    }
    const init(para: Int64) {
        b = para
    }
    static let A: Int64 = 10
    let b: Int64
}

main() {
    return
}
