import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import tomllib
import unittest
import zipfile

from src.server import SERVER_VERSION


class WheelArtifactTest(unittest.TestCase):
    def test_builds_secret_free_runtime_wheel_without_repository_checkout(self):
        project_root = Path(__file__).resolve().parents[1]
        project_version = tomllib.loads((project_root / "pyproject.toml").read_text())["project"]["version"]
        self.assertEqual(project_version, SERVER_VERSION)
        with tempfile.TemporaryDirectory(prefix="tbank-banking-wheel-") as temporary:
            isolated_source = Path(temporary) / "source"
            isolated_source.mkdir()
            for name in ["pyproject.toml", "README.md", "LICENSE.upstream", "UPSTREAM.md"]:
                shutil.copy2(project_root / name, isolated_source / name)
            shutil.copytree(project_root / "src", isolated_source / "src", ignore=shutil.ignore_patterns("__pycache__", "*.pyc"))
            wheel_directory = Path(temporary) / "wheel"
            wheel_directory.mkdir()
            completed = subprocess.run(
                [
                    sys.executable, "-m", "pip", "wheel", "--no-deps",
                    "--no-build-isolation", "--wheel-dir", str(wheel_directory),
                    str(isolated_source),
                ],
                text=True,
                capture_output=True,
                env={
                    "PATH": os.environ.get("PATH", ""),
                    "PIP_NO_INDEX": "1",
                    "PIP_DISABLE_PIP_VERSION_CHECK": "1",
                    "PIP_CACHE_DIR": str(Path(temporary) / "pip-cache"),
                },
            )
            self.assertEqual(completed.returncode, 0, completed.stderr)
            wheels = list(wheel_directory.glob("*.whl"))
            self.assertEqual(len(wheels), 1)
            with zipfile.ZipFile(wheels[0]) as archive:
                names = archive.namelist()
                rendered = "\n".join(names).lower()
                self.assertTrue(any(name == "src/server.py" for name in names))
                self.assertTrue(any(name == "src/login_cli.py" for name in names))
                self.assertTrue(any(name.endswith(".dist-info/METADATA") for name in names))
                self.assertTrue(any(name.endswith(".dist-info/entry_points.txt") for name in names))
                for forbidden in ["/.env", "session.json", "message (", "/test/", "__pycache__", ".pyc"]:
                    self.assertNotIn(forbidden, rendered)
                metadata_name = next(name for name in names if name.endswith(".dist-info/METADATA"))
                metadata = archive.read(metadata_name).decode("utf-8")
                self.assertIn(f"Version: {SERVER_VERSION}", metadata)
                entry_points_name = next(name for name in names if name.endswith(".dist-info/entry_points.txt"))
                entry_points = archive.read(entry_points_name).decode("utf-8")
                self.assertIn("tbank-banking-login = src.login_cli:main", entry_points)

            installed = Path(temporary) / "installed"
            install = subprocess.run(
                [sys.executable, "-m", "pip", "install", "--no-deps", "--no-index", "--target", str(installed), str(wheels[0])],
                text=True,
                capture_output=True,
                env={
                    "PATH": os.environ.get("PATH", ""),
                    "PIP_DISABLE_PIP_VERSION_CHECK": "1",
                    "PIP_CACHE_DIR": str(Path(temporary) / "pip-cache"),
                },
            )
            self.assertEqual(install.returncode, 0, install.stderr)
            initialized = subprocess.run(
                [sys.executable, "-m", "src.server"],
                input=json.dumps({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {}}) + "\n",
                text=True,
                capture_output=True,
                cwd=temporary,
                env={
                    "PATH": os.environ.get("PATH", ""),
                    "PYTHONPATH": str(installed),
                    "TBANK_BANKING_SESSION": str(Path(temporary) / "missing-session.json"),
                },
            )
            self.assertEqual(initialized.returncode, 0, initialized.stderr)
            response = json.loads(initialized.stdout.strip())
            self.assertEqual(response["result"]["serverInfo"]["version"], SERVER_VERSION)

            logged_out = subprocess.run(
                [sys.executable, "-m", "src.login_cli", "--logout"],
                text=True,
                capture_output=True,
                cwd=temporary,
                env={
                    "PATH": os.environ.get("PATH", ""),
                    "PYTHONPATH": str(installed),
                    "TBANK_BANKING_SESSION": str(Path(temporary) / "missing-session.json"),
                },
            )
            self.assertEqual(logged_out.returncode, 0, logged_out.stderr)
            self.assertIn("уже отсутствует", logged_out.stdout)


if __name__ == "__main__":
    unittest.main()
