// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

<!sema_inheritance_cycle!>i<!>nterface I1<T> <: I3<T> {}

interface I2<K> <: I1<K> {}

interface I3<Q> <: I2<Q> & <!sema_inherit_duplicate_interface!>I<!>2<Int32> {}

main(): Int64 {
    var i3: <!sema_inherit_duplicate_interface!>I<!>3<Int32>
    return 0;
}
