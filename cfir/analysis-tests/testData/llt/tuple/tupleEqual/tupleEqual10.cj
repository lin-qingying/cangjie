// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {
    public operator func ==(right: A): Int64 {
        return 1;
    }
}

main(): Int64 {
    let x = (A(), 1);
    if (<!sema_tuple_element_cmp_not_bool!>x<!> == (A(), 1)) {
        return 0;
    }
    return 1;
}
