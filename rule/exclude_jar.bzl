load("@rules_java//java:defs.bzl", "java_common")
load("@rules_java//java:defs.bzl", "JavaInfo")

def _exclude_jar_impl(ctx):
    merged_deps = java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])
    merged_excludes = java_common.merge([dep[JavaInfo] for dep in ctx.attr.excludes])

    return [merged_deps]

exclude_jar = rule(
    implementation = _exclude_jar_impl,
    attrs = {
        "deps": attr.label_list(
            mandatory = True,
            providers = [JavaInfo],
            doc = "Input JARs to merge"
        ),
        "excludes": attr.label_list(
            mandatory = True,
            doc = "Exclude items"
        ),
    },
    doc = "Exclude specified JARs"
)
