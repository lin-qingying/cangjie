// FILE: propertyReference.cj
// TARGET_EXPRESSION_TEXT: holder.state
// EXPECTED_STABILITY: COMPUTED_VALUE
// EXPECTED_IS_PURE_REFERENCE: true
// EXPECTED_EXPRESSION_TYPE: Int64
package sample.dataflow

class Holder {
    prop state: Int64 {
        get() {
            1
        }
    }
}

func useProperty(holder: Holder) {
    let value = holder.state
}
