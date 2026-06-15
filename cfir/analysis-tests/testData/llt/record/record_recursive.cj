<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


<!VALUE_TYPE_RECURSIVE!>s<!>truct A {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: A
}

<!VALUE_TYPE_RECURSIVE!>s<!>truct B {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: C
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let y: Int32
}

struct C {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: D
}

struct D {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic let x: B
}
