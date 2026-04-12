// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
// ASSERT: scan 22

main() {
    CaseRunner<Int8>().runCases()
    CaseRunner<Int16>().runCases()
    return 0
}

class CaseRunner<T> where T <: Equatable<T> {
    public func runCases(checktype!: String = "AssertEquals") {
        match (checktype) {
            case "AssertNotEqual" => print("1")
            case "AssertEquals" => print("2")
            case _ => print("3")
        }
    }
}
