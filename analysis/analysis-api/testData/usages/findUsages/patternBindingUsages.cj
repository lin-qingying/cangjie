// TARGET_KIND: BINDING_PATTERN
// TARGET_NAME: value
// EXPECTED_USAGE_COUNT: 2
// EXPECTED_USAGE_SCOPE_KIND: LOCAL
package sample.findusages

func consume(): Int64 {
    let value = 1
    let other = value
    return value + other
}
