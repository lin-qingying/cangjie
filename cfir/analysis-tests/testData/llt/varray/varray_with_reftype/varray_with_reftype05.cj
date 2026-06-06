// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

<!sema_value_type_recursive!>s<!>truct S1 {
    var a: S2
    init(a1: S2) {
        a = a1
    }
}

struct S2 {
    static var b: Array<Int64> = [1, 2, 3]
    var a: S3
    init(a1: S3) {
        a = a1
    }
}

struct S3 {
    var a: S1
    var b: String = "123"
    init(a1: S1) {
        a = a1
    }
}

main() {
    var a: VArray<<!sema_varray_arg_type_with_reftype!>S1<!>, $1>
}
