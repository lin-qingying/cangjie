// RUN_PIPELINE_TILL: FRONTEND
public interface Comparable<T> {
    func compareTo(other: T): Int64 {
        return 0
    }
}