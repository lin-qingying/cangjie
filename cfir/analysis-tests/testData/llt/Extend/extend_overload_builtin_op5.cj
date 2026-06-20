// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface Add<T> {
    operator func +(right: T): Int64
}

<!RETURN_TYPE_INCOMPATIBLE!>extend Int32 <: Add<Int32> {}<!>
