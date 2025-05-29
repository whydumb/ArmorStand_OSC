load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

def _modmenu_impl(mctx):
    http_archive(
        name = "modmenu",
        url = "https://maven.terraformersmc.com/releases/com/terraformersmc/modmenu/14.0.0-rc.2/modmenu-14.0.0-rc.2-sources.jar",
        sha256 = "9d374878db50f92cc41067d43e8584069d19eceee3be7777217f5e5465397c8d",
        build_file = "@//modmenu:modmenu.BUILD.bazel",
        patch_strip = 1,
        patches = [
            "@//modmenu/patches:text-render-mixin.patch",
            "@//modmenu/patches:mod-list-widget.patch",
            "@//modmenu/patches:descrption-list-widget.patch",
            "@//modmenu/patches:mods-screen.patch",
            "@//modmenu/patches:update-checker-util.patch",
            "@//modmenu/patches:http-util.patch",
            "@//modmenu/patches:drawing-util.patch",
            "@//modmenu/patches:legacy-textured-button-widget.patch",
            "@//modmenu/patches:mod-list-entry.patch",
            "@//modmenu/patches:parent-entry.patch",
            "@//modmenu/patches:update-avaliable-badge.patch",
        ],
    )

modmenu = module_extension(implementation = _modmenu_impl)
