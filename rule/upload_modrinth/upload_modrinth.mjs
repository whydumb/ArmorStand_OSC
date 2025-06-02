import fs from "node:fs/promises";
import process from "node:process";
import childProcess from "node:child_process";
import util from "node:util";

function getEnvironmentVariable(name) {
    const value = process.env[name];
    if (value) {
        return value;
    }
    throw `Require ${name} environment variable, but is not set`;
}

const projectId = getEnvironmentVariable("MODRINTH_PROJECT_ID");
const modrinthToken = getEnvironmentVariable("MODRINTH_TOKEN");
const modVersion = getEnvironmentVariable("MOD_VERSION");
const gameVersion = getEnvironmentVariable("GAME_VERSION");

const execFile = util.promisify(childProcess.execFile);
const gitCommit = (await execFile("git", ["rev-parse", "HEAD"])).stdout.trim();
if (!gitCommit) {
    console.error("Failed to get current git commit")
    process.exit(1);
}

const userAgent = `fifth_light/ArmorStand/${gitCommit}`;
async function fetchJson(url, params) {
    const response = await fetch(url, params);
    if (!response.ok) {
        const result = await response.text();
        throw `Bad response code ${response.status}, result: ${result}`;
    }
    return await response.json();
}

async function fetchVersions() {
    return await fetchJson(`https://api.modrinth.com/v2/project/${projectId}/version`, {
        headers: {
            "User-Agent": userAgent,
        },
    });
}

await (async () => {
    const shortCommit = gitCommit.substring(0, 8);

    const versions = (await fetchVersions())
        .map(o => o.version_number)
        .filter(o => o.match("\\+dev-[0-9a-f]{8}$"))
        .map(o => o.match("([0-9a-f]{8})$")[1]);
    if (versions.includes(shortCommit)) {
        console.log(`Version ${shortCommit} is already uploaded. Stop.`)
    }

    const version = `${modVersion}-${shortCommit}`;
    console.log(`Uploading version ${version} to modrinth`);

    const modJar = "mod/mod.jar";
    const modBuffer = await fs.readFile(modJar);
    const modBlob = new Blob([modBuffer], {"type": "application/java-archive"});

    const data = {
        "project_id": projectId,
        "name": version,
        "version_number": version,
        "changelog": `Commit ${gitCommit}\n\nUnder development. Missing lots of features.`,
        "version_type": "alpha",
        "featured": false,
        "file_parts": ["primary_file"],
        "primary_file": "primary_file",
        "game_versions": [gameVersion],
        "loaders": ["fabric"],
        "dependencies": [
            {
                "project_id": "Ha28R6CL",
                "dependency_type": "required"
            },
            {
                "project_id": "P7dR8mSH",
                "dependency_type": "required"
            },
            {
                "project_id": "mOgUt4GM",
                "dependency_type": "optional"
            },
        ],
    };
    const formData = new FormData();
    formData.append("data", JSON.stringify(data));
    formData.append("primary_file", modBlob, `ArmorStand-${version}.jar`);
    await fetchJson(`https://api.modrinth.com/v2/version`, {
        method: "POST",
        headers: {
            "User-Agent": userAgent,
            "Authorization": modrinthToken,
        },
        body: formData,
    });
})();