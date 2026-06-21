

package pkgs

public enum TimeUnit<T> {
    century
    | Year(T)
    | Month(Int32, Int32)
    | Day(Int32, Int32, T)
}

public var g_time: TimeUnit<Int32> = TimeUnit<Int32>.Year(10000)
