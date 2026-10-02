#!/usr/bin/env python3
"""Fail if an APK's 64-bit native libraries cannot run with 16 KB pages.

Checks both ELF LOAD/RELRO alignment and uncompressed ZIP entry alignment.
32-bit libraries are reported but are not subject to Android's 16 KB requirement.
Uses only the Python standard library; it does not modify the APK.
"""

import argparse
import json
from pathlib import Path
import struct
import sys
import zipfile

PAGE_SIZE = 16384
PT_LOAD = 1
PT_GNU_RELRO = 0x6474E552


def check_library(data):
    if len(data) < 64 or data[:4] != b"\x7fELF" or data[4:6] != b"\x02\x01":
        raise ValueError("expected a little-endian ELF64 library")
    offset = struct.unpack_from("<Q", data, 32)[0]
    entry_size, count = struct.unpack_from("<HH", data, 54)
    if entry_size < 56 or offset + entry_size * count > len(data):
        raise ValueError("invalid ELF program-header table")
    segments = [struct.unpack_from("<IIQQQQQQ", data, offset + i * entry_size)
                for i in range(count)]
    loads = [s for s in segments if s[0] == PT_LOAD]
    errors = []
    if not loads:
        errors.append("no LOAD segments")
    for s in loads:
        _, _, file_offset, address, _, _, _, alignment = s
        if alignment < PAGE_SIZE or alignment & (alignment - 1):
            errors.append(f"LOAD alignment {alignment} is not a power of two >= 16384")
        if (address - file_offset) % PAGE_SIZE:
            errors.append("LOAD address and file offset differ modulo 16384")
    for s in segments:
        if s[0] == PT_GNU_RELRO and (s[3] + s[6]) % PAGE_SIZE:
            errors.append(f"RELRO end 0x{s[3] + s[6]:x} is not 16 KB aligned")
    return errors


def check_apk(path):
    results = []
    with path.open("rb") as raw, zipfile.ZipFile(path) as apk:
        for entry in apk.infolist():
            if not entry.filename.startswith("lib/") or not entry.filename.endswith(".so"):
                continue
            abi = entry.filename.split("/")[1]
            if abi not in ("arm64-v8a", "x86_64"):
                results.append({"library": entry.filename, "status": "32-bit", "errors": []})
                continue
            try:
                errors = check_library(apk.read(entry))
                if entry.compress_type == zipfile.ZIP_STORED:
                    raw.seek(entry.header_offset)
                    header = raw.read(30)
                    if len(header) != 30 or header[:4] != b"PK\x03\x04":
                        raise ValueError("invalid ZIP local header")
                    name_length, extra_length = struct.unpack_from("<HH", header, 26)
                    data_offset = entry.header_offset + 30 + name_length + extra_length
                    if data_offset % PAGE_SIZE:
                        errors.append(f"uncompressed ZIP data offset {data_offset} is not 16 KB aligned")
            except (ValueError, struct.error) as error:
                errors = [str(error)]
            results.append({"library": entry.filename,
                            "status": "FAIL" if errors else "PASS", "errors": errors})
    if not any(r["status"] in ("PASS", "FAIL") for r in results):
        raise ValueError("APK contains no supported 64-bit native libraries")
    return {"apk": str(path), "passed": all(r["status"] != "FAIL" for r in results),
            "libraries": results}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path, nargs="+")
    parser.add_argument("--json-output", type=Path)
    args = parser.parse_args()
    results = []
    for path in args.apk:
        try:
            result = check_apk(path)
        except (OSError, ValueError, zipfile.BadZipFile) as error:
            result = {"apk": str(path), "passed": False, "error": str(error), "libraries": []}
        results.append(result)
        print(f"{'PASS' if result['passed'] else 'FAIL'} {path}")
        for library in result["libraries"]:
            print(f"  {library['status']}: {library['library']}")
            for error in library["errors"]:
                print(f"    {error}")
        if "error" in result:
            print(f"  {result['error']}")
    if args.json_output:
        args.json_output.write_text(json.dumps(results, indent=2) + "\n")
    return 0 if all(result["passed"] for result in results) else 1


if __name__ == "__main__":
    sys.exit(main())
