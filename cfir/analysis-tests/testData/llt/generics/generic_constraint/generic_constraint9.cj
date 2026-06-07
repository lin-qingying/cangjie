<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func atest<<!sema_generic_param_exist_in_class_irrelevant_upperbound_recursively!>T<!>>(param: T) where T <: Array<T> {
    return param
}
