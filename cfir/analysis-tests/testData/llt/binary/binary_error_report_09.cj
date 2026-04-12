// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %enableO2 %cmp_opt %n.cj -o %output %diag_json 2>&1 |compare %f

class Test {
    operator func +(other: Test) {
        return this + other  // recursive call without return type
    }
}

main() {
    let a = Test()
    let b = Test()
    let c = a + b
}

// ASSERT: scan sema_unable_to_infer_return_type
// ASSERT: scan with recursive usage from
// ASSERT: scan sema_invalid_binary_expr