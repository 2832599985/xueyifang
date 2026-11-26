#!/usr/bin/env python3
"""
学艺坊部署包组装与单一真源同步脚本

功能：
  1. --target docker|portable|all  校验测试后，将主仓库干净源码、SQL 与对应环境配置组装到答辩部署目录
  2. --sync-sql                    仅将 xueyifang-clean-init.sql 同步到各部署目录的 init-db/
  3. --clean                       清理部署目录中组装出来的源码副本（保留模板、脚本与便携环境 env/）
  4. --export-openapi              从本地 8080 Knife4j 导出最新 OpenAPI 文档到 docs/openapi.json
"""
import argparse
import json
import os
import shutil
import stat
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

SKILL_DIR = Path(__file__).resolve().parent.parent
REPO_ROOT = SKILL_DIR.parent.parent.parent
BACKEND_SRC = REPO_ROOT / "xueyifang-backend"
FRONTEND_SRC = REPO_ROOT / "xueyifang-frontend"
SQL_SOURCE = BACKEND_SRC / "script" / "xueyifang-clean-init.sql"
QA_RUNNER = REPO_ROOT / ".agents" / "skills" / "xueyifang-qa" / "scripts" / "run_qa.py"

DEPLOY_DIRS = {
    "docker": REPO_ROOT / "deploy" / "docker",
    "portable": REPO_ROOT / "deploy" / "portable",
}

BACKEND_EXCLUDES = {
    ".git", ".idea", "target", "logs", "uploads", ".mvn",
}
FRONTEND_EXCLUDES = {
    ".git", ".idea", "node_modules", "dist", "dist-ssr", ".vite",
}
SKIP_FILE_NAMES = {"nul", "codex_test.log"}
SKIP_SUFFIXES = {".log", ".iml"}


def on_rm_error(_func, path, _exc_info):
    try:
        os.chmod(path, stat.S_IWRITE)
        os.unlink(path)
    except OSError:
        pass


def _win_long(path: Path) -> str:
    resolved = str(path.resolve())
    if sys.platform == "win32" and not resolved.startswith("\\\\?\\"):
        return "\\\\?\\" + resolved
    return resolved


def safe_rmtree(path: Path) -> None:
    if not path.exists():
        return
    # Windows 下若目录内含有历史上误生成的 nul 等保留名文件，必须使用 \\?\ 前缀才能删掉
    target = Path(_win_long(path)) if sys.platform == "win32" else path
    shutil.rmtree(target, onerror=on_rm_error)


def copy_clean_tree(src: Path, dst: Path, excluded_dirs: set[str]) -> int:
    safe_rmtree(dst)
    copied = 0

    def ignore_filter(_dir: str, names: list[str]) -> set[str]:
        ignored: set[str] = set()
        for name in names:
            lower = name.lower()
            if name in excluded_dirs or lower in SKIP_FILE_NAMES or any(lower.endswith(s) for s in SKIP_SUFFIXES):
                ignored.add(name)
        return ignored

    def copy_fn(s: str, d: str) -> None:
        nonlocal copied
        shutil.copy2(s, d)
        copied += 1

    shutil.copytree(src, dst, ignore=ignore_filter, copy_function=copy_fn)
    return copied


def sync_sql(targets: list[str]) -> None:
    if not SQL_SOURCE.exists():
        raise FileNotFoundError(f"找不到 SQL 真源: {SQL_SOURCE}")
    for key in targets:
        init_dir = DEPLOY_DIRS[key] / "init-db"
        init_dir.mkdir(parents=True, exist_ok=True)
        dst = init_dir / "xueyifang-full.sql"
        shutil.copy2(SQL_SOURCE, dst)
        print(f"  [SQL] {SQL_SOURCE.name} -> {dst.relative_to(REPO_ROOT)}")


def package_target(key: str) -> None:
    deploy_dir = DEPLOY_DIRS[key]
    print(f"\n=== 组装部署包: {key} ({deploy_dir.relative_to(REPO_ROOT)}) ===")

    be_dst = deploy_dir / "xueyifang-backend"
    fe_dst = deploy_dir / "xueyifang-frontend"

    be_count = copy_clean_tree(BACKEND_SRC, be_dst, BACKEND_EXCLUDES)
    fe_count = copy_clean_tree(FRONTEND_SRC, fe_dst, FRONTEND_EXCLUDES)
    print(f"  [源码] 后端 {be_count} 个文件，前端 {fe_count} 个文件")

    # 仅保留演示必需的 uploads 目录骨架（不拷入本地开发产生的大量测试上传图）
    (be_dst / "uploads").mkdir(exist_ok=True)

    if key == "docker":
        shutil.copy2(deploy_dir / "Dockerfile.backend", be_dst / "Dockerfile")
        shutil.copy2(deploy_dir / "Dockerfile.frontend", fe_dst / "Dockerfile")
        shutil.copy2(
            deploy_dir / "application-docker.yml",
            be_dst / "src" / "main" / "resources" / "application-docker.yml",
        )
        print("  [配置] 已注入 Dockerfile 与 application-docker.yml")
    elif key == "portable":
        shutil.copy2(
            deploy_dir / "application-portable.yml",
            be_dst / "src" / "main" / "resources" / "application-portable.yml",
        )
        print("  [配置] 已注入 application-portable.yml（保留现有 env/ 运行时）")

    sync_sql([key])


def clean_targets(targets: list[str]) -> None:
    print("=== 清理部署目录中的临时源码副本 ===")
    for key in targets:
        deploy_dir = DEPLOY_DIRS[key]
        for sub in ("xueyifang-backend", "xueyifang-frontend"):
            p = deploy_dir / sub
            if p.exists():
                safe_rmtree(p)
                print(f"  [删除] {p.relative_to(REPO_ROOT)}")
            else:
                print(f"  [跳过] {p.relative_to(REPO_ROOT)} 不存在")


def export_openapi() -> int:
    url = "http://127.0.0.1:8080/api/v2/api-docs"
    out = REPO_ROOT / "deploy" / "openapi.json"
    print(f"=== 导出 OpenAPI 文档 ({url}) ===")
    try:
        with urllib.request.urlopen(url, timeout=5) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        paths_count = len(data.get("paths", {}))
        print(f"  [成功] 已导出 {paths_count} 个接口路径到 {out.relative_to(REPO_ROOT)}")
        return 0
    except urllib.error.URLError as e:
        print(f"  [失败] 无法连接后端 Knife4j 接口 ({e.reason})。请先启动后端 (8080)。")
        return 1


def run_unit_gate() -> int:
    print("=== 发布前门禁：执行离线单元测试与前端构建 ===")
    proc = subprocess.run([sys.executable, str(QA_RUNNER), "--layer", "unit"], cwd=str(REPO_ROOT))
    return proc.returncode


def main() -> int:
    if sys.platform == "win32":
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description="学艺坊部署包组装与真源同步脚本")
    parser.add_argument("--target", choices=["docker", "portable", "all"], help="组装指定部署包")
    parser.add_argument("--skip-tests", action="store_true", help="跳过打包前的 unit 层门禁校验")
    parser.add_argument("--sync-sql", action="store_true", help="仅同步 SQL 初始化脚本到部署目录")
    parser.add_argument("--clean", action="store_true", help="清理部署目录中的源码副本，恢复为模板状态")
    parser.add_argument("--export-openapi", action="store_true", help="从本地 8080 Knife4j 导出 docs/openapi.json")
    args = parser.parse_args()

    if args.clean:
        clean_targets(["docker", "portable"])
        return 0

    if args.sync_sql:
        print("=== 同步数据库初始化 SQL ===")
        sync_sql(["docker", "portable"])
        return 0

    if args.export_openapi:
        return export_openapi()

    if args.target:
        if not args.skip_tests:
            rc = run_unit_gate()
            if rc != 0:
                print("\n[BLOCKED] 测试或构建未通过，已中止打包。")
                return rc
        targets = ["docker", "portable"] if args.target == "all" else [args.target]
        for t in targets:
            package_target(t)
        print("\n=== 部署包组装完成 ===")
        return 0

    parser.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
