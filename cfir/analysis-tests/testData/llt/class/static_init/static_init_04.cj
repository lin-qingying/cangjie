// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    static init() {
        if (1 > 2) {
            <!INVALID_RETURN_IN_STATIC_INIT!>return<!>
        } else {
            <!INVALID_RETURN_IN_STATIC_INIT!>return<!>
        }
    }
}

main() {
    return 0
}
