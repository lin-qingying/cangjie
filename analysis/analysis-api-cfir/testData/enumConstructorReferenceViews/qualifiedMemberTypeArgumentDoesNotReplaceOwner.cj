enum TimeUnit<T> {
    Year(T) | Month(T)
}

func make(): TimeUnit<Int32> {
    TimeUnit<Int32>.<caret>Year<Int64>(4i32)
}
