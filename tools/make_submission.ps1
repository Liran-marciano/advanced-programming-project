<#
.SYNOPSIS
    Builds a "test"-packaged copy of the files required for a specific
    exercise's submission to the course's grading system.

.DESCRIPTION
    The grading system requires every submitted .java file to live in
    `package test;`. The exact set of files differs per exercise (the
    course did not say "submit the whole package every time"), so this
    script knows the per-exercise file list and only copies what is
    actually required.

    The output goes to submission_ex<N>/test/<filenames>, with `package`
    and `import` statements rewritten so the files compile as a flat
    `test` package.

.PARAMETER Exercise
    Exercise number (1-5). Exercise 6 is a Git/Moodle submission and is
    not produced by this script.

.EXAMPLE
    .\tools\make_submission.ps1 1
    # produces submission_ex1/test/{Message,Topic,TopicManagerSingleton}.java

.EXAMPLE
    .\tools\make_submission.ps1 3
    # produces submission_ex3/test/{...ex2 files..., BinOpAgent, Graph, Node}.java
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 5)]
    [int]$Exercise
)

$ErrorActionPreference = 'Stop'

# --------------------------------------------------------------------------
# Per-exercise file manifests, copied straight from the Moodle instructions.
# Each entry is the file's relative path inside src/project_biu/.
# --------------------------------------------------------------------------
$manifests = @{
    1 = @(
        "graph/Message.java",
        "graph/Topic.java",
        "graph/TopicManagerSingleton.java"
    )
    2 = @(
        "graph/Message.java",
        "graph/Topic.java",
        "graph/TopicManagerSingleton.java",
        "graph/ParallelAgent.java"
    )
    3 = @(
        "graph/Message.java",
        "graph/Topic.java",
        "graph/TopicManagerSingleton.java",
        "graph/ParallelAgent.java",
        "configs/BinOpAgent.java",
        "configs/Graph.java",
        "configs/Node.java"
    )
    4 = @(
        "graph/Message.java",
        "graph/Topic.java",
        "graph/TopicManagerSingleton.java",
        "graph/ParallelAgent.java",
        "configs/PlusAgent.java",
        "configs/IncAgent.java",
        "configs/GenericConfig.java"
    )
    5 = @(
        "server/MyHTTPServer.java",
        "server/RequestParser.java"
    )
}

$files = $manifests[$Exercise]

# Files that should sit alongside the submission for local compilation /
# IDE happiness, but must NOT be uploaded -- the grader supplies them.
# Different exercises reference different supporting classes.
$referenceManifests = @{
    1 = @("graph/Agent.java")
    2 = @("graph/Agent.java")
    3 = @("graph/Agent.java")
    4 = @("graph/Agent.java", "configs/Config.java")
    5 = @("server/HTTPServer.java", "servlets/Servlet.java")
}
$referenceOnly = $referenceManifests[$Exercise]
if ($null -eq $referenceOnly) { $referenceOnly = @() }

$repoRoot = Split-Path -Parent $PSScriptRoot
$srcRoot  = Join-Path $repoRoot "src\project_biu"
$outRoot  = Join-Path $repoRoot ("submission_ex{0}" -f $Exercise)
$outDir   = Join-Path $outRoot  "test"

# Validate every file exists before we touch anything on disk.
$missing = @()
foreach ($rel in $files) {
    $full = Join-Path $srcRoot $rel
    if (-not (Test-Path $full)) { $missing += $rel }
}
if ($missing.Count -gt 0) {
    Write-Error ("Missing source files for exercise {0}:`n  - {1}" -f $Exercise, ($missing -join "`n  - "))
    exit 1
}

if (Test-Path $outRoot) { Remove-Item -Recurse -Force $outRoot }
New-Item -ItemType Directory -Path $outDir | Out-Null

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

foreach ($rel in $files) {
    $full = Join-Path $srcRoot $rel
    # Read explicitly as UTF-8 so that non-ASCII characters in source comments
    # (e.g. em-dashes) survive the round-trip. The default Get-Content on
    # Hebrew Windows uses codepage 1255 and would garble them.
    $content = [System.IO.File]::ReadAllText($full, [System.Text.Encoding]::UTF8)

    # Rewrite the package declaration.
    $content = $content -replace 'package\s+project_biu\.[a-zA-Z0-9_.]+\s*;', 'package test;'

    # Rewrite imports of other project_biu.<x>.<y> classes so they live in
    # the flat `test` package.
    $content = $content -replace 'import\s+project_biu\.[a-zA-Z0-9_]+\.([A-Za-z0-9_.]+)\s*;', 'import test.$1;'

    $name   = Split-Path $rel -Leaf
    $target = Join-Path $outDir $name
    [System.IO.File]::WriteAllText($target, $content, $utf8NoBom)
}

# Also drop reference-only files (e.g. Agent.java) into the folder so the
# IDE can resolve symbols. These are NOT part of the upload list.
foreach ($rel in $referenceOnly) {
    $full = Join-Path $srcRoot $rel
    if (-not (Test-Path $full)) { continue }
    $content = [System.IO.File]::ReadAllText($full, [System.Text.Encoding]::UTF8)
    $content = $content -replace 'package\s+project_biu\.[a-zA-Z0-9_.]+\s*;', 'package test;'
    $content = $content -replace 'import\s+project_biu\.[a-zA-Z0-9_]+\.([A-Za-z0-9_.]+)\s*;', 'import test.$1;'
    $name = Split-Path $rel -Leaf
    [System.IO.File]::WriteAllText((Join-Path $outDir $name), $content, $utf8NoBom)
}

$uploadNames = $files | ForEach-Object { Split-Path $_ -Leaf }
$referenceNames = $referenceOnly | ForEach-Object { Split-Path $_ -Leaf }

Write-Host ""
Write-Host ("Exercise {0} submission written to: {1}" -f $Exercise, $outDir)
Write-Host ""
Write-Host "Upload these files (zip them):" -ForegroundColor Green
$uploadNames | ForEach-Object { Write-Host ("  " + $_) -ForegroundColor Green }
Write-Host ""
Write-Host "Reference-only (DO NOT upload -- the grader supplies them):" -ForegroundColor Yellow
$referenceNames | ForEach-Object { Write-Host ("  " + $_) -ForegroundColor Yellow }
