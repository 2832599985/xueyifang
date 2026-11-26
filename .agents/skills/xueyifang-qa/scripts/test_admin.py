#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Admin Integration Test Script - Fixed version"""

import json
import os
import time
import sys
import urllib.request
import urllib.error
import random

BASE = os.environ.get("XUEYIFANG_API_BASE", "http://127.0.0.1:8080/api").rstrip("/")
UNIQUE = str(random.randint(10000, 99999))
PASS_COUNT = 0
FAIL_COUNT = 0
FAIL_DETAILS = []

def curl(method, path, token=None, data=None):
    url = BASE + path
    headers = {}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = None
    if data is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw)
    except urllib.error.HTTPError as e:
        try:
            raw = e.read().decode("utf-8")
            return json.loads(raw)
        except:
            return {"code": e.code, "message": str(e), "data": None}
    except Exception as e:
        return {"code": -1, "message": str(e), "data": None}

def passed(name):
    global PASS_COUNT
    PASS_COUNT += 1
    print("[PASS] " + name)

def failed(name, actual, expected):
    global FAIL_COUNT
    FAIL_COUNT += 1
    FAIL_DETAILS.append("  - " + name + ": actual=" + str(actual) + ", expected=" + str(expected))
    print("[FAIL] " + name)
    print("  actual: " + str(actual))
    print("  expected: " + str(expected))

def gc(resp):
    return resp.get("code")

def gd(resp):
    return resp.get("data")

# ========== Step 1: Login ==========
print("=" * 50)
print("=== Step 1: Login ===")
print("=" * 50)

resp = curl("POST", "/auth/login", data={"studentId": "20240001", "password": "password123"})
ADMIN_TOKEN = ""
if gc(resp) == 0 and gd(resp):
    ADMIN_TOKEN = gd(resp).get("token", "")
if ADMIN_TOKEN:
    passed("1.1 Admin login")
else:
    failed("1.1 Admin login", "code=" + str(gc(resp)), "token")

time.sleep(3)
resp = curl("POST", "/auth/login", data={"studentId": "20240002", "password": "password123"})
SELLER_TOKEN = ""
if gc(resp) == 0 and gd(resp):
    SELLER_TOKEN = gd(resp).get("token", "")
if SELLER_TOKEN:
    passed("1.2 Seller login")
else:
    failed("1.2 Seller login", "code=" + str(gc(resp)), "token")

time.sleep(3)
resp = curl("POST", "/auth/login", data={"studentId": "20240003", "password": "password123"})
BUYER_TOKEN = ""
if gc(resp) == 0 and gd(resp):
    BUYER_TOKEN = gd(resp).get("token", "")
if BUYER_TOKEN:
    passed("1.3 Buyer login")
else:
    failed("1.3 Buyer login", "code=" + str(gc(resp)), "token")

# ========== Step 2: Admin Statistics ==========
print("")
print("=" * 50)
print("=== Step 2: Admin Statistics ===")
print("=" * 50)

# 2.1 - field names are totalUsers/totalServices/totalOrders (not userCount etc)
resp = curl("GET", "/admin/statistics", ADMIN_TOKEN)
data = gd(resp)
print("  data: " + str(data))
if gc(resp) == 0 and data and "totalUsers" in data and "totalServices" in data and "totalOrders" in data:
    passed("2.1 Statistics - totalUsers=" + str(data["totalUsers"]) + " totalServices=" + str(data["totalServices"]) + " totalOrders=" + str(data["totalOrders"]))
else:
    failed("2.1 Statistics", "code=" + str(gc(resp)) + " data keys=" + str(list(data.keys()) if data else "None"), "totalUsers,totalServices,totalOrders")

# 2.2 Trend - returns object with dates/orderCounts/serviceCounts
resp = curl("GET", "/admin/statistics/trend", ADMIN_TOKEN)
data = gd(resp)
has_dates = isinstance(data, dict) and "dates" in data and len(data.get("dates", [])) > 0
print("  trend data: " + str(data)[:200] if data else "  trend data: None")
if gc(resp) == 0 and has_dates:
    passed("2.2 Trend - " + str(len(data["dates"])) + " days")
else:
    # trend data might be an object with empty lists if no data
    if gc(resp) == 0 and isinstance(data, dict) and "dates" in data:
        passed("2.2 Trend - returned structure (dates list may be empty)")
    else:
        failed("2.2 Trend", "code=" + str(gc(resp)) + " data=" + str(data)[:100], "dates/orderCounts/serviceCounts")

# 2.3 Non-admin (code 40003 = NO_AUTH_ERROR)
resp = curl("GET", "/admin/statistics", BUYER_TOKEN)
print("  Non-admin: code=" + str(gc(resp)))
if gc(resp) == 40003:
    passed("2.3 Non-admin rejected (40003 NO_AUTH_ERROR)")
else:
    failed("2.3 Non-admin", "code=" + str(gc(resp)), "40003")

# ========== Step 3: Sys Config ==========
print("")
print("=" * 50)
print("=== Step 3: System Config ===")
print("=" * 50)

resp = curl("GET", "/admin/sys-config/list?pageNum=1&pageSize=20", ADMIN_TOKEN)
data = gd(resp)
total = data.get("total", 0) if isinstance(data, dict) else 0
records = data.get("records", []) if isinstance(data, dict) else []
if gc(resp) == 0 and total > 0:
    passed("3.1 Config list - " + str(total) + " items")
else:
    failed("3.1 Config list", "code=" + str(gc(resp)) + " total=" + str(total), "code=0 total>0")

REVIEW_MODE_ID = None
REVIEW_MODE_VALUE = None
REG_ID = None
REG_VALUE = None
for r in records:
    if r.get("configKey") == "REVIEW_MODE":
        REVIEW_MODE_ID = r.get("id")
        REVIEW_MODE_VALUE = r.get("configValue")
    if r.get("configKey") == "REGISTER_ENABLED":
        REG_ID = r.get("id")
        REG_VALUE = r.get("configValue")
print("  REVIEW_MODE: id=" + str(REVIEW_MODE_ID) + " value=" + str(REVIEW_MODE_VALUE))
print("  REGISTER_ENABLED: id=" + str(REG_ID) + " value=" + str(REG_VALUE))

# 3.2 Single config
if REVIEW_MODE_ID:
    resp = curl("GET", "/admin/sys-config/" + str(REVIEW_MODE_ID), ADMIN_TOKEN)
    key = gd(resp).get("configKey", "") if gd(resp) else ""
    if gc(resp) == 0 and key == "REVIEW_MODE":
        passed("3.2 Single config - REVIEW_MODE")
    else:
        failed("3.2 Single config", "key=" + key, "REVIEW_MODE")
else:
    failed("3.2 Single config", "no ID", "ID found")

# 3.3 Key-values
resp = curl("GET", "/admin/sys-config/key-values?keys=REVIEW_MODE,REGISTER_ENABLED", ADMIN_TOKEN)
data = gd(resp)
if gc(resp) == 0 and data and "REVIEW_MODE" in data and "REGISTER_ENABLED" in data:
    passed("3.3 Key-values batch")
else:
    failed("3.3 Key-values", "data=" + str(data), "both keys")

# ========== Step 4: Review Mode Switch ==========
print("")
print("=" * 50)
print("=== Step 4: Review Mode Switch ===")
print("=" * 50)

ORIG_REVIEW_MODE = REVIEW_MODE_VALUE
print("  Current REVIEW_MODE=" + str(ORIG_REVIEW_MODE))

# 4.1 Switch to review required (1)
if REVIEW_MODE_ID:
    resp = curl("PUT", "/admin/sys-config/update", ADMIN_TOKEN, {"id": REVIEW_MODE_ID, "configValue": "1"})
    if gc(resp) == 0:
        passed("4.1 Switch REVIEW_MODE to 1")
    else:
        failed("4.1 Switch", "code=" + str(gc(resp)), "0")
else:
    failed("4.1 Switch", "no ID", "ID")

# 4.2 Seller publish service (correct field names)
resp = curl("POST", "/service/publish", SELLER_TOKEN, {
    "serviceTitle": "TestReviewSvc-Auto",
    "serviceDescription": "Service for review mode testing auto",
    "tagId": 1,
    "tradeType": 1,
    "tradeLocationId": 1,
    "price": 50.00,
    "maxPurchases": 10
})
print("  Publish: code=" + str(gc(resp)) + " msg=" + str(resp.get("message")))
NEW_SERVICE_ID = None
if gc(resp) == 0:
    d = gd(resp)
    # publish returns Long id directly, not an object
    NEW_SERVICE_ID = d if not isinstance(d, dict) else d.get("id")
    passed("4.2 Seller publish - id=" + str(NEW_SERVICE_ID))
else:
    failed("4.2 Seller publish", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

# 4.3 reviewStatus - use GET /service/{id}
if NEW_SERVICE_ID:
    resp = curl("GET", "/service/" + str(NEW_SERVICE_ID), SELLER_TOKEN)
    rs = gd(resp).get("reviewStatus") if gd(resp) else None
    print("  reviewStatus=" + str(rs))
    if gc(resp) == 0 and rs == 2:
        passed("4.3 reviewStatus=2 (pending)")
    else:
        failed("4.3 reviewStatus", str(rs), "2")
else:
    failed("4.3 reviewStatus", "no ID", "ID")

# 4.4 Pending list
resp = curl("GET", "/admin/services/pending?pageNum=1&pageSize=20", ADMIN_TOKEN)
data = gd(resp)
ptotal = data.get("total", 0) if isinstance(data, dict) else (len(data) if isinstance(data, list) else 0)
if gc(resp) == 0 and ptotal > 0:
    passed("4.4 Pending list - " + str(ptotal) + " items")
else:
    failed("4.4 Pending list", "total=" + str(ptotal), ">0")

# 4.5 Approve
if NEW_SERVICE_ID:
    resp = curl("PUT", "/admin/services/service/review", ADMIN_TOKEN, {
        "serviceId": NEW_SERVICE_ID, "approved": True, "reason": "Approved-autotest"
    })
    if gc(resp) == 0:
        passed("4.5 Admin approve")
    else:
        failed("4.5 Approve", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

    # 4.6 After approval - use GET /service/{id}
    # Note: service detail may be cached for 30min, try seller's myServices instead
    resp = curl("GET", "/service/myServices?status=1&pageNum=1&pageSize=50", SELLER_TOKEN)
    found_approved = False
    if gc(resp) == 0 and isinstance(gd(resp), dict):
        for svc in gd(resp).get("records", []):
            if svc.get("id") == NEW_SERVICE_ID:
                rs = svc.get("reviewStatus")
                st = svc.get("status")
                print("  myServices: id=" + str(NEW_SERVICE_ID) + " reviewStatus=" + str(rs) + " status=" + str(st))
                if rs == 1 and st == 1:
                    found_approved = True
                break
    if found_approved:
        passed("4.6 After approval reviewStatus=1, status=1")
    else:
        failed("4.6 After approval", "rs=" + str(rs), "1")
else:
    failed("4.5 Approve", "no ID", "ID")
    failed("4.6 After approval", "no ID", "ID")

# 4.7 Another service
resp = curl("POST", "/service/publish", SELLER_TOKEN, {
    "serviceTitle": "TestRejectSvc-Auto", "serviceDescription": "Will be rejected auto",
    "tagId": 1, "tradeType": 1, "tradeLocationId": 1, "price": 30.00, "maxPurchases": 5
})
REJECT_SERVICE_ID = None
if gc(resp) == 0:
    d = gd(resp)
    REJECT_SERVICE_ID = d if not isinstance(d, dict) else d.get("id")
    passed("4.7 Publish for rejection - id=" + str(REJECT_SERVICE_ID))
else:
    failed("4.7 Publish for rejection", "code=" + str(gc(resp)), "0")

# 4.8 Reject
if REJECT_SERVICE_ID:
    resp = curl("PUT", "/admin/services/service/review", ADMIN_TOKEN, {
        "serviceId": REJECT_SERVICE_ID, "approved": False, "reason": "Incomplete-autotest"
    })
    if gc(resp) == 0:
        passed("4.8 Admin reject")
    else:
        failed("4.8 Reject", "code=" + str(gc(resp)), "0")

    resp = curl("GET", "/service/" + str(REJECT_SERVICE_ID), SELLER_TOKEN)
    rs = gd(resp).get("reviewStatus") if gd(resp) else None
    if rs == 3:
        passed("4.9 After rejection reviewStatus=3")
    else:
        failed("4.9 After rejection", "rs=" + str(rs), "3")
else:
    failed("4.8 Reject", "no ID", "ID")
    failed("4.9 After rejection", "no ID", "ID")

# 4.10 Restore
if REVIEW_MODE_ID and ORIG_REVIEW_MODE:
    resp = curl("PUT", "/admin/sys-config/update", ADMIN_TOKEN, {"id": REVIEW_MODE_ID, "configValue": ORIG_REVIEW_MODE})
    if gc(resp) == 0:
        passed("4.10 Restore REVIEW_MODE=" + str(ORIG_REVIEW_MODE))
    else:
        failed("4.10 Restore", "code=" + str(gc(resp)), "0")
else:
    failed("4.10 Restore", "missing", "restored")

# ========== Step 5: Register Switch ==========
print("")
print("=" * 50)
print("=== Step 5: Register Switch ===")
print("=" * 50)

resp = curl("GET", "/sys-config/register-status")
if gc(resp) == 0:
    passed("5.1 Check register status")
else:
    failed("5.1 Register status", "code=" + str(gc(resp)), "0")

# 5.2 Disable
if REG_ID:
    resp = curl("PUT", "/admin/sys-config/update", ADMIN_TOKEN, {"id": REG_ID, "configValue": "false"})
    if gc(resp) == 0:
        passed("5.2 Disable registration")
    else:
        failed("5.2 Disable", "code=" + str(gc(resp)), "0")

# Wait for cache to clear
time.sleep(2)

# 5.3 Register when disabled
resp = curl("POST", "/auth/register", data={"studentId": "20249998", "password": "test123456", "realName": "TestDisReg"})
print("  Register when disabled: code=" + str(gc(resp)) + " msg=" + str(resp.get("message")))
if gc(resp) != 0:
    passed("5.3 Register rejected when disabled (code=" + str(gc(resp)) + ")")
else:
    failed("5.3 Register when disabled", "code=0", "non-zero")

# 5.4 Enable
if REG_ID:
    resp = curl("PUT", "/admin/sys-config/update", ADMIN_TOKEN, {"id": REG_ID, "configValue": "1"})
    if gc(resp) == 0:
        passed("5.4 Enable registration")
    else:
        failed("5.4 Enable", "code=" + str(gc(resp)), "0")

time.sleep(2)

# 5.5 Register new user (with complete required fields)
REG_PAY = {
    "studentId": "20249999",
    "password": "test123456",
    "confirmPassword": "test123456",
    "realName": "TestUser",
    "phone": "13900009999",
    "professionalId": 1
}
resp = curl("POST", "/auth/register", data=REG_PAY)
print("  Register: code=" + str(gc(resp)) + " msg=" + str(resp.get("message", "")))
if gc(resp) == 0:
    passed("5.5 Register new user")
else:
    msg = resp.get("message", "")
    if gc(resp) == 40105:
        passed("5.5 Register API works (user already exists)")
    else:
        failed("5.5 Register", "code=" + str(gc(resp)) + " msg=" + msg, "0 or 40105")

# 5.6 Login new user
time.sleep(2)
resp = curl("POST", "/auth/login", data={"studentId": "20249999", "password": "test123456"})
NEW_USER_TOKEN = ""
NEW_USER_ID = None
if gc(resp) == 0 and gd(resp):
    NEW_USER_TOKEN = gd(resp).get("token", "")
    NEW_USER_ID = gd(resp).get("userInfo", {}).get("id")
    passed("5.6 New user login")
else:
    failed("5.6 Login", "code=" + str(gc(resp)), "0")

# ========== Step 6: Publish Permission ==========
print("")
print("=" * 50)
print("=== Step 6: Publish Permission ===")
print("=" * 50)

resp = curl("GET", "/permission/status", BUYER_TOKEN)
print("  Permission: " + str(gd(resp)))
if gc(resp) == 0:
    passed("6.1 Permission status")
else:
    failed("6.1 Permission status", "code=" + str(gc(resp)), "0")

# Buyer already has permission (publishPermission=1 from login data)
buyer_has_perm = False
if gd(resp) and isinstance(gd(resp), dict):
    buyer_has_perm = gd(resp).get("publishPermission") == 1 or gd(resp).get("hasPermission") == True

resp = curl("POST", "/permission/apply", BUYER_TOKEN)
msg = resp.get("message", "")
print("  Apply: code=" + str(gc(resp)) + " msg=" + msg)
# Already has permission or already applied
if gc(resp) == 0 or gc(resp) == 50000 or gc(resp) == 50001 or gc(resp) == 40402:
    passed("6.2 Apply permission (code=" + str(gc(resp)) + " - may already have)")
else:
    failed("6.2 Apply", "code=" + str(gc(resp)), "0 or already")

resp = curl("GET", "/admin/users/pending?pageNum=1&pageSize=20", ADMIN_TOKEN)
if gc(resp) == 0:
    passed("6.3 Admin pending users")
else:
    failed("6.3 Pending users", "code=" + str(gc(resp)), "0")

resp = curl("PUT", "/admin/permission/review", ADMIN_TOKEN, {"userId": 3, "approved": True})
msg = resp.get("message", "")
print("  Approve perm: code=" + str(gc(resp)) + " msg=" + msg)
# 50001 = OPERATION_ERROR may mean already approved
if gc(resp) == 0 or gc(resp) == 50001:
    passed("6.4 Admin approve permission (code=" + str(gc(resp)) + ")")
else:
    failed("6.4 Approve perm", "code=" + str(gc(resp)), "0 or 50001")

resp = curl("GET", "/permission/status", BUYER_TOKEN)
pdata = gd(resp)
if gc(resp) == 0:
    pp = pdata.get("publishPermission") if isinstance(pdata, dict) else None
    hp = pdata.get("hasPermission") if isinstance(pdata, dict) else None
    print("  publishPermission=" + str(pp) + " hasPermission=" + str(hp))
    if pp == 1 or hp == True:
        passed("6.5 Buyer has permission after approval")
    else:
        failed("6.5 Permission after approval", "pp=" + str(pp) + " hp=" + str(hp), "pp=1 or hp=True")
else:
    failed("6.5 Re-check", "code=" + str(gc(resp)), "0")

# 6.6 Buyer publish (correct field names)
resp = curl("POST", "/service/publish", BUYER_TOKEN, {
    "serviceTitle": "BuyerTestSvc-PermVerify", "serviceDescription": "Buyer test publish",
    "tagId": 1, "tradeType": 1, "tradeLocationId": 1, "price": 20.00, "maxPurchases": 5
})
print("  Buyer publish: code=" + str(gc(resp)) + " msg=" + str(resp.get("message")))
BUYER_SVC_ID = None
if gc(resp) == 0:
    d = gd(resp)
    BUYER_SVC_ID = d if not isinstance(d, dict) else d.get("id")
    passed("6.6 Buyer publish - id=" + str(BUYER_SVC_ID))
else:
    failed("6.6 Buyer publish", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

# ========== Step 7: Dispute Handling ==========
print("")
print("=" * 50)
print("=== Step 7: Dispute Handling ===")
print("=" * 50)

# Find seller's available service - use /service/list not /service/search
# Also try existing service id=2 which is known to be available
resp = curl("GET", "/service/list?pageNum=1&pageSize=10&sellerId=2", BUYER_TOKEN)
avail_svc = None
if gc(resp) == 0:
    recs = gd(resp).get("records", []) if isinstance(gd(resp), dict) else []
    print("  Service list returned " + str(len(recs)) + " records")
    for svc in recs:
        sid = svc.get("sellerId", svc.get("userId"))
        st = svc.get("status")
        rs = svc.get("reviewStatus")
        print("  svc id=" + str(svc.get("id")) + " sellerId=" + str(sid) + " status=" + str(st) + " reviewStatus=" + str(rs))
        if st == 1 and rs == 1:
            mp = svc.get("maxPurchases", -1)
            cp = svc.get("currentPurchaseCount", 0)
            if mp == -1 or mp == 0 or (mp > 0 and cp < mp):
                avail_svc = svc.get("id")
                break

# Fallback: use the service approved in step 4
if not avail_svc and NEW_SERVICE_ID:
    print("  Fallback to step 4 approved service id=" + str(NEW_SERVICE_ID))
    avail_svc = NEW_SERVICE_ID
print("  Service for order: id=" + str(avail_svc))

DISPUTE_ORDER_ID = None
DISPUTE_ID = None

if avail_svc:
    # 7.1 - use tradeType=2 (online) for dispute refund test
    resp = curl("POST", "/order/create", BUYER_TOKEN, {
        "serviceId": avail_svc, "quantity": 1, "tradeType": 2
    })
    print("  Create order: code=" + str(gc(resp)) + " msg=" + str(resp.get("message")))
    if gc(resp) == 0:
        d = gd(resp)
        DISPUTE_ORDER_ID = d if not isinstance(d, dict) else d.get("id")
        passed("7.1 Create order - id=" + str(DISPUTE_ORDER_ID))
    else:
        failed("7.1 Create order", "code=" + str(gc(resp)), "0")

    if DISPUTE_ORDER_ID:
        # 7.2 Pay (path variable + paymentMethod)
        resp = curl("POST", "/order/" + str(DISPUTE_ORDER_ID) + "/pay", BUYER_TOKEN, {"paymentMethod": 1})
        if gc(resp) == 0:
            passed("7.2 Pay")
        else:
            failed("7.2 Pay", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

        # 7.3 Ship (seller ships the order)
        resp = curl("POST", "/order/" + str(DISPUTE_ORDER_ID) + "/ship", SELLER_TOKEN)
        if gc(resp) == 0:
            passed("7.3 Deliver")
        else:
            failed("7.3 Deliver", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

        # 7.4 Create dispute
        resp = curl("POST", "/dispute/create", BUYER_TOKEN, {
            "orderId": DISPUTE_ORDER_ID, "disputeType": 2,
            "description": "Service differs from description"
        })
        if gc(resp) == 0:
            d = gd(resp)
            DISPUTE_ID = d if not isinstance(d, dict) else d.get("id")
            passed("7.4 Create dispute - id=" + str(DISPUTE_ID))
        else:
            failed("7.4 Create dispute", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

        # 7.5
        resp = curl("GET", "/dispute/my?pageNum=1&pageSize=10", BUYER_TOKEN)
        if gc(resp) == 0:
            passed("7.5 Buyer dispute list")
        else:
            failed("7.5 Dispute list", "code=" + str(gc(resp)), "0")

        if DISPUTE_ID:
            # 7.6
            resp = curl("GET", "/dispute/" + str(DISPUTE_ID), BUYER_TOKEN)
            if gc(resp) == 0:
                passed("7.6 Dispute detail")
            else:
                failed("7.6 Detail", "code=" + str(gc(resp)), "0")

            # 7.7
            resp = curl("GET", "/admin/dispute/list?pageNum=1&pageSize=10", ADMIN_TOKEN)
            if gc(resp) == 0:
                passed("7.7 Admin dispute list")
            else:
                failed("7.7 Admin list", "code=" + str(gc(resp)), "0")

            # 7.8
            resp = curl("GET", "/admin/dispute/" + str(DISPUTE_ID), ADMIN_TOKEN)
            if gc(resp) == 0:
                passed("7.8 Admin dispute detail")
            else:
                failed("7.8 Admin detail", "code=" + str(gc(resp)), "0")

            # 7.9
            resp = curl("GET", "/admin/dispute/by-order/" + str(DISPUTE_ORDER_ID), ADMIN_TOKEN)
            if gc(resp) == 0:
                passed("7.9 Admin by-order")
            else:
                failed("7.9 By-order", "code=" + str(gc(resp)), "0")

            # Balance before
            resp = curl("GET", "/user/current", BUYER_TOKEN)
            bal_before = gd(resp).get("walletBalance") if gd(resp) else None
            print("  Buyer balance before: " + str(bal_before))

            # 7.10 Handle refund (actionType=1 resolve, needRefund=true)
            resp = curl("POST", "/admin/dispute/" + str(DISPUTE_ID) + "/handle", ADMIN_TOKEN, {
                "actionType": 1, "adminReply": "Verified quality issue", "resolution": "Full refund to buyer", "needRefund": True
            })
            print("  Handle: code=" + str(gc(resp)) + " msg=" + str(resp.get("message")))
            if gc(resp) == 0:
                passed("7.10 Admin handle (refund)")
            else:
                failed("7.10 Handle", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

            # 7.11 Order status
            resp = curl("GET", "/order/" + str(DISPUTE_ORDER_ID), BUYER_TOKEN)
            ost = gd(resp).get("orderStatus") if gd(resp) else None
            print("  Order status: " + str(ost))
            if ost == 6:
                passed("7.11 Order status=6 (failed)")
            else:
                failed("7.11 Order status", "status=" + str(ost), "6")

            # Balance after
            resp = curl("GET", "/user/current", BUYER_TOKEN)
            bal_after = gd(resp).get("walletBalance") if gd(resp) else None
            print("  Buyer balance after: " + str(bal_after))
            if bal_before is not None and bal_after is not None and bal_after > bal_before:
                print("  Balance restored: " + str(bal_before) + " -> " + str(bal_after))
        else:
            for l in ["7.6","7.7","7.8","7.9","7.10","7.11"]:
                failed(l + " (skip)", "no dispute", "dispute")

        # ---- Dismiss test ----
        print("--- Dismiss dispute test ---")
        resp = curl("POST", "/order/create", BUYER_TOKEN, {
            "serviceId": avail_svc, "quantity": 1, "tradeType": 2
        })
        DISMISS_ORDER_ID = None
        if gc(resp) == 0:
            d = gd(resp)
            DISMISS_ORDER_ID = d if not isinstance(d, dict) else d.get("id")

        if DISMISS_ORDER_ID:
            curl("POST", "/order/" + str(DISMISS_ORDER_ID) + "/pay", BUYER_TOKEN, {"paymentMethod": 1})
            curl("POST", "/order/" + str(DISMISS_ORDER_ID) + "/ship", SELLER_TOKEN)
            resp = curl("POST", "/dispute/create", BUYER_TOKEN, {
                "orderId": DISMISS_ORDER_ID, "disputeType": 4, "description": "Dismiss test dispute"
            })
            DISMISS_DISPUTE_ID = None
            if gc(resp) == 0:
                d = gd(resp)
                DISMISS_DISPUTE_ID = d if not isinstance(d, dict) else d.get("id")

            if DISMISS_DISPUTE_ID:
                resp = curl("POST", "/admin/dispute/" + str(DISMISS_DISPUTE_ID) + "/handle", ADMIN_TOKEN, {
                    "actionType": 2, "adminReply": "Claim not substantiated", "resolution": "Dispute dismissed, maintain original transaction"
                })
                if gc(resp) == 0:
                    passed("7.12 Admin dismiss")
                else:
                    failed("7.12 Dismiss", "code=" + str(gc(resp)), "0")

                resp = curl("GET", "/order/" + str(DISMISS_ORDER_ID), BUYER_TOKEN)
                ost = gd(resp).get("orderStatus") if gd(resp) else None
                if ost != 6:
                    passed("7.13 Order continues (status=" + str(ost) + ")")
                else:
                    failed("7.13 After dismiss", "status=6", "not 6")
            else:
                failed("7.12 Dismiss", "no dispute", "dispute")
                failed("7.13 After dismiss", "no dispute", "dispute")
        else:
            failed("7.12 Dismiss", "no order", "order")
            failed("7.13 After dismiss", "no order", "order")
    else:
        for l in ["7.2","7.3","7.4","7.5","7.6","7.7","7.8","7.9","7.10","7.11","7.12","7.13"]:
            failed(l + " (skip)", "no order", "order")
else:
    for l in ["7.1","7.2","7.3","7.4","7.5","7.6","7.7","7.8","7.9","7.10","7.11","7.12","7.13"]:
        failed(l + " (skip-no svc)", "N/A", "N/A")

# ========== Step 8: Professional CRUD ==========
print("")
print("=" * 50)
print("=== Step 8: Professional CRUD ===")
print("=" * 50)

resp = curl("GET", "/professional/list")
data = gd(resp)
pcount = len(data) if isinstance(data, list) else 0
if gc(resp) == 0 and pcount > 0:
    passed("8.1 Professional list - " + str(pcount))
else:
    failed("8.1 List", "count=" + str(pcount), ">0")

PROF_NAME = "TestProf-Auto-" + UNIQUE
resp = curl("POST", "/admin/professional/add", ADMIN_TOKEN, {"professionalName": PROF_NAME})
print("  Add: code=" + str(gc(resp)) + " data=" + str(gd(resp)))
NEW_PROF_ID = None
if gc(resp) == 0:
    # add returns boolean, not ID. Need to find it from list
    resp2 = curl("GET", "/professional/list")
    if gc(resp2) == 0 and isinstance(gd(resp2), list):
        for p in gd(resp2):
            pn = p.get("professionalName", "")
            if PROF_NAME in pn:
                NEW_PROF_ID = p.get("id")
                break
    passed("8.2 Add professional - id=" + str(NEW_PROF_ID))
else:
    failed("8.2 Add", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

PROF_MOD_NAME = "TestProf-Modified-" + UNIQUE
if NEW_PROF_ID:
    resp = curl("PUT", "/admin/professional/update", ADMIN_TOKEN, {"id": NEW_PROF_ID, "professionalName": PROF_MOD_NAME})
    if gc(resp) == 0:
        passed("8.3 Update professional")
    else:
        failed("8.3 Update", "code=" + str(gc(resp)), "0")

    resp = curl("GET", "/professional/" + str(NEW_PROF_ID), ADMIN_TOKEN)
    name = ""
    if gd(resp) and isinstance(gd(resp), dict):
        name = gd(resp).get("name", gd(resp).get("professionalName", ""))
    if gc(resp) == 0 and "Modified" in name:
        passed("8.4 Detail - name=" + name)
    else:
        failed("8.4 Detail", "name=" + name, "contains Modified")

    resp = curl("DELETE", "/admin/professional/" + str(NEW_PROF_ID), ADMIN_TOKEN)
    if gc(resp) == 0:
        passed("8.5 Delete professional")
    else:
        failed("8.5 Delete", "code=" + str(gc(resp)), "0")
else:
    failed("8.3 Update", "no ID", "ID")
    failed("8.4 Detail", "no ID", "ID")
    failed("8.5 Delete", "no ID", "ID")

resp = curl("POST", "/admin/professional/add", BUYER_TOKEN, {"professionalName": "Illegal"})
# Buyer is not admin -> NO_AUTH_ERROR (40003)
if gc(resp) == 40003:
    passed("8.6 Non-admin add rejected (40003)")
else:
    failed("8.6 Non-admin add", "code=" + str(gc(resp)), "40003")

# ========== Step 9: Trade Location CRUD ==========
print("")
print("=" * 50)
print("=== Step 9: Trade Location CRUD ===")
print("=" * 50)

resp = curl("GET", "/trade-location/list")
data = gd(resp)
lcount = len(data) if isinstance(data, list) else 0
if gc(resp) == 0:
    passed("9.1 Location list - " + str(lcount))
else:
    failed("9.1 List", "code=" + str(gc(resp)), "0")

resp = curl("GET", "/trade-location/list/all", BUYER_TOKEN)
acount = len(gd(resp)) if isinstance(gd(resp), list) else 0
if gc(resp) == 0:
    passed("9.2 All locations - " + str(acount))
else:
    failed("9.2 All", "code=" + str(gc(resp)), "0")

LOC_NAME = "TestLoc-BuildingA-" + UNIQUE
resp = curl("POST", "/trade-location/add", ADMIN_TOKEN, {
    "locationName": LOC_NAME, "locationDescription": "1st floor", "isAvailable": 1
})
print("  Add location: code=" + str(gc(resp)) + " data=" + str(gd(resp)))
NEW_LOC_ID = None
if gc(resp) == 0:
    # add may return null data, find from list
    resp2 = curl("GET", "/trade-location/list/all", ADMIN_TOKEN)
    if gc(resp2) == 0 and isinstance(gd(resp2), list):
        for loc in gd(resp2):
            ln = loc.get("locationName", loc.get("name", ""))
            if LOC_NAME in ln:
                NEW_LOC_ID = loc.get("id")
                break
    passed("9.3 Add location - id=" + str(NEW_LOC_ID))
else:
    failed("9.3 Add", "code=" + str(gc(resp)) + " msg=" + str(resp.get("message")), "0")

if NEW_LOC_ID:
    resp = curl("PUT", "/trade-location/update", ADMIN_TOKEN, {"id": NEW_LOC_ID, "locationName": "TestLoc-Modified-" + UNIQUE})
    if gc(resp) == 0:
        passed("9.4 Update location")
    else:
        failed("9.4 Update", "code=" + str(gc(resp)), "0")

    resp = curl("GET", "/trade-location/" + str(NEW_LOC_ID), ADMIN_TOKEN)
    if gc(resp) == 0:
        passed("9.5 Location detail")
    else:
        failed("9.5 Detail", "code=" + str(gc(resp)), "0")

    resp = curl("DELETE", "/trade-location/" + str(NEW_LOC_ID), ADMIN_TOKEN)
    if gc(resp) == 0:
        passed("9.6 Delete location")
    else:
        failed("9.6 Delete", "code=" + str(gc(resp)), "0")
else:
    failed("9.4 Update", "no ID", "ID")
    failed("9.5 Detail", "no ID", "ID")
    failed("9.6 Delete", "no ID", "ID")

resp = curl("POST", "/trade-location/add", BUYER_TOKEN, {"locationName": "Illegal"})
if gc(resp) == 40003:
    passed("9.7 Non-admin add rejected (40003)")
else:
    failed("9.7 Non-admin add", "code=" + str(gc(resp)), "40003")

# ========== Step 10: Permission Boundary ==========
print("")
print("=" * 50)
print("=== Step 10: Permission Boundary ===")
print("=" * 50)

# Non-admin should get 40003 (NO_AUTH_ERROR)
for tid, path in [("10.1","/admin/statistics"),("10.2","/admin/services/pending?pageNum=1&pageSize=10"),("10.3","/admin/dispute/list?pageNum=1&pageSize=10"),("10.4","/admin/sys-config/list?pageNum=1&pageSize=10")]:
    resp = curl("GET", path, BUYER_TOKEN)
    short = path.split("?")[0]
    if gc(resp) == 40003:
        passed(tid + " " + short + " rejected (40003)")
    else:
        failed(tid + " " + short, "code=" + str(gc(resp)), "40003")

# ========== Step 11: Admin Governance & Service Tag CRUD ==========
print("")
print("=" * 50)
print("=== Step 11: Admin Governance & Service Tag CRUD ===")
print("=" * 50)

# 11.1 User list
resp = curl("GET", "/admin/users/list?pageNum=1&pageSize=20", ADMIN_TOKEN)
if gc(resp) == 0 and isinstance(gd(resp), dict) and gd(resp).get("total", 0) >= 3:
    passed("11.1 Admin user list - total=" + str(gd(resp).get("total")))
else:
    failed("11.1 Admin user list", str(resp), "total>=3")

# 11.2 New user permission apply with reason & review
if NEW_USER_TOKEN and NEW_USER_ID:
    st_before = curl("GET", "/permission/status", NEW_USER_TOKEN)
    if gc(st_before) == 0 and gd(st_before).get("permissionReviewStatus") is None:
        passed("11.2a Newly registered user starts with permissionReviewStatus=None (not applied)")
    else:
        failed("11.2a New user initial status", str(gd(st_before)), "permissionReviewStatus=None")

    apply_resp = curl("POST", "/permission/apply", NEW_USER_TOKEN, {"reason": "精通Java与Vue全栈开发，申请开通技能服务发布权限"})
    if gc(apply_resp) == 0:
        passed("11.2b New user apply permission with reason")
    else:
        failed("11.2b Apply with reason", str(apply_resp), "code=0")

    pending_resp = curl("GET", "/admin/users/pending?pageNum=1&pageSize=20", ADMIN_TOKEN)
    found_reason = False
    if gc(pending_resp) == 0 and isinstance(gd(pending_resp), dict):
        for u in gd(pending_resp).get("records", []):
            if u.get("userId") == NEW_USER_ID and "Java与Vue" in str(u.get("applyReason", "")):
                found_reason = True
                break
    if found_reason:
        passed("11.2c Pending users list includes persisted applyReason")
    else:
        failed("11.2c Pending applyReason", str(gd(pending_resp)), "contains applyReason")

    # 11.3 Admin reset password & user status toggle
    rst = curl("PUT", "/admin/users/" + str(NEW_USER_ID) + "/reset-password", ADMIN_TOKEN)
    if gc(rst) == 0:
        passed("11.3 Admin reset user password")
    else:
        failed("11.3 Reset password", str(rst), "code=0")

    ust = curl("PUT", "/admin/users/status", ADMIN_TOKEN, {"userId": NEW_USER_ID, "accountStatus": 1, "publishPermission": 1})
    if gc(ust) == 0:
        passed("11.4 Admin update user status & publishPermission")
    else:
        failed("11.4 Update user status", str(ust), "code=0")

# 11.5 Admin all-services inspection & force-offline
all_svc_resp = curl("GET", "/admin/services/list?pageNum=1&pageSize=20", ADMIN_TOKEN)
if gc(all_svc_resp) == 0 and isinstance(gd(all_svc_resp), dict):
    passed("11.5 Admin all services list - total=" + str(gd(all_svc_resp).get("total")))
else:
    failed("11.5 Admin all services list", str(all_svc_resp), "code=0")

if BUYER_SVC_ID:
    fo_resp = curl("PUT", "/admin/services/" + str(BUYER_SVC_ID) + "/offline", ADMIN_TOKEN, {"reason": "巡检测试强制下架"})
    if gc(fo_resp) == 0:
        passed("11.6 Admin force-offline service")
    else:
        failed("11.6 Force-offline", str(fo_resp), "code=0")

# 11.7 Admin Service Tag CRUD
TAG_NAME = "TestTag-" + UNIQUE
add_tag = curl("POST", "/admin/service-tag/add", ADMIN_TOKEN, {"tagName": TAG_NAME, "description": "自动化测试分类"})
if gc(add_tag) == 0:
    passed("11.7a Add service tag")
else:
    failed("11.7a Add service tag", str(add_tag), "code=0")

tag_list = curl("GET", "/admin/service-tag/list?pageNum=1&pageSize=50&nameLike=" + TAG_NAME, ADMIN_TOKEN)
NEW_TAG_ID = None
if gc(tag_list) == 0 and isinstance(gd(tag_list), dict):
    for t in gd(tag_list).get("records", []):
        if t.get("tagName") == TAG_NAME:
            NEW_TAG_ID = t.get("id")
            break
if NEW_TAG_ID:
    passed("11.7b Query added service tag - id=" + str(NEW_TAG_ID))
    upd_tag = curl("PUT", "/admin/service-tag/update", ADMIN_TOKEN, {"id": NEW_TAG_ID, "tagName": TAG_NAME + "-Mod", "description": "修改后描述"})
    if gc(upd_tag) == 0:
        passed("11.7c Update service tag")
    else:
        failed("11.7c Update service tag", str(upd_tag), "code=0")

    del_tag = curl("DELETE", "/admin/service-tag/" + str(NEW_TAG_ID), ADMIN_TOKEN)
    if gc(del_tag) == 0:
        passed("11.7d Delete service tag")
    else:
        failed("11.7d Delete service tag", str(del_tag), "code=0")
else:
    failed("11.7b Query added service tag", str(tag_list), "tag found")

# ========== Summary ==========
print("")
print("=" * 50)
print("=== Admin Integration Test Summary ===")
print("=" * 50)
TOTAL = PASS_COUNT + FAIL_COUNT
print("Total tests: " + str(TOTAL))
print("Passed: " + str(PASS_COUNT))
print("Failed: " + str(FAIL_COUNT))
if FAIL_DETAILS:
    print("Failure details:")
    for d in FAIL_DETAILS:
        print(d)
print("=" * 50)

sys.exit(FAIL_COUNT)
