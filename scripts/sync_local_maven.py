#!/usr/bin/env python3
"""Populate a local Maven-style repository from Gradle's files-2.1 cache for legacy-opensrp-libs.

The script copies all artifacts (AAR/JAR/POM/MODULE metadata) from the
legacy-opensrp-libs cache that is checked into the repository and writes them into a
local-maven/ directory using the canonical Maven repository layout. This
allows Gradle to resolve the artifacts without hitting remote repositories.
"""
from __future__ import annotations

import argparse
import os
import shutil
from pathlib import Path
from typing import Iterable

# File extensions worth copying across. Anything else (e.g. checksum files)
# is ignored to keep the mirror minimal.
ARTIFACT_EXTENSIONS = {
    ".aar",
    ".jar",
    ".pom",
    ".module",
}


def iter_artifact_files(root: Path) -> Iterable[Path]:
    """Yield all artifact files under ``root`` that match the desired extensions."""
    for path in root.rglob("*"):
        if path.is_file() and path.suffix in ARTIFACT_EXTENSIONS:
            yield path


def compute_destination(root: Path, dest_root: Path, artifact_path: Path) -> Path | None:
    """Map a cached artifact onto the target Maven repository layout.

    ``artifact_path`` looks like ``group/artifact/version/hash/file`` inside
    ``files-2.1``. We only care about the first three path components to build
    the Maven coordinates. If any part is missing we skip the file.
    """
    try:
        relative = artifact_path.relative_to(root)
    except ValueError:
        return None

    parts = relative.parts
    if len(parts) < 4:
        # Need at least group/artifact/version/hash/file
        return None

    group, artifact, version = parts[:3]
    file_name = parts[-1]

    # Convert the dotted group into path segments expected by Maven repos.
    group_path = Path(*group.split('.'))
    dest_dir = dest_root / group_path / artifact / version
    dest_dir.mkdir(parents=True, exist_ok=True)
    return dest_dir / file_name


def sync_repo(source: Path, dest: Path, overwrite: bool = False) -> list[Path]:
    """Copy all artifacts from ``source`` to ``dest`` using the Maven layout."""
    copied = []
    for artifact_path in iter_artifact_files(source):
        target_path = compute_destination(source, dest, artifact_path)
        if target_path is None:
            continue
        if not overwrite and target_path.exists():
            continue
        shutil.copy2(artifact_path, target_path)
        copied.append(target_path)
    return copied


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source",
        type=Path,
        default=Path("legacy-opensrp-libs"),
        help="Path to the Gradle legacy-opensrp-libs cache",
    )
    parser.add_argument(
        "--dest",
        type=Path,
        default=Path("local-maven"),
        help="Destination directory for the generated Maven repository",
    )
    parser.add_argument(
        "destination",
        nargs="?",
        type=Path,
        help=(
            "Optional positional destination overriding --dest; useful for paths like"
            " ~/.m2/repository"
        ),
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Overwrite existing files in the destination",
    )
    args = parser.parse_args()

    source_root = args.source.expanduser()
    dest_root = (args.destination or args.dest).expanduser()

    if not source_root.exists():
        parser.error(f"Source cache directory {source_root} does not exist")

    dest_root.mkdir(parents=True, exist_ok=True)
    copied = sync_repo(source_root, dest_root, overwrite=args.overwrite)

    print(f"Copied {len(copied)} artifacts into {dest_root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
