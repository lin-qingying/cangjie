package cjmp_generic_owner_v113

public common interface Box<T> {
    common func transform<R>(value: T, replacement: R): R {
        replacement
    }
}
