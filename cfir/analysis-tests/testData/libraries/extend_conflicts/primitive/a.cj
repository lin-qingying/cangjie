package a
public interface IA {
    prop code: Int64 { get() { 1 } }
}
extend Int64 <: IA {}
