load("@rules_java//java:defs.bzl", "java_common")
load("@rules_java//java:defs.bzl", "JavaInfo")

def _merge_jar_impl(ctx):
    output_jar = ctx.actions.declare_file(ctx.label.name + ".jar")

    merged_deps = java_common.merge([dep[JavaInfo] for dep in ctx.attr.deps])

    args = ctx.actions.args()
    args.add(output_jar)
    args.add_all(merged_deps.full_compile_jars.to_list())

    ctx.actions.run(
        inputs = merged_deps.full_compile_jars,
        outputs = [output_jar],
        executable = ctx.executable._merge_jar_executable,
        arguments = [args],
        progress_message = "Merging JAR %s" % ctx.label.name,
        toolchain = "@bazel_tools//tools/jdk:toolchain_type",
    )

    return [
        JavaInfo(
            output_jar = output_jar,
            compile_jar = output_jar,
        ),
        DefaultInfo(files = depset([output_jar])),
    ]

merge_jar = rule(
    implementation = _merge_jar_impl,
    attrs = {
        "deps": attr.label_list(
            mandatory = True,
            providers = [JavaInfo],
            doc = "Input JARs to merge"
        ),
        "_merge_jar_executable": attr.label(
            default = Label("@//rule/merge_jar"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Merge JARs"
)
