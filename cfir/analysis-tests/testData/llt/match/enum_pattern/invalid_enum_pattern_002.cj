// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %diag_json %n.cj -o %output 2>&1 | compare %f

main() {
    let a = None<Int64>
    if (let <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Option<Int64>.None(v)<!> <- a) {
        return 1
    }
    return 0
}
