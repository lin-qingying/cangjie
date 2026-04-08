// RUN_PIPELINE_TILL: FRONTEND
@<!ANNOTATION_NO_CONST_INIT!>Annotation<!>
public struct BadAnnotation {
    init(value: String) {}
}

@Annotation
public struct GoodAnnotation {
    const init(value: String) {}
}
