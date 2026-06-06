// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    mut func f() {
        false
    }
    S() {
        let g = {=> <!sema_illegal_capture_this!>f<!>()}
        print("${g()}")
    }
}

main() {}
