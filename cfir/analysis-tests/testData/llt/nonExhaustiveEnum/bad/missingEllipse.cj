// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
enum A {
    | B | ...
}

main() {
    match (B) {
        case B => 0
    }
}

// ASSERT: scan error: non-exhaustive patterns
// ASSERT: scan the selector is of type 'Enum-A'
// ASSERT: scan 1 error generated, 1 error printed.
