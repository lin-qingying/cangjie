// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output %run_args

func test(s: ToString) {
    if (s.toString() == "fail") {
        return 1
    } else {
        return 0
    }
}

main() {
    let v: ?Int64 = 1
    return test(v ?? "fail")
}
