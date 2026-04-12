// ERRCHECK: %compiler %enableEH %cmp_opt %f -o %output %diag_json
import stdx.effect.Command

// Was ICE, should report error
func foo(a: Array<Int64>) : Int64 {1}
main() : Int64 {
    foo([try {
        1
    } handle (e: Command<String>) {
        f // undefined
        resume with "Result"
    }])
}

// ASSERT: scan sema_undeclared_identifier


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
