// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
enum A {
    | B | ... | ...
}

enum B {
    B | ... | C | ...
}
main() {
}

// ASSERT: scan-2 error: expected declaration, found '|'
