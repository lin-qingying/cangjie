


interface B<T> where T <: B<T> {}

class BB <: B<BB> {}

main(): Int64 {
    return 0
}
