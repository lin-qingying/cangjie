// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    let err = 1 <!INVALID_BINARY_OPERATOR!>+<!> true

    try (x = err) {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    } catch (_: Exception) {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    } finally {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    }

    let b: Unit = try (x = err) {
        true
    } catch (_: Exception) {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    } finally {
        let b = 1 <!INVALID_BINARY_OPERATOR!>+<!> false
    }

    if (err.isClosed) {}

    0
}

// No errors:

// Errors:
