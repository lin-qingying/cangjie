

interface B<T> where T <: B<T> {}

interface BB <: B<BB> {}

main(): Int64 {
    return 0
}
