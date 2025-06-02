def _merge_jij_impl(ctx):
    output_jar = ctx.actions.declare_file(ctx.label.name + ".jar")
    input_jar = ctx.file.input

    args = ctx.actions.args()
    args.add(input_jar)
    args.add(output_jar)
    dep_files = []
    for dep in ctx.attr.deps.keys():
        name = ctx.attr.deps[dep]
        jar = dep.files.to_list()[0]
        dep_files.append(jar)
        args.add(name)
        args.add(jar)

    ctx.actions.run(
        inputs = depset(dep_files + [input_jar]),
        outputs = [output_jar],
        executable = ctx.executable._jij_merger_executable,
        arguments = [args],
        progress_message = "Create jij JAR %s" % ctx.label.name,
        toolchain = "@bazel_tools//tools/jdk:toolchain_type",
    )

    return [
        JavaInfo(
            output_jar = output_jar,
            compile_jar = output_jar,
        ),
        DefaultInfo(files = depset([output_jar])),
    ]

merge_jij = rule(
    implementation = _merge_jij_impl,
    attrs = {
        "input": attr.label(
            mandatory = True,
            allow_single_file = [".jar"],
            doc = "Input JAR",
        ),
        "deps": attr.label_keyed_string_dict(
            mandatory = True,
            allow_files = [".jar"],
            doc = "JARs to be merged as jar-in-jar"
        ),
        "_jij_merger_executable": attr.label(
            default = Label("@//rule/jij_merger"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Merge JAR to a main JAR as Fabric jar-in-jar",
)
