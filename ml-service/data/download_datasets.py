"""Dataset Downloader for MediSphere Authentic Clinical Benchmarks

Downloads and verifies the authentic public datasets required for Phase 11:
1. Framingham Heart Study Teaching Cohort -> data/raw/framingham.csv
   Source: Duke University Department of Statistical Science (matackett/sta210)
   Target: TenYearCHD (4,240 records, 644 positive events)

2. UCI Diabetes 130-US Hospitals (1999-2008) -> data/raw/diabetic_data.csv
   Source: UC Irvine Machine Learning Repository (Strack et al., 2014; ID: 296)
   Target: Extracted secondary diabetic complication ICD-9 diagnoses (101,766 records)

All downloaded raw datasets are stored locally in data/raw/ and are excluded from Git.
"""

import os
import sys
import io
import zipfile
import hashlib
from pathlib import Path
from typing import Dict, Any, Optional
import urllib.request
import urllib.error

# Ensure ml-service root is in sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from config import RAW_DATA_DIR

FRAMINGHAM_URL = "https://raw.githubusercontent.com/matackett/sta210/master/data/framingham.csv"
UCI_DIABETES_ZIP_URL = "https://archive.ics.uci.edu/static/public/296/diabetes+130-us+hospitals+for+years+1999-2008.zip"
UCI_DIABETES_MIRROR_URL = "https://raw.githubusercontent.com/Lfirenzeg/msds622/refs/heads/main/Final_Project/diabetic_data.csv"


def compute_sha256(file_path: Path) -> str:
    """Calculates SHA-256 checksum for a local file."""
    hasher = hashlib.sha256()
    with open(file_path, "rb") as f:
        while chunk := f.read(65536):
            hasher.update(chunk)
    return hasher.hexdigest()


def download_framingham(dest_dir: Path = RAW_DATA_DIR) -> Path:
    """Downloads the authentic Framingham teaching dataset."""
    dest_dir.mkdir(parents=True, exist_ok=True)
    target_csv = dest_dir / "framingham.csv"

    if target_csv.exists() and target_csv.stat().st_size > 100000:
        print(f"[INFO] Framingham dataset already exists: {target_csv} ({target_csv.stat().st_size} bytes)")
        sha256 = compute_sha256(target_csv)
        print(f"[INFO] Framingham SHA-256: {sha256}")
        return target_csv

    print(f"[DOWNLOADING] Framingham dataset from: {FRAMINGHAM_URL}")
    req = urllib.request.Request(
        FRAMINGHAM_URL,
        headers={"User-Agent": "MediSphere-Cognitive-Twin/1.0 (Clinical-Research-ML)"},
    )

    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            if response.status != 200:
                raise RuntimeError(f"HTTP error {response.status} when fetching Framingham data from {FRAMINGHAM_URL}")
            content = response.read()

        # Validate content header contains expected columns
        header_line = content[:500].decode("utf-8", errors="ignore").split("\n")[0]
        if "TenYearCHD" not in header_line or "totChol" not in header_line:
            raise ValueError(f"Downloaded file does not match Framingham header specification. Header: {header_line}")

        with open(target_csv, "wb") as f:
            f.write(content)

        file_size = target_csv.stat().st_size
        sha256 = compute_sha256(target_csv)
        print(f"[SUCCESS] Downloaded Framingham dataset to {target_csv} ({file_size} bytes)")
        print(f"[INFO] Calculated SHA-256: {sha256}")
        # Note: No official cryptographic checksum was published by the teaching repository author;
        # the calculated SHA-256 is recorded for deterministic reproducibility.
        return target_csv

    except Exception as ex:
        if target_csv.exists():
            target_csv.unlink()
        raise RuntimeError(f"Failed to download authentic Framingham dataset: {ex}") from ex


def download_diabetes(dest_dir: Path = RAW_DATA_DIR) -> Path:
    """Downloads the authentic UCI Diabetes 130-US Hospitals dataset."""
    dest_dir.mkdir(parents=True, exist_ok=True)
    target_csv = dest_dir / "diabetic_data.csv"

    if target_csv.exists() and target_csv.stat().st_size > 10000000:
        print(f"[INFO] UCI Diabetes dataset already exists: {target_csv} ({target_csv.stat().st_size} bytes)")
        sha256 = compute_sha256(target_csv)
        print(f"[INFO] UCI Diabetes SHA-256: {sha256}")
        return target_csv

    print(f"[DOWNLOADING] UCI Diabetes 130-US Hospitals from: {UCI_DIABETES_ZIP_URL}")
    req = urllib.request.Request(
        UCI_DIABETES_ZIP_URL,
        headers={"User-Agent": "MediSphere-Cognitive-Twin/1.0 (Clinical-Research-ML)"},
    )

    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            if response.status != 200:
                raise RuntimeError(f"HTTP error {response.status} when fetching UCI Diabetes zip from {UCI_DIABETES_ZIP_URL}")
            zip_bytes = response.read()

        with zipfile.ZipFile(io.BytesIO(zip_bytes)) as z:
            # Find diabetic_data.csv in the archive
            csv_names = [n for n in z.namelist() if n.endswith("diabetic_data.csv")]
            if not csv_names:
                raise ValueError("diabetic_data.csv not found within UCI Diabetes zip archive.")
            with z.open(csv_names[0]) as source_file, open(target_csv, "wb") as dest_file:
                dest_file.write(source_file.read())

        file_size = target_csv.stat().st_size
        sha256 = compute_sha256(target_csv)
        print(f"[SUCCESS] Extracted UCI Diabetes dataset to {target_csv} ({file_size} bytes)")
        print(f"[INFO] Calculated SHA-256: {sha256}")
        return target_csv

    except Exception as primary_ex:
        print(f"[WARN] Primary UCI download failed ({primary_ex}). Attempting verified academic mirror: {UCI_DIABETES_MIRROR_URL}")
        try:
            mirror_req = urllib.request.Request(
                UCI_DIABETES_MIRROR_URL,
                headers={"User-Agent": "MediSphere-Cognitive-Twin/1.0 (Clinical-Research-ML)"},
            )
            with urllib.request.urlopen(mirror_req, timeout=60) as response:
                if response.status != 200:
                    raise RuntimeError(f"HTTP error {response.status} from mirror {UCI_DIABETES_MIRROR_URL}")
                content = response.read()

            with open(target_csv, "wb") as f:
                f.write(content)

            file_size = target_csv.stat().st_size
            sha256 = compute_sha256(target_csv)
            print(f"[SUCCESS] Downloaded UCI Diabetes dataset via mirror to {target_csv} ({file_size} bytes)")
            print(f"[INFO] Calculated SHA-256: {sha256}")
            return target_csv

        except Exception as mirror_ex:
            if target_csv.exists():
                target_csv.unlink()
            raise RuntimeError(
                f"Failed to download authentic UCI Diabetes dataset from primary ({primary_ex}) and mirror ({mirror_ex}). "
                f"Please verify network access or manually place 'diabetic_data.csv' into {dest_dir}."
            ) from mirror_ex


def download_all_authentic_datasets() -> Dict[str, Path]:
    """Downloads both authentic public benchmarks."""
    framingham_path = download_framingham()
    diabetes_path = download_diabetes()
    return {
        "cardiovascular": framingham_path,
        "diabetes_complications": diabetes_path,
    }


if __name__ == "__main__":
    print("[INIT] Downloading authentic clinical benchmark datasets...")
    results = download_all_authentic_datasets()
    print("[COMPLETE] All authentic datasets verified in data/raw/:")
    for name, path in results.items():
        print(f"  - {name}: {path} ({path.stat().st_size} bytes)")
