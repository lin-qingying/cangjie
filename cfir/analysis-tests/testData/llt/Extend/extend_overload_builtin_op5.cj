


interface Add<T> {
    operator func +(right: T): Int64
}

<!RETURN_TYPE_INCOMPATIBLE!>extend Int32 <: Add<Int32> {}<!>
