// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct StructA {
    static const init() {}
    public static let A: Array<String> = ["array"]
}

class ClassC {
    static const init() {}
    public static let A: Array<String> = ["array"]
}

main() {
    return
}

// ASSERT: scan-2 error: expected 'const' expression guaranteed to be evaluated at compile time
