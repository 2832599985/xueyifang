#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
学艺坊 (Xueyifang) 真实校园场景商业级演示数据与对象存储实拍图注入脚本
- 每个用户使用独立账号登录并上传各自真实人像头像（避免 FileController 清理旧头像误删其他账号头像）
- 支持通过环境变量配置目标 API、对象存储前缀与数据库连接：
  - XUEYIFANG_API_BASE (默认 http://127.0.0.1:8080/api)
  - OSS_URL_PREFIX     (默认 http://localhost:9000/xueyifang)
  - DB_HOST / DB_PORT / DB_NAME / DB_USERNAME / DB_PASSWORD
  - REDIS_FLUSH_CMD    (可选，刷新缓存命令)
"""

import json
import os
import subprocess
import urllib.request

BASE_API = os.environ.get("XUEYIFANG_API_BASE", "http://127.0.0.1:8080/api").rstrip("/")
OSS_URL_PREFIX = os.environ.get("OSS_URL_PREFIX", "http://localhost:9000/xueyifang").rstrip("/") + "/"

def download_photo(url, fallback="https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1000&q=80"):
    for target in (url, fallback):
        try:
            req = urllib.request.Request(target, headers={
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            })
            with urllib.request.urlopen(req, timeout=20) as resp:
                return resp.read()
        except Exception as e:
            print(f"    [WARN] Failed {target}: {e}")
    raise RuntimeError(f"Failed downloading {url}")

def api_call(method, path, token=None, data=None):
    body = json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(BASE_API + path, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.loads(resp.read().decode("utf-8"))

def upload_to_us_s3(token, biz, img_bytes, filename="photo.jpg"):
    boundary = "----XyfCommercialUploadBoundary8848"
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"biz\"\r\n\r\n{biz}\r\n".encode("utf-8"),
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\nContent-Type: image/jpeg\r\n\r\n".encode("utf-8"),
        img_bytes,
        f"\r\n--{boundary}--\r\n".encode("utf-8"),
    ]
    req = urllib.request.Request(
        BASE_API + "/file/upload",
        data=b"".join(parts),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        if res.get("code") != 0:
            raise RuntimeError(f"Upload failed: {res}")
        url = res["data"] if isinstance(res["data"], str) else res["data"]["fileUrl"]
        return url

def to_relative_key(full_url):
    return full_url[len(OSS_URL_PREFIX):] if full_url.startswith(OSS_URL_PREFIX) else full_url

def run_remote_sql(sql_text):
    db_host = os.environ.get("DB_HOST", "127.0.0.1")
    db_port = os.environ.get("DB_PORT", "3306")
    db_user = os.environ.get("DB_USERNAME", "root")
    db_pass = os.environ.get("DB_PASSWORD", "")
    db_name = os.environ.get("DB_NAME", "xueyifang")
    cmd = [
        "mysql",
        f"-h{db_host}",
        f"-P{db_port}",
        f"-u{db_user}",
        "--default-character-set=utf8mb4",
        db_name,
    ]
    env = os.environ.copy()
    if db_pass:
        env["MYSQL_PWD"] = db_pass
    proc = subprocess.run(cmd, input=sql_text.encode("utf-8"), capture_output=True, env=env)
    if proc.returncode != 0:
        raise RuntimeError(f"MySQL execution failed:\n{proc.stderr.decode('utf-8', errors='replace')}")

def flush_remote_redis():
    flush_cmd = os.environ.get("REDIS_FLUSH_CMD", "").strip()
    if flush_cmd:
        subprocess.run(flush_cmd, shell=True, check=True)

def main():
    print("=== 1. 登录并上传 8 张真实人像头像（使用 service_image 通道避免跨用户清理）+ 替换 2 张场景封面图 ===")
    seller_tk = api_call("POST", "/auth/login", data={"studentId": "20240002", "password": "password123"})["data"]["token"]

    AVATAR_URLS = {
        1: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
        2: "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=400&q=80",
        3: "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80",
        4: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80",
        5: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80",
        6: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80",
        7: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80",
        8: "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=400&q=80",
    }

    avatar_keys = {}
    for uid, url in AVATAR_URLS.items():
        img = download_photo(url)
        # 用 service_image 上传不会触发 user_avatar 对同一操作者的旧头像清理，确保 8 张头像全部稳稳保留在美国 S3
        s3_url = upload_to_us_s3(seller_tk, "service_image", img, f"portrait_{uid}.jpg")
        avatar_keys[uid] = to_relative_key(s3_url)
        print(f"  [头像 {uid}] -> {s3_url}")

    # 替换之前违和的 2 张封面图（courier 换成校园骑行配送实拍，laptop_clean 换成硬件拆机/精密主板维修实拍）
    courier_new = upload_to_us_s3(
        seller_tk,
        "service_image",
        download_photo("https://images.unsplash.com/photo-1526367790999-0150786686a2?auto=format&fit=crop&w=1000&q=80"),
        "courier_bike.jpg"
    )
    print(f"  [新封面 courier] -> {courier_new}")

    laptop_new = upload_to_us_s3(
        seller_tk,
        "service_image",
        download_photo("https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?auto=format&fit=crop&w=1000&q=80"),
        "laptop_hardware.jpg"
    )
    print(f"  [新封面 laptop_clean] -> {laptop_new}")

    svc_image_keys = {
        "cs_tutor": [
            "service_image/2/202611/382cb476de9648b3ad066ad023be9249.jpg",
            "service_image/2/202611/15fc98765ac2407f8a47ae01a08be2ad.jpg",
            "service_image/2/202611/a85356ef03a04f4299c441cf6e9ebfe2.jpg",
        ],
        "campus_photo": [
            "service_image/2/202611/6d19b29d2e0e495c80f9ce7e6adc4d01.jpg",
            "service_image/2/202611/f23321379a36495b826fd7186365e8ce.jpg",
            "service_image/2/202611/1aec0b33b70942b0b0a0dfd264ac00de.jpg",
        ],
        "laptop_clean": [
            to_relative_key(laptop_new),
            "service_image/2/202611/d75d6a5df40d47df80d73b4e78c28043.jpg",
        ],
        "illustration": [
            "service_image/2/202611/735dded88ad14286b5168ad74b7595e2.jpg",
            "service_image/2/202611/984e6069816c462ba37b434eac42feb8.jpg",
        ],
        "guitar": [
            "service_image/2/202611/1ad0e559c649417a9c5a53092cc44531.jpg",
            "service_image/2/202611/76fcb2b21c5b40eeb09346c4e60f025b.jpg",
        ],
        "ielts": [
            "service_image/2/202611/cbe759af16d047b8acf6de8a9c8aec07.jpg",
            "service_image/2/202611/ee91777aa0214bffb60386ec24b2d19d.jpg",
        ],
        "courier": [
            to_relative_key(courier_new),
            "service_image/2/202611/a9fb863269014e3ca37d4842e995afad.jpg",
        ],
        "badminton": [
            "service_image/2/202611/06913747b6a1461eb116435ce0d0a8d7.jpg",
            "service_image/2/202611/f1ec0b68fb4e40a8b6aaf2505f633162.jpg",
        ],
        "ppt_design": [
            "service_image/2/202611/ecf6caa84c2047e688d2711c736fbb43.jpg",
            "service_image/2/202611/a293b04f5c594903bd9865c294be1f7a.jpg",
        ],
        "print3d": [
            "service_image/2/202611/1df7a7879cb74f3fb6c78216e0ad257f.jpg",
            "service_image/2/202611/5489dfb8e0374b689c7bf7cecae6f785.jpg",
        ],
        "math_tutor": [
            "service_image/2/202611/154f5d78cf4741f28e8b4e3e1d7957fc.jpg",
        ],
    }

    print("=== 2. 写入精简商业化标题、全员真实头像与高清图数据 ===")
    bcrypt_hash = "$2a$10$EUS.QncMvLoJFgFzpEfxV.wsqnSVjq1FFSIiCpgARG94QekZUJ7Ky"

    sql_statements = f"""
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

UPDATE `user` SET `avatar` = '{avatar_keys[1]}' WHERE `id` = 1;
UPDATE `user` SET `avatar` = '{avatar_keys[2]}' WHERE `id` = 2;
UPDATE `user` SET `avatar` = '{avatar_keys[3]}' WHERE `id` = 3;
UPDATE `user` SET `avatar` = '{avatar_keys[4]}' WHERE `id` = 4;
UPDATE `user` SET `avatar` = '{avatar_keys[5]}' WHERE `id` = 5;
UPDATE `user` SET `avatar` = '{avatar_keys[6]}' WHERE `id` = 6;
UPDATE `user` SET `avatar` = '{avatar_keys[7]}' WHERE `id` = 7;
UPDATE `user` SET `avatar` = '{avatar_keys[8]}' WHERE `id` = 8;
UPDATE `user` SET `avatar` = '{avatar_keys[2]}' WHERE `id` = 9;

-- 去除冗余【】括弧，统一为克制、高辨识度的商业级服务标题
UPDATE `service` SET `service_title` = 'SpringBoot + Vue 全栈架构 1v1 代码辅导与排障' WHERE `id` = 1;
UPDATE `service` SET `service_title` = '富士 X-T5 西区礼堂与银杏大道胶片写真约拍' WHERE `id` = 2;
UPDATE `service` SET `service_title` = '游戏本深度清灰与霍尼韦尔 7950 相变片更换' WHERE `id` = 3;
UPDATE `service` SET `service_title` = 'Procreate 日系水彩双人头像与社团海报定制' WHERE `id` = 4;
UPDATE `service` SET `service_title` = '民谣木吉他 1v1 零基础弹唱与指弹入门课' WHERE `id` = 5;
UPDATE `service` SET `service_title` = '雅思 7.5 学姐口语全真模考与大作文逐句精批' WHERE `id` = 6;
UPDATE `service` SET `service_title` = '南苑 / 松园大件快递与行李箱电动车闪送' WHERE `id` = 7;
UPDATE `service` SET `service_title` = '校队主力 1v1 羽毛球发力纠正与多球陪练' WHERE `id` = 8;
UPDATE `service` SET `service_title` = '挑战杯 / 互联网+ 国奖级路演 PPT 视觉重塑' WHERE `id` = 9;
UPDATE `service` SET `service_title` = '拓竹 P1S 高精度 FDM 3D 打印代打与模型修补' WHERE `id` = 10;
UPDATE `service` SET `service_title` = '高数与线性代数历年期末真题 3 小时押题串讲' WHERE `id` = 11;

TRUNCATE TABLE `service_image`;
INSERT INTO `service_image` (`service_id`, `image_url`, `sort_order`, `is_delete`) VALUES
(1, '{svc_image_keys["cs_tutor"][0]}', 0, 0),
(1, '{svc_image_keys["cs_tutor"][1]}', 1, 0),
(1, '{svc_image_keys["cs_tutor"][2]}', 2, 0),
(2, '{svc_image_keys["campus_photo"][0]}', 0, 0),
(2, '{svc_image_keys["campus_photo"][1]}', 1, 0),
(2, '{svc_image_keys["campus_photo"][2]}', 2, 0),
(3, '{svc_image_keys["laptop_clean"][0]}', 0, 0),
(3, '{svc_image_keys["laptop_clean"][1]}', 1, 0),
(4, '{svc_image_keys["illustration"][0]}', 0, 0),
(4, '{svc_image_keys["illustration"][1]}', 1, 0),
(5, '{svc_image_keys["guitar"][0]}', 0, 0),
(5, '{svc_image_keys["guitar"][1]}', 1, 0),
(6, '{svc_image_keys["ielts"][0]}', 0, 0),
(6, '{svc_image_keys["ielts"][1]}', 1, 0),
(7, '{svc_image_keys["courier"][0]}', 0, 0),
(7, '{svc_image_keys["courier"][1]}', 1, 0),
(8, '{svc_image_keys["badminton"][0]}', 0, 0),
(8, '{svc_image_keys["badminton"][1]}', 1, 0),
(9, '{svc_image_keys["ppt_design"][0]}', 0, 0),
(9, '{svc_image_keys["ppt_design"][1]}', 1, 0),
(10, '{svc_image_keys["print3d"][0]}', 0, 0),
(10, '{svc_image_keys["print3d"][1]}', 1, 0),
(11, '{svc_image_keys["math_tutor"][0]}', 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
"""
    run_remote_sql(sql_statements)
    flush_remote_redis()
    print("=== 3. 全部 8 张人像头像 + 2 张新封面图 + 精简商业化标题更新完成！ ===")

if __name__ == "__main__":
    main()
