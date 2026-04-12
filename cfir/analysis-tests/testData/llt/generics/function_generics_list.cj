// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

public class A<T> {
    public func runCases(cases: Array<(T, T)>) {
        for (i in 0..cases.size) {
            testCase(cases[i][0]);
        }
    }

    public static func testCase(input: T) {}
}

main() {
    return 0
}
