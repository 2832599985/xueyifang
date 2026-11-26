#!/usr/bin/env python3
"""Coverage gap tests for xueyifang API - tests endpoints not covered by other testers."""

import json
import sys
import urllib.request
import urllib.parse
import urllib.error
import os
import tempfile
import time

BASE_URL = os.environ.get("XUEYIFANG_API_BASE", "http://127.0.0.1:8080/api").rstrip("/")
SERVER_ROOT = BASE_URL[:-4] if BASE_URL.endswith("/api") else BASE_URL

PASS_COUNT = 0
FAIL_COUNT = 0
RESULTS = []

def log(status, step, detail, actual="", expected=""):
    global PASS_COUNT, FAIL_COUNT
    if status == "PASS":
        PASS_COUNT += 1
    else:
        FAIL_COUNT += 1
    msg = f"[{status}] {step} - {detail}"
    if actual:
        msg += f"\n  实际值: {actual}"
    if expected:
        msg += f"\n  预期值: {expected}"
    print(msg)
    RESULTS.append({"status": status, "step": step, "detail": detail, "actual": actual, "expected": expected})

def api_request(method, path, data=None, token=None, raw_response=False):
    """Make HTTP request and return parsed JSON response."""
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    body = None
    if data is not None:
        body = json.dumps(data).encode("utf-8")

    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req, timeout=10)
        resp_body = resp.read().decode("utf-8")
        if raw_response:
            return resp.status, resp_body
        return json.loads(resp_body)
    except urllib.error.HTTPError as e:
        resp_body = e.read().decode("utf-8")
        if raw_response:
            return e.code, resp_body
        try:
            return json.loads(resp_body)
        except:
            return {"code": e.code, "message": resp_body}
    except Exception as e:
        return {"code": -1, "message": str(e)}

def api_get(path, token=None, raw_response=False):
    return api_request("GET", path, token=token, raw_response=raw_response)

def api_post(path, data=None, token=None):
    return api_request("POST", path, data=data, token=token)

def api_put(path, data=None, token=None):
    return api_request("PUT", path, data=data, token=token)

def api_delete(path, token=None):
    return api_request("DELETE", path, token=token)

def multipart_upload(path, file_path, field_name="file", extra_fields=None, token=None):
    """Upload a file using multipart/form-data."""
    import mimetypes
    boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
    lines = []

    # Add extra fields
    if extra_fields:
        for key, val in extra_fields.items():
            lines.append(f"--{boundary}")
            lines.append(f'Content-Disposition: form-data; name="{key}"')
            lines.append("")
            lines.append(val)

    # Add file
    filename = os.path.basename(file_path)
    mime_type = mimetypes.guess_type(file_path)[0] or "application/octet-stream"
    with open(file_path, "rb") as f:
        file_content = f.read()

    body_parts = []
    for line in lines:
        body_parts.append(line.encode("utf-8"))
        body_parts.append(b"\r\n")

    body_parts.append(f"--{boundary}\r\n".encode("utf-8"))
    body_parts.append(f'Content-Disposition: form-data; name="{field_name}"; filename="{filename}"\r\n'.encode("utf-8"))
    body_parts.append(f"Content-Type: {mime_type}\r\n\r\n".encode("utf-8"))
    body_parts.append(file_content)
    body_parts.append(f"\r\n--{boundary}--\r\n".encode("utf-8"))

    body = b"".join(body_parts)

    url = f"{BASE_URL}{path}"
    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        resp = urllib.request.urlopen(req, timeout=15)
        resp_body = resp.read().decode("utf-8")
        return json.loads(resp_body)
    except urllib.error.HTTPError as e:
        resp_body = e.read().decode("utf-8")
        try:
            return json.loads(resp_body)
        except:
            return {"code": e.code, "message": resp_body}
    except Exception as e:
        return {"code": -1, "message": str(e)}

def login(student_id, password):
    resp = api_post("/auth/login", {"studentId": student_id, "password": password})
    if resp.get("code") == 0 and resp.get("data", {}).get("token"):
        return resp["data"]["token"]
    print(f"  [DEBUG] login failed for {student_id}: {resp}")
    return None


def main():
    print("=" * 60)
    print("学艺坊 补充覆盖测试")
    print("=" * 60)

    # ===== Step 1: Login =====
    print("\n--- 第一步: 登录 ---")
    ADMIN_TOKEN = login("20240001", "password123")
    if ADMIN_TOKEN:
        log("PASS", "1.1 管理员登录", "获取Token成功", ADMIN_TOKEN[:30] + "...", "非空Token")
    else:
        log("FAIL", "1.1 管理员登录", "获取Token失败")
        print("FATAL: 管理员登录失败，无法继续测试")
        sys.exit(1)

    time.sleep(1)  # Avoid rate limiting

    SELLER_TOKEN = login("20240002", "password123")
    if SELLER_TOKEN:
        log("PASS", "1.2 卖家登录", "获取Token成功", SELLER_TOKEN[:30] + "...", "非空Token")
    else:
        log("FAIL", "1.2 卖家登录", "获取Token失败")
        sys.exit(1)

    time.sleep(1)  # Avoid rate limiting

    BUYER_TOKEN = login("20240003", "password123")
    if BUYER_TOKEN:
        log("PASS", "1.3 买家登录", "获取Token成功", BUYER_TOKEN[:30] + "...", "非空Token")
    else:
        log("FAIL", "1.3 买家登录", "获取Token失败")
        sys.exit(1)

    # ===== Step 2: User Auth Full Tests =====
    print("\n--- 第二步: 用户认证功能完整测试 ---")

    # 2.1 Get current user info
    resp = api_get("/auth/currentUser", token=BUYER_TOKEN)
    if resp.get("code") == 0 and resp.get("data"):
        user_data = resp["data"]
        has_fields = all(k in user_data for k in ["id", "studentId", "realName"])
        if has_fields:
            log("PASS", "2.1 获取当前用户信息", "返回完整用户信息",
                f"id={user_data['id']}, studentId={user_data['studentId']}, realName={user_data.get('realName')}",
                "包含 id, studentId, realName")
        else:
            log("FAIL", "2.1 获取当前用户信息", "缺少必要字段", str(user_data.keys()), "id, studentId, realName")
    else:
        log("FAIL", "2.1 获取当前用户信息", "请求失败", str(resp), "code=0, data非空")

    # 2.2 Update profile
    update_data = {"nickname": "测试昵称修改", "bio": "这是修改后的个人简介", "dormitory": "3号楼201"}
    resp = api_put("/auth/updateProfile", update_data, token=BUYER_TOKEN)
    if resp.get("code") == 0:
        log("PASS", "2.2a 修改个人资料", "修改请求成功", f"code={resp['code']}", "code=0")
    else:
        log("FAIL", "2.2a 修改个人资料", "修改请求失败", str(resp), "code=0")

    # Verify profile update
    resp = api_get("/auth/currentUser", token=BUYER_TOKEN)
    if resp.get("code") == 0 and resp.get("data"):
        user = resp["data"]
        nickname_ok = user.get("nickname") == "测试昵称修改"
        bio_ok = user.get("bio") == "这是修改后的个人简介"
        dormitory_ok = user.get("dormitory") == "3号楼201"
        if nickname_ok and bio_ok and dormitory_ok:
            log("PASS", "2.2b 验证资料修改", "所有字段修改生效",
                f"nickname={user['nickname']}, bio={user['bio']}, dormitory={user['dormitory']}",
                "nickname=测试昵称修改, bio=这是修改后的个人简介, dormitory=3号楼201")
        else:
            log("FAIL", "2.2b 验证资料修改", "部分字段未生效",
                f"nickname={user.get('nickname')}, bio={user.get('bio')}, dormitory={user.get('dormitory')}",
                "nickname=测试昵称修改, bio=这是修改后的个人简介, dormitory=3号楼201")
    else:
        log("FAIL", "2.2b 验证资料修改", "获取用户信息失败", str(resp), "code=0")

    # 2.3 Change password (requires confirmPassword field)
    resp = api_put("/auth/changePassword", {"oldPassword": "password123", "newPassword": "newpass456", "confirmPassword": "newpass456"}, token=BUYER_TOKEN)
    if resp.get("code") == 0:
        log("PASS", "2.3a 修改密码", "密码修改成功", f"code={resp['code']}", "code=0")
    else:
        log("FAIL", "2.3a 修改密码", "密码修改失败", str(resp), "code=0")

    time.sleep(2)  # Wait before login attempt to avoid rate limiting

    # Login with new password
    new_token = login("20240003", "newpass456")
    if new_token:
        log("PASS", "2.3b 新密码登录", "新密码登录成功", "获取到新Token", "登录成功")
        BUYER_TOKEN = new_token
    else:
        log("FAIL", "2.3b 新密码登录", "新密码登录失败", "Token为空", "登录成功")

    time.sleep(1)

    # Change back
    resp = api_put("/auth/changePassword", {"oldPassword": "newpass456", "newPassword": "password123", "confirmPassword": "password123"}, token=BUYER_TOKEN)
    if resp.get("code") == 0:
        log("PASS", "2.3c 恢复原密码", "密码恢复成功", f"code={resp['code']}", "code=0")
    else:
        log("FAIL", "2.3c 恢复原密码", "密码恢复失败", str(resp), "code=0")

    time.sleep(2)

    # Verify old password works
    verify_token = login("20240003", "password123")
    if verify_token:
        log("PASS", "2.3d 原密码登录验证", "原密码登录成功", "获取到Token", "登录成功")
        BUYER_TOKEN = verify_token
    else:
        log("FAIL", "2.3d 原密码登录验证", "原密码登录失败", "Token为空", "登录成功")

    # 2.4 Logout
    resp = api_post("/auth/logout", token=BUYER_TOKEN)
    if resp.get("code") == 0:
        log("PASS", "2.4a 登出", "登出成功", f"code={resp['code']}", "code=0")
    else:
        log("FAIL", "2.4a 登出", "登出失败", str(resp), "code=0")

    # Verify token invalidated
    resp = api_get("/auth/currentUser", token=BUYER_TOKEN)
    if resp.get("code") == 40103 or resp.get("code") == 40100:
        log("PASS", "2.4b 验证Token失效", "Token已失效",
            f"code={resp.get('code')}", "code=40103或40100")
    else:
        log("FAIL", "2.4b 验证Token失效", "Token未失效",
            f"code={resp.get('code')}, message={resp.get('message')}", "code=40103或40100")

    # Re-login
    time.sleep(2)  # Avoid rate limiting
    BUYER_TOKEN = login("20240003", "password123")
    if BUYER_TOKEN:
        log("PASS", "2.4c 重新登录", "重新登录成功", "获取到新Token", "登录成功")
    else:
        log("FAIL", "2.4c 重新登录", "重新登录失败")
        sys.exit(1)

    # ===== Step 3: Wallet Withdrawal Tests =====
    print("\n--- 第三步: 钱包提现测试 ---")

    # 3.1 Check balance (returns WalletBalanceVO with walletBalance, frozenAmount, totalAmount)
    resp = api_get("/wallet/balance", token=BUYER_TOKEN)
    initial_balance = None
    if resp.get("code") == 0 and resp.get("data") is not None:
        balance_data = resp["data"]
        if isinstance(balance_data, dict):
            initial_balance = balance_data.get("walletBalance", 0)
        else:
            initial_balance = balance_data
        log("PASS", "3.1 查询余额", "获取余额成功",
            f"walletBalance={initial_balance}, data={balance_data}", "code=0, data非空")
    else:
        log("FAIL", "3.1 查询余额", "获取余额失败", str(resp), "code=0")

    # 3.2 Normal withdrawal
    if initial_balance is not None and float(str(initial_balance)) >= 10:
        resp = api_post("/wallet/withdraw", {"amount": 10.00}, token=BUYER_TOKEN)
        if resp.get("code") == 0:
            log("PASS", "3.2a 正常提现", "提现请求成功", f"code={resp['code']}", "code=0")
        else:
            log("FAIL", "3.2a 正常提现", "提现请求失败", str(resp), "code=0")

        # Verify balance decreased
        resp = api_get("/wallet/balance", token=BUYER_TOKEN)
        if resp.get("code") == 0:
            balance_data = resp["data"]
            if isinstance(balance_data, dict):
                new_balance = balance_data.get("walletBalance", 0)
            else:
                new_balance = balance_data
            expected_balance = float(str(initial_balance)) - 10
            if abs(float(str(new_balance)) - expected_balance) < 0.01:
                log("PASS", "3.2b 验证余额减少", "余额正确减少",
                    f"新余额={new_balance}", f"预期={expected_balance}")
            else:
                log("FAIL", "3.2b 验证余额减少", "余额不正确",
                    f"新余额={new_balance}", f"预期={expected_balance}")
        else:
            log("FAIL", "3.2b 验证余额减少", "查询余额失败", str(resp), "code=0")

        # Verify transaction record
        resp = api_get("/wallet/transactions?pageNum=1&pageSize=5", token=BUYER_TOKEN)
        if resp.get("code") == 0 and resp.get("data"):
            records = resp["data"].get("records", resp["data"].get("list", []))
            if records and len(records) > 0:
                latest = records[0]
                tx_type = latest.get("transactionType", latest.get("type"))
                if tx_type == 2:
                    log("PASS", "3.2c 验证交易记录", "最新记录为提现",
                        f"transactionType={tx_type}", "transactionType=2")
                else:
                    log("FAIL", "3.2c 验证交易记录", "最新记录类型不是提现",
                        f"transactionType={tx_type}", "transactionType=2")
            else:
                log("FAIL", "3.2c 验证交易记录", "无交易记录", str(resp["data"]), "至少1条记录")
        else:
            log("FAIL", "3.2c 验证交易记录", "获取交易记录失败", str(resp), "code=0")
    else:
        log("FAIL", "3.2 正常提现", "余额不足10元，跳过提现测试", f"余额={initial_balance}", ">=10")

    # 3.3 Withdraw exceeding balance
    resp = api_post("/wallet/withdraw", {"amount": 9999999.00}, token=BUYER_TOKEN)
    if resp.get("code") != 0:
        log("PASS", "3.3 提现超出余额", "正确拒绝超额提现",
            f"code={resp.get('code')}, message={resp.get('message')}", "code!=0")
    else:
        log("FAIL", "3.3 提现超出余额", "未拒绝超额提现", str(resp), "code!=0")

    # 3.4 Withdraw negative amount
    resp = api_post("/wallet/withdraw", {"amount": -10.00}, token=BUYER_TOKEN)
    if resp.get("code") != 0:
        log("PASS", "3.4 提现负数金额", "正确拒绝负数提现",
            f"code={resp.get('code')}, message={resp.get('message')}", "code!=0")
    else:
        log("FAIL", "3.4 提现负数金额", "未拒绝负数提现", str(resp), "code!=0")

    # ===== Step 4: Notification Management =====
    print("\n--- 第四步: 通知管理测试 ---")

    # 4.1 Get notifications
    resp = api_get("/notification/my-notifications?pageNum=1&pageSize=10", token=SELLER_TOKEN)
    notification_id = None
    if resp.get("code") == 0 and resp.get("data") is not None:
        data = resp["data"]
        records = data.get("records", data.get("list", []))
        log("PASS", "4.1 获取通知列表", "返回分页数据",
            f"记录数={len(records) if records else 0}", "code=0, 分页数据")
        # Find an unread notification
        if records:
            for notif in records:
                if notif.get("isRead") == 0 or notif.get("read") == False:
                    notification_id = notif.get("id")
                    break
            if not notification_id and records:
                notification_id = records[0].get("id")
    else:
        log("FAIL", "4.1 获取通知列表", "获取失败", str(resp), "code=0")

    # 4.2 Get unread count
    resp = api_get("/notification/unreadCount", token=SELLER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") is not None:
        unread_before = resp["data"]
        log("PASS", "4.2 获取未读数量", "获取成功", f"未读数={unread_before}", "code=0")
    else:
        log("FAIL", "4.2 获取未读数量", "获取失败", str(resp), "code=0")
        unread_before = None

    # 4.3 Mark single as read
    if notification_id:
        resp = api_post(f"/notification/{notification_id}/read", token=SELLER_TOKEN)
        if resp.get("code") == 0:
            log("PASS", "4.3a 标记单条已读", "标记成功", f"id={notification_id}, code={resp['code']}", "code=0")
        else:
            log("FAIL", "4.3a 标记单条已读", "标记失败", str(resp), "code=0")

        # Verify unread count
        if unread_before is not None and unread_before > 0:
            resp = api_get("/notification/unreadCount", token=SELLER_TOKEN)
            if resp.get("code") == 0:
                unread_after = resp["data"]
                if unread_after <= unread_before:
                    log("PASS", "4.3b 验证未读数减少", "未读数正确变化",
                        f"变化前={unread_before}, 变化后={unread_after}", "变化后<=变化前")
                else:
                    log("FAIL", "4.3b 验证未读数减少", "未读数未减少",
                        f"变化前={unread_before}, 变化后={unread_after}", "变化后<=变化前")
            else:
                log("FAIL", "4.3b 验证未读数减少", "获取未读数失败", str(resp), "code=0")
        else:
            log("PASS", "4.3b 验证未读数减少", "无未读通知或未读数为0，跳过验证", f"unread_before={unread_before}", "N/A")
    else:
        log("PASS", "4.3 标记单条已读", "无通知可标记，跳过", "通知列表为空", "N/A")

    # 4.4 Mark all as read
    resp = api_post("/notification/readAll", token=SELLER_TOKEN)
    if resp.get("code") == 0:
        log("PASS", "4.4a 标记全部已读", "标记成功", f"code={resp['code']}", "code=0")
    else:
        log("FAIL", "4.4a 标记全部已读", "标记失败", str(resp), "code=0")

    resp = api_get("/notification/unreadCount", token=SELLER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") == 0:
        log("PASS", "4.4b 验证全部已读", "未读数为0", f"未读数={resp['data']}", "0")
    elif resp.get("code") == 0:
        log("FAIL", "4.4b 验证全部已读", "未读数不为0", f"未读数={resp.get('data')}", "0")
    else:
        log("FAIL", "4.4b 验证全部已读", "获取未读数失败", str(resp), "code=0, data=0")

    # ===== Step 5: Service Tags =====
    print("\n--- 第五步: 服务标签 ---")
    resp = api_get("/service/tags")
    if resp.get("code") == 0 and resp.get("data"):
        tags = resp["data"]
        if isinstance(tags, list) and len(tags) > 0:
            log("PASS", "5.1 获取服务标签", "返回标签列表",
                f"标签数={len(tags)}, 第一个={tags[0]}", "非空列表")
        else:
            log("FAIL", "5.1 获取服务标签", "标签列表为空", str(tags), "非空列表")
    else:
        log("FAIL", "5.1 获取服务标签", "请求失败", str(resp), "code=0, 非空列表")

    # ===== Step 6: Seller Statistics =====
    print("\n--- 第六步: 卖家统计 ---")
    resp = api_get("/statistics/sales", token=SELLER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") is not None:
        log("PASS", "6.1 卖家销售统计", "获取统计数据成功", str(resp["data"])[:200], "code=0, data非空")
    else:
        log("FAIL", "6.1 卖家销售统计", "获取统计数据失败", str(resp), "code=0, data非空")

    # ===== Step 7: File Upload Tests =====
    print("\n--- 第七步: 文件上传测试 ---")

    # Create a minimal valid PNG file (1x1 pixel)
    test_file_path = os.path.join(tempfile.gettempdir(), "test_upload.png")
    # Minimal 1x1 white PNG
    import base64
    png_data = base64.b64decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
    )
    with open(test_file_path, "wb") as f:
        f.write(png_data)

    # 7.1 Upload file (biz must be user_avatar or service_image)
    resp = multipart_upload("/file/upload", test_file_path, extra_fields={"biz": "user_avatar"}, token=BUYER_TOKEN)
    file_url = None
    if resp.get("code") == 0 and resp.get("data"):
        file_url = resp["data"]
        log("PASS", "7.1 上传文件", "上传成功", f"文件URL={file_url}", "code=0, 返回文件URL")
    else:
        log("FAIL", "7.1 上传文件", "上传失败", str(resp), "code=0, 返回文件URL")

    # 7.2 View file (public) - use direct HTTP request for binary content (supports remote S3/OSS full URLs and local paths)
    if file_url:
        if file_url.startswith("http://") or file_url.startswith("https://"):
            view_url = file_url
        elif file_url.startswith("/api/"):
            view_url = f"{SERVER_ROOT}{file_url}"
        else:
            view_url = f"{BASE_URL}{file_url}"
        try:
            req = urllib.request.Request(view_url, method="GET")
            resp_obj = urllib.request.urlopen(req, timeout=10)
            view_status = resp_obj.status
            resp_obj.read()  # consume the response
            if 200 <= view_status < 300:
                log("PASS", "7.2 查看文件", "文件可访问", f"HTTP {view_status}", "HTTP 200")
            else:
                log("FAIL", "7.2 查看文件", "文件不可访问", f"HTTP {view_status}", "HTTP 200")
        except urllib.error.HTTPError as e:
            log("FAIL", "7.2 查看文件", "文件不可访问", f"HTTP {e.code}", "HTTP 200")
        except Exception as e:
            log("FAIL", "7.2 查看文件", "请求异常", str(e), "HTTP 200")

    # 7.3 Delete file (param is 'url', not 'filePath')
    if file_url:
        encoded_url = urllib.parse.quote(file_url, safe="")
        resp = api_delete(f"/file/delete?url={encoded_url}", token=BUYER_TOKEN)
        if resp.get("code") == 0:
            log("PASS", "7.3 删除文件", "删除成功", f"code={resp['code']}", "code=0")
        else:
            log("FAIL", "7.3 删除文件", "删除失败", str(resp), "code=0")
    else:
        log("FAIL", "7.2 查看文件", "无文件URL，跳过", "N/A", "N/A")
        log("FAIL", "7.3 删除文件", "无文件URL，跳过", "N/A", "N/A")

    # Cleanup
    if os.path.exists(test_file_path):
        os.remove(test_file_path)

    # ===== Step 8: Service List & Search =====
    print("\n--- 第八步: 服务列表和搜索测试 ---")

    # 8.1 Basic list (requires auth token)
    resp = api_get("/service/list?pageNum=1&pageSize=10", token=BUYER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") is not None:
        data = resp["data"]
        records = data.get("records", data.get("list", []))
        total = data.get("total", len(records) if records else 0)
        log("PASS", "8.1 服务基础列表", "返回分页数据",
            f"记录数={len(records) if records else 0}, total={total}", "code=0, 分页数据")
    else:
        log("FAIL", "8.1 服务基础列表", "获取失败", str(resp), "code=0")

    # 8.2 Keyword search
    resp = api_get("/service/list?keyword=%E8%BE%85%E5%AF%BC", token=BUYER_TOKEN)  # 辅导
    if resp.get("code") == 0 and resp.get("data") is not None:
        data = resp["data"]
        records = data.get("records", data.get("list", []))
        log("PASS", "8.2 关键词搜索", "搜索返回成功",
            f"关键词=辅导, 结果数={len(records) if records else 0}", "code=0")
    else:
        log("FAIL", "8.2 关键词搜索", "搜索失败", str(resp), "code=0")

    # 8.3 Tag filter
    resp = api_get("/service/list?tagId=1", token=BUYER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") is not None:
        data = resp["data"]
        records = data.get("records", data.get("list", []))
        log("PASS", "8.3 标签筛选", "筛选返回成功",
            f"tagId=1, 结果数={len(records) if records else 0}", "code=0")
    else:
        log("FAIL", "8.3 标签筛选", "筛选失败", str(resp), "code=0")

    # 8.4 My services (seller)
    resp = api_get("/service/myServices?pageNum=1&pageSize=10", token=SELLER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") is not None:
        data = resp["data"]
        records = data.get("records", data.get("list", []))
        log("PASS", "8.4 我的服务(卖家)", "返回卖家服务列表",
            f"记录数={len(records) if records else 0}", "code=0")
    else:
        log("FAIL", "8.4 我的服务(卖家)", "获取失败", str(resp), "code=0")

    # ===== Step 9: Order Lists =====
    print("\n--- 第九步: 订单列表测试 ---")

    # 9.1 My orders (buyer)
    resp = api_get("/order/myOrders?pageNum=1&pageSize=10", token=BUYER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") is not None:
        data = resp["data"]
        records = data.get("records", data.get("list", []))
        log("PASS", "9.1 我的订单(买家)", "返回订单列表",
            f"记录数={len(records) if records else 0}", "code=0")
    else:
        log("FAIL", "9.1 我的订单(买家)", "获取失败", str(resp), "code=0")

    # 9.2 My selling orders (seller)
    resp = api_get("/order/mySellingOrders?pageNum=1&pageSize=10", token=SELLER_TOKEN)
    if resp.get("code") == 0 and resp.get("data") is not None:
        data = resp["data"]
        records = data.get("records", data.get("list", []))
        log("PASS", "9.2 我的售出订单(卖家)", "返回售出订单列表",
            f"记录数={len(records) if records else 0}", "code=0")
    else:
        log("FAIL", "9.2 我的售出订单(卖家)", "获取失败", str(resp), "code=0")

    # ===== Step 10: Unauthenticated Access =====
    print("\n--- 第十步: 无认证访问测试 ---")

    # 10.1 Authenticated endpoint without token -> should fail
    resp = api_get("/auth/currentUser")
    if resp.get("code") == 40103 or resp.get("code") == 40100:
        log("PASS", "10.1 无Token访问认证接口", "正确拒绝",
            f"code={resp.get('code')}", "code=40103或40100")
    else:
        log("FAIL", "10.1 无Token访问认证接口", "未正确拒绝",
            f"code={resp.get('code')}, message={resp.get('message')}", "code=40103或40100")

    # 10.2 Public endpoint: service tags
    resp = api_get("/service/tags")
    if resp.get("code") == 0:
        log("PASS", "10.2 无Token访问/service/tags", "公开接口正常访问", f"code={resp['code']}", "code=0")
    else:
        log("FAIL", "10.2 无Token访问/service/tags", "公开接口访问失败", str(resp), "code=0")

    # 10.3 Public endpoint: register status
    resp = api_get("/sys-config/register-status")
    if resp.get("code") == 0:
        log("PASS", "10.3 无Token访问/sys-config/register-status", "公开接口正常访问", f"code={resp['code']}, data={resp.get('data')}", "code=0")
    else:
        log("FAIL", "10.3 无Token访问/sys-config/register-status", "公开接口访问失败", str(resp), "code=0")

    # 10.4 Public endpoint: professional list
    resp = api_get("/professional/list")
    if resp.get("code") == 0:
        data = resp.get("data", [])
        log("PASS", "10.4 无Token访问/professional/list", "公开接口正常访问",
            f"code={resp['code']}, 数量={len(data) if isinstance(data, list) else 'N/A'}", "code=0")
    else:
        log("FAIL", "10.4 无Token访问/professional/list", "公开接口访问失败", str(resp), "code=0")

    # 10.5 Public endpoint: trade location list
    resp = api_get("/trade-location/list")
    if resp.get("code") == 0:
        data = resp.get("data", [])
        log("PASS", "10.5 无Token访问/trade-location/list", "公开接口正常访问",
            f"code={resp['code']}, 数量={len(data) if isinstance(data, list) else 'N/A'}", "code=0")
    else:
        log("FAIL", "10.5 无Token访问/trade-location/list", "公开接口访问失败", str(resp), "code=0")

    # 10.6 Public endpoint: service reviews
    resp = api_get("/review/service/1")
    if resp.get("code") == 0 or resp.get("code") != 40103:
        log("PASS", "10.6 无Token访问/review/service/1", "公开接口不返回认证错误",
            f"code={resp.get('code')}", "code!=40103")
    else:
        log("FAIL", "10.6 无Token访问/review/service/1", "返回认证错误", str(resp), "code!=40103")

    # ===== Summary =====
    print("\n" + "=" * 60)
    print("=== 补充覆盖测试汇总 ===")
    print(f"总测试数: {PASS_COUNT + FAIL_COUNT}")
    print(f"通过: {PASS_COUNT}")
    print(f"失败: {FAIL_COUNT}")
    if FAIL_COUNT > 0:
        print("失败详情:")
        for r in RESULTS:
            if r["status"] == "FAIL":
                print(f"  - {r['step']}: {r['detail']}")
                if r['actual']:
                    print(f"    实际: {r['actual']}")
                if r['expected']:
                    print(f"    预期: {r['expected']}")
    print("=" * 60)

    return FAIL_COUNT

if __name__ == "__main__":
    sys.exit(main())
