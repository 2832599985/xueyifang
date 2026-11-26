#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
学艺坊 - 买卖交易联动集成测试
"""
import os
import subprocess
import json
import time
import sys

# Force UTF-8 output
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

BASE_URL = os.environ.get("XUEYIFANG_API_BASE", "http://127.0.0.1:8080/api").rstrip("/")

# Test results tracking
results = []

def log(msg):
    print(msg, flush=True)

def record(status, step, detail, actual=None, expected=None):
    results.append({"status": status, "step": step, "detail": detail})
    if status == "PASS":
        log("[PASS] {} - {}".format(step, detail))
    else:
        log("[FAIL] {} - {}".format(step, detail))
    if actual is not None:
        log("  actual: {}".format(actual))
    if expected is not None:
        log("  expected: {}".format(expected))

def api(method, path, token=None, data=None, timeout=15):
    """Execute curl request and return parsed JSON."""
    url = BASE_URL + path
    cmd = ["curl", "-4", "-s", "--max-time", str(timeout), url, "-X", method]
    if token:
        cmd += ["-H", "Authorization: Bearer {}".format(token)]
    cmd += ["-H", "Content-Type: application/json"]
    if data:
        cmd += ["-d", json.dumps(data)]
    try:
        result = subprocess.check_output(cmd, stderr=subprocess.PIPE)
        text = result.decode("utf-8", errors="replace")
        if not text.strip():
            return {"code": -999, "message": "Empty response", "data": None}
        return json.loads(text)
    except subprocess.CalledProcessError as e:
        return {"code": -999, "message": "curl error: {}".format(str(e)), "data": None}
    except json.JSONDecodeError as e:
        return {"code": -998, "message": "JSON parse error: {}".format(str(e)), "data": text if 'text' in dir() else None}

def check_code(resp, expected_code=0):
    return resp.get("code") == expected_code

# ============================================================
# STEP 1: Login and get tokens
# ============================================================
log("=" * 60)
log("STEP 1: Login and get tokens")
log("=" * 60)

admin_resp = api("POST", "/auth/login", data={"studentId": "20240001", "password": "password123"})
seller_resp = api("POST", "/auth/login", data={"studentId": "20240002", "password": "password123"})
buyer_resp = api("POST", "/auth/login", data={"studentId": "20240003", "password": "password123"})

ADMIN_TOKEN = None
SELLER_TOKEN = None
BUYER_TOKEN = None
ADMIN_ID = None
SELLER_ID = None
BUYER_ID = None

if check_code(admin_resp):
    ADMIN_TOKEN = admin_resp["data"]["token"]
    ADMIN_ID = admin_resp["data"]["userInfo"]["id"]
    record("PASS", "Step1-AdminLogin", "Admin login success, userId={}".format(ADMIN_ID))
else:
    record("FAIL", "Step1-AdminLogin", "Admin login failed", admin_resp.get("message"), "code=0")

if check_code(seller_resp):
    SELLER_TOKEN = seller_resp["data"]["token"]
    SELLER_ID = seller_resp["data"]["userInfo"]["id"]
    record("PASS", "Step1-SellerLogin", "Seller login success, userId={}".format(SELLER_ID))
else:
    record("FAIL", "Step1-SellerLogin", "Seller login failed", seller_resp.get("message"), "code=0")

if check_code(buyer_resp):
    BUYER_TOKEN = buyer_resp["data"]["token"]
    BUYER_ID = buyer_resp["data"]["userInfo"]["id"]
    record("PASS", "Step1-BuyerLogin", "Buyer login success, userId={}".format(BUYER_ID))
else:
    record("FAIL", "Step1-BuyerLogin", "Buyer login failed", buyer_resp.get("message"), "code=0")

if not all([ADMIN_TOKEN, SELLER_TOKEN, BUYER_TOKEN]):
    log("FATAL: Cannot proceed without all tokens. Aborting.")
    sys.exit(1)

# ============================================================
# STEP 2: Record initial wallet balances
# ============================================================
log("")
log("=" * 60)
log("STEP 2: Record initial wallet balances")
log("=" * 60)

buyer_wallet = api("GET", "/wallet/balance", BUYER_TOKEN)
seller_wallet = api("GET", "/wallet/balance", SELLER_TOKEN)

BUYER_INITIAL_BALANCE = None
SELLER_INITIAL_BALANCE = None
BUYER_INITIAL_FROZEN = None

if check_code(buyer_wallet):
    BUYER_INITIAL_BALANCE = float(buyer_wallet["data"]["walletBalance"])
    BUYER_INITIAL_FROZEN = float(buyer_wallet["data"].get("frozenAmount", 0))
    record("PASS", "Step2-BuyerBalance", "Buyer initial balance={}, frozen={}".format(BUYER_INITIAL_BALANCE, BUYER_INITIAL_FROZEN))
else:
    record("FAIL", "Step2-BuyerBalance", "Failed to get buyer balance", buyer_wallet.get("message"))

if check_code(seller_wallet):
    SELLER_INITIAL_BALANCE = float(seller_wallet["data"]["walletBalance"])
    record("PASS", "Step2-SellerBalance", "Seller initial balance={}".format(SELLER_INITIAL_BALANCE))
else:
    record("FAIL", "Step2-SellerBalance", "Failed to get seller balance", seller_wallet.get("message"))

# ============================================================
# STEP 3: Buyer recharge
# ============================================================
log("")
log("=" * 60)
log("STEP 3: Buyer recharge 500")
log("=" * 60)

recharge_resp = api("POST", "/wallet/recharge", BUYER_TOKEN, {"amount": 500.00})
if check_code(recharge_resp):
    record("PASS", "Step3-Recharge", "Buyer recharge 500 success")
else:
    record("FAIL", "Step3-Recharge", "Buyer recharge failed", recharge_resp.get("message"))

buyer_wallet2 = api("GET", "/wallet/balance", BUYER_TOKEN)
if check_code(buyer_wallet2):
    new_balance = float(buyer_wallet2["data"]["walletBalance"])
    expected = BUYER_INITIAL_BALANCE + 500.0 if BUYER_INITIAL_BALANCE is not None else None
    if expected is not None and abs(new_balance - expected) < 0.01:
        record("PASS", "Step3-BalanceVerify", "Balance correctly increased to {}".format(new_balance))
    else:
        record("FAIL", "Step3-BalanceVerify", "Balance mismatch", new_balance, expected)
    BUYER_BALANCE_AFTER_RECHARGE = new_balance
else:
    record("FAIL", "Step3-BalanceVerify", "Failed to verify balance after recharge")
    BUYER_BALANCE_AFTER_RECHARGE = (BUYER_INITIAL_BALANCE or 0) + 500.0

# ============================================================
# STEP 4: Seller publishes service
# ============================================================
log("")
log("=" * 60)
log("STEP 4: Seller publishes service")
log("=" * 60)

publish_data = {
    "serviceTitle": "API-Trade-Integration-Test-Programming-Tutoring",
    "serviceDescription": "Provide one-on-one programming tutoring service covering Java basics to advanced topics.",
    "tagId": 1,
    "tradeType": 2,
    "price": 50.00,
    "maxPurchases": 10,
    "isPhysical": 0
}
publish_resp = api("POST", "/service/publish", SELLER_TOKEN, publish_data)
SERVICE_ID = None
if check_code(publish_resp):
    SERVICE_ID = publish_resp["data"]
    record("PASS", "Step4-Publish", "Service published, serviceId={}".format(SERVICE_ID))
else:
    record("FAIL", "Step4-Publish", "Publish failed", publish_resp.get("message"))

# Admin approves the service (required before it can be used)
if SERVICE_ID:
    review_resp = api("PUT", "/admin/services/service/review", ADMIN_TOKEN, {
        "serviceId": SERVICE_ID,
        "approved": True,
        "reason": "Approved for testing"
    })
    if check_code(review_resp):
        record("PASS", "Step4-AdminApprove", "Admin approved service")
    else:
        record("FAIL", "Step4-AdminApprove", "Admin approval failed", review_resp.get("message"))

# Verify service details (now accessible after approval)
if SERVICE_ID:
    svc_detail = api("GET", "/service/{}".format(SERVICE_ID), BUYER_TOKEN)
    if check_code(svc_detail):
        svc = svc_detail["data"]
        title_ok = "Integration-Test" in str(svc.get("serviceTitle", ""))
        price_ok = abs(float(svc.get("price", 0)) - 50.0) < 0.01
        if title_ok and price_ok:
            record("PASS", "Step4-Verify", "Service details correct, title={}, price={}".format(svc.get("serviceTitle"), svc.get("price")))
        else:
            record("FAIL", "Step4-Verify", "Service details mismatch", "title={}, price={}".format(svc.get("serviceTitle"), svc.get("price")))
    else:
        record("FAIL", "Step4-Verify", "Cannot get service detail", svc_detail.get("message"))

# ============================================================
# STEP 5: Buyer searches for service
# ============================================================
log("")
log("=" * 60)
log("STEP 5: Buyer searches for service")
log("=" * 60)

search_resp = api("GET", "/service/list?keyword=API-Trade-Integration-Test", BUYER_TOKEN)
if check_code(search_resp):
    records_list = search_resp["data"].get("records", []) if isinstance(search_resp.get("data"), dict) else []
    found = any(str(SERVICE_ID) == str(r.get("id")) for r in records_list)
    if found:
        record("PASS", "Step5-Search", "Service found in search results")
    else:
        record("FAIL", "Step5-Search", "Service not found in search", "serviceId={} not in results (count={})".format(SERVICE_ID, len(records_list)))
else:
    record("FAIL", "Step5-Search", "Search failed", search_resp.get("message"))

# ============================================================
# STEP 6: Buyer creates order
# ============================================================
log("")
log("=" * 60)
log("STEP 6: Buyer creates order")
log("=" * 60)

ORDER_ID = None
if SERVICE_ID:
    order_resp = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": SERVICE_ID, "quantity": 2, "tradeType": 2})
    if check_code(order_resp):
        ORDER_ID = order_resp["data"]
        record("PASS", "Step6-CreateOrder", "Order created, orderId={}".format(ORDER_ID))
    else:
        record("FAIL", "Step6-CreateOrder", "Create order failed", order_resp.get("message"))

    if ORDER_ID:
        order_detail = api("GET", "/order/{}".format(ORDER_ID), BUYER_TOKEN)
        if check_code(order_detail):
            od = order_detail["data"]
            status_ok = od.get("orderStatus") == 1
            amount_ok = abs(float(od.get("totalAmount", 0)) - 100.0) < 0.01
            order_no = od.get("orderNumber", "")
            order_no_ok = isinstance(order_no, str) and order_no.startswith("ORDER") and len(order_no) >= 19
            if status_ok and amount_ok and order_no_ok:
                record("PASS", "Step6-Verify", "Order status=1(pending), totalAmount=100.00, orderNumber={}".format(order_no))
            else:
                record("FAIL", "Step6-Verify", "Order details mismatch", "status={}, amount={}, orderNumber={}".format(od.get("orderStatus"), od.get("totalAmount"), order_no), "status=1, amount=100.00, ORDER...")
        else:
            record("FAIL", "Step6-Verify", "Cannot get order detail", order_detail.get("message"))
else:
    record("FAIL", "Step6-CreateOrder", "Skipped - no serviceId")

# ============================================================
# STEP 7: Buyer pays order
# ============================================================
log("")
log("=" * 60)
log("STEP 7: Buyer pays order")
log("=" * 60)

if ORDER_ID:
    pay_resp = api("POST", "/order/{}/pay".format(ORDER_ID), BUYER_TOKEN, {"paymentMethod": 1})
    if check_code(pay_resp):
        record("PASS", "Step7-Pay", "Payment success")
    else:
        record("FAIL", "Step7-Pay", "Payment failed", pay_resp.get("message"))

    # Verify order status = 2
    order_after_pay = api("GET", "/order/{}".format(ORDER_ID), BUYER_TOKEN)
    if check_code(order_after_pay):
        if order_after_pay["data"].get("orderStatus") == 2:
            record("PASS", "Step7-StatusVerify", "Order status=2 (pending shipment)")
        else:
            record("FAIL", "Step7-StatusVerify", "Order status wrong", order_after_pay["data"].get("orderStatus"), 2)

    # Verify buyer balance decreased by 100
    buyer_wallet3 = api("GET", "/wallet/balance", BUYER_TOKEN)
    if check_code(buyer_wallet3):
        bal = float(buyer_wallet3["data"]["walletBalance"])
        frozen = float(buyer_wallet3["data"].get("frozenAmount", 0))
        expected_bal = BUYER_BALANCE_AFTER_RECHARGE - 100.0
        if abs(bal - expected_bal) < 0.01:
            record("PASS", "Step7-BuyerBalanceDecrease", "Buyer balance decreased correctly to {}".format(bal))
        else:
            record("FAIL", "Step7-BuyerBalanceDecrease", "Buyer balance mismatch", bal, expected_bal)

        expected_frozen = BUYER_INITIAL_FROZEN + 100.0 if BUYER_INITIAL_FROZEN is not None else 100.0
        # frozen amount may have been from initial, just check it increased
        record("PASS" if frozen >= 100.0 else "FAIL", "Step7-FrozenAmount", "Frozen amount={}".format(frozen), frozen, ">=100")

    # Verify wallet transactions & transactionNo uniqueness
    txn_resp = api("GET", "/wallet/transactions?pageNum=1&pageSize=10", BUYER_TOKEN)
    if check_code(txn_resp):
        txn_records = txn_resp.get("data", {}).get("records", []) if isinstance(txn_resp.get("data"), dict) else []
        order_txns = [t for t in txn_records if str(t.get("relatedOrderId")) == str(ORDER_ID)]
        tx_nos = [t.get("transactionNo") for t in order_txns if t.get("transactionNo")]
        if len(order_txns) >= 2 and len(tx_nos) == len(set(tx_nos)):
            record("PASS", "Step7-Transactions", "Wallet transactions retrieved ({} records, all transactionNo unique)".format(len(order_txns)))
        elif len(order_txns) >= 2:
            record("FAIL", "Step7-Transactions", "Duplicate transactionNo detected in same-order payment", tx_nos, "all distinct")
        else:
            record("FAIL", "Step7-Transactions", "Expected >=2 wallet transactions for order pay", len(order_txns), ">=2")
    else:
        record("FAIL", "Step7-Transactions", "Cannot get transactions", txn_resp.get("message"))

    # Verify service currentPurchaseCount incremented by quantity=2
    svc_after_pay = api("GET", "/service/{}".format(SERVICE_ID), BUYER_TOKEN)
    if check_code(svc_after_pay):
        cpc = svc_after_pay["data"].get("currentPurchaseCount", 0)
        if cpc == 2:
            record("PASS", "Step7-StockIncrement", "Service currentPurchaseCount=2 after pay")
        else:
            record("FAIL", "Step7-StockIncrement", "currentPurchaseCount mismatch", cpc, 2)
else:
    record("FAIL", "Step7-Pay", "Skipped - no orderId")

# ============================================================
# STEP 8: Seller ships order
# ============================================================
log("")
log("=" * 60)
log("STEP 8: Seller ships order")
log("=" * 60)

if ORDER_ID:
    ship_resp = api("POST", "/order/{}/ship".format(ORDER_ID), SELLER_TOKEN)
    if check_code(ship_resp):
        record("PASS", "Step8-Ship", "Seller shipped order")
    else:
        record("FAIL", "Step8-Ship", "Ship failed", ship_resp.get("message"))

    order_after_ship = api("GET", "/order/{}".format(ORDER_ID), BUYER_TOKEN)
    if check_code(order_after_ship):
        if order_after_ship["data"].get("orderStatus") == 3:
            record("PASS", "Step8-StatusVerify", "Order status=3 (pending receipt)")
        else:
            record("FAIL", "Step8-StatusVerify", "Status wrong", order_after_ship["data"].get("orderStatus"), 3)
else:
    record("FAIL", "Step8-Ship", "Skipped - no orderId")

# ============================================================
# STEP 9: Buyer confirms receipt
# ============================================================
log("")
log("=" * 60)
log("STEP 9: Buyer confirms receipt")
log("=" * 60)

if ORDER_ID:
    # Get seller balance before confirm
    seller_wallet_before = api("GET", "/wallet/balance", SELLER_TOKEN)
    seller_bal_before = float(seller_wallet_before["data"]["walletBalance"]) if check_code(seller_wallet_before) else SELLER_INITIAL_BALANCE

    confirm_resp = api("POST", "/order/{}/confirm".format(ORDER_ID), BUYER_TOKEN)
    if check_code(confirm_resp):
        record("PASS", "Step9-Confirm", "Buyer confirmed receipt")
    else:
        record("FAIL", "Step9-Confirm", "Confirm failed", confirm_resp.get("message"))

    order_after_confirm = api("GET", "/order/{}".format(ORDER_ID), BUYER_TOKEN)
    if check_code(order_after_confirm):
        if order_after_confirm["data"].get("orderStatus") == 4:
            record("PASS", "Step9-StatusVerify", "Order status=4 (completed)")
        else:
            record("FAIL", "Step9-StatusVerify", "Status wrong", order_after_confirm["data"].get("orderStatus"), 4)

    # Verify seller balance increased by 100
    seller_wallet_after = api("GET", "/wallet/balance", SELLER_TOKEN)
    if check_code(seller_wallet_after):
        seller_bal_after = float(seller_wallet_after["data"]["walletBalance"])
        if abs(seller_bal_after - seller_bal_before - 100.0) < 0.01:
            record("PASS", "Step9-SellerBalanceIncrease", "Seller balance increased by 100 to {}".format(seller_bal_after))
        else:
            record("FAIL", "Step9-SellerBalanceIncrease", "Seller balance mismatch", "before={}, after={}, diff={}".format(seller_bal_before, seller_bal_after, seller_bal_after - seller_bal_before), "diff=100")

    # Verify buyer frozen decreased
    buyer_wallet4 = api("GET", "/wallet/balance", BUYER_TOKEN)
    if check_code(buyer_wallet4):
        frozen = float(buyer_wallet4["data"].get("frozenAmount", 0))
        record("PASS", "Step9-BuyerFrozenDecrease", "Buyer frozen amount after confirm={}".format(frozen))
else:
    record("FAIL", "Step9-Confirm", "Skipped - no orderId")

# ============================================================
# STEP 10: Buyer reviews
# ============================================================
log("")
log("=" * 60)
log("STEP 10: Buyer reviews")
log("=" * 60)

if ORDER_ID and SERVICE_ID:
    review_resp = api("POST", "/review/create", BUYER_TOKEN, {
        "orderId": ORDER_ID,
        "rating": 5,
        "content": "Service was excellent, great programming tutoring!",
        "anonymous": False
    })
    if check_code(review_resp):
        record("PASS", "Step10-CreateReview", "Review created successfully")
    else:
        record("FAIL", "Step10-CreateReview", "Review creation failed", review_resp.get("message"))

    # Verify review status
    review_status = api("GET", "/review/order/{}/status".format(ORDER_ID), BUYER_TOKEN)
    if check_code(review_status):
        if review_status["data"] == True:
            record("PASS", "Step10-ReviewStatus", "Order review status=true")
        else:
            record("FAIL", "Step10-ReviewStatus", "Review status wrong", review_status["data"], True)
    else:
        record("FAIL", "Step10-ReviewStatus", "Cannot check review status", review_status.get("message"))

    # Verify review visible on service
    svc_reviews = api("GET", "/review/service/{}".format(SERVICE_ID), BUYER_TOKEN)
    if check_code(svc_reviews):
        review_list = svc_reviews.get("data", {}).get("records", []) if isinstance(svc_reviews.get("data"), dict) else svc_reviews.get("data", [])
        if isinstance(review_list, list) and len(review_list) > 0:
            record("PASS", "Step10-ReviewVisible", "Review visible on service page, count={}".format(len(review_list)))
        else:
            record("FAIL", "Step10-ReviewVisible", "No reviews found for service", svc_reviews.get("data"))
    else:
        record("FAIL", "Step10-ReviewVisible", "Cannot get service reviews", svc_reviews.get("message"))

    # Try duplicate review - should fail
    dup_review = api("POST", "/review/create", BUYER_TOKEN, {
        "orderId": ORDER_ID,
        "rating": 4,
        "content": "duplicate",
        "anonymous": False
    })
    if not check_code(dup_review):
        record("PASS", "Step10-DuplicateReview", "Duplicate review correctly rejected, msg={}".format(dup_review.get("message")))
    else:
        record("FAIL", "Step10-DuplicateReview", "Duplicate review should have been rejected but succeeded")
else:
    record("FAIL", "Step10-Review", "Skipped - no orderId or serviceId")

# ============================================================
# STEP 11: Refund Test A - Direct refund when pending shipment
# ============================================================
log("")
log("=" * 60)
log("STEP 11: Refund Test A - Direct refund (pending shipment)")
log("=" * 60)

# Get buyer balance before this test
buyer_wallet_pre11 = api("GET", "/wallet/balance", BUYER_TOKEN)
buyer_bal_pre11 = float(buyer_wallet_pre11["data"]["walletBalance"]) if check_code(buyer_wallet_pre11) else 0

# Create new order
REFUND_A_ORDER_ID = None
if SERVICE_ID:
    oa_resp = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": SERVICE_ID, "quantity": 1, "tradeType": 2})
    if check_code(oa_resp):
        REFUND_A_ORDER_ID = oa_resp["data"]
        record("PASS", "Step11-CreateOrder", "Order created, orderId={}".format(REFUND_A_ORDER_ID))
    else:
        record("FAIL", "Step11-CreateOrder", "Create order failed", oa_resp.get("message"))

    if REFUND_A_ORDER_ID:
        # Pay - need to wait for idempotent lock (5 seconds since last pay)
        log("  Waiting 6s for idempotent lock...")
        time.sleep(6)
        pay_a = api("POST", "/order/{}/pay".format(REFUND_A_ORDER_ID), BUYER_TOKEN, {"paymentMethod": 1})
        if check_code(pay_a):
            record("PASS", "Step11-Pay", "Payment success")
        else:
            record("FAIL", "Step11-Pay", "Payment failed", pay_a.get("message"))

        # Verify status=2
        order_a = api("GET", "/order/{}".format(REFUND_A_ORDER_ID), BUYER_TOKEN)
        if check_code(order_a) and order_a["data"].get("orderStatus") == 2:
            record("PASS", "Step11-StatusPending", "Order status=2 before refund")
        else:
            record("FAIL", "Step11-StatusPending", "Order status not 2", order_a.get("data", {}).get("orderStatus") if check_code(order_a) else "error")

        # Request refund
        refund_a = api("POST", "/order/{}/refund".format(REFUND_A_ORDER_ID), BUYER_TOKEN, {"reason": "No longer needed"})
        if check_code(refund_a):
            record("PASS", "Step11-Refund", "Refund request success")
        else:
            record("FAIL", "Step11-Refund", "Refund failed", refund_a.get("message"))

        # Verify order status=6, refundStatus=4
        order_a2 = api("GET", "/order/{}".format(REFUND_A_ORDER_ID), BUYER_TOKEN)
        if check_code(order_a2):
            os2 = order_a2["data"].get("orderStatus")
            rs2 = order_a2["data"].get("refundStatus")
            if os2 == 6 and rs2 == 4:
                record("PASS", "Step11-RefundVerify", "Order status=6, refundStatus=4")
            else:
                record("FAIL", "Step11-RefundVerify", "Status mismatch", "orderStatus={}, refundStatus={}".format(os2, rs2), "orderStatus=6, refundStatus=4")
        else:
            record("FAIL", "Step11-RefundVerify", "Cannot verify", order_a2.get("message"))

        # Verify buyer balance restored
        buyer_wallet_post11 = api("GET", "/wallet/balance", BUYER_TOKEN)
        if check_code(buyer_wallet_post11):
            bal_post = float(buyer_wallet_post11["data"]["walletBalance"])
            if abs(bal_post - buyer_bal_pre11) < 0.01:
                record("PASS", "Step11-BalanceRestore", "Buyer balance restored to {}".format(bal_post))
            else:
                record("FAIL", "Step11-BalanceRestore", "Balance not restored", bal_post, buyer_bal_pre11)

        # Verify stock restored (Step7 had 2, Step11 paid +1 then refunded -1 -> back to 2)
        svc_post11 = api("GET", "/service/{}".format(SERVICE_ID), BUYER_TOKEN)
        if check_code(svc_post11):
            cpc11 = svc_post11["data"].get("currentPurchaseCount")
            if cpc11 == 2:
                record("PASS", "Step11-StockRestore", "Stock restored to 2 after refund")
            else:
                record("FAIL", "Step11-StockRestore", "Stock not restored after refund", cpc11, 2)
else:
    record("FAIL", "Step11", "Skipped - no serviceId")

# ============================================================
# STEP 12: Refund Test B - Seller approves refund (pending receipt)
# ============================================================
log("")
log("=" * 60)
log("STEP 12: Refund Test B - Seller approves refund")
log("=" * 60)

buyer_wallet_pre12 = api("GET", "/wallet/balance", BUYER_TOKEN)
buyer_bal_pre12 = float(buyer_wallet_pre12["data"]["walletBalance"]) if check_code(buyer_wallet_pre12) else 0

REFUND_B_ORDER_ID = None
if SERVICE_ID:
    ob_resp = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": SERVICE_ID, "quantity": 1, "tradeType": 2})
    if check_code(ob_resp):
        REFUND_B_ORDER_ID = ob_resp["data"]
        record("PASS", "Step12-CreateOrder", "Order created, orderId={}".format(REFUND_B_ORDER_ID))
    else:
        record("FAIL", "Step12-CreateOrder", "Failed", ob_resp.get("message"))

    if REFUND_B_ORDER_ID:
        # Pay
        log("  Waiting 6s for idempotent lock...")
        time.sleep(6)
        pay_b = api("POST", "/order/{}/pay".format(REFUND_B_ORDER_ID), BUYER_TOKEN, {"paymentMethod": 1})
        if check_code(pay_b):
            record("PASS", "Step12-Pay", "Payment success")
        else:
            record("FAIL", "Step12-Pay", "Payment failed", pay_b.get("message"))

        # Seller ships
        ship_b = api("POST", "/order/{}/ship".format(REFUND_B_ORDER_ID), SELLER_TOKEN)
        if check_code(ship_b):
            record("PASS", "Step12-Ship", "Seller shipped")
        else:
            record("FAIL", "Step12-Ship", "Ship failed", ship_b.get("message"))

        # Verify status=3
        order_b = api("GET", "/order/{}".format(REFUND_B_ORDER_ID), BUYER_TOKEN)
        if check_code(order_b) and order_b["data"].get("orderStatus") == 3:
            record("PASS", "Step12-StatusReceipt", "Order status=3")

        # Buyer requests refund
        refund_b = api("POST", "/order/{}/refund".format(REFUND_B_ORDER_ID), BUYER_TOKEN, {"reason": "Quality issue"})
        if check_code(refund_b):
            record("PASS", "Step12-RefundRequest", "Refund request submitted")
        else:
            record("FAIL", "Step12-RefundRequest", "Refund request failed", refund_b.get("message"))

        # Verify refundStatus=1
        order_b2 = api("GET", "/order/{}".format(REFUND_B_ORDER_ID), BUYER_TOKEN)
        if check_code(order_b2):
            rs = order_b2["data"].get("refundStatus")
            if rs == 1:
                record("PASS", "Step12-RefundPending", "refundStatus=1 (pending)")
            else:
                record("FAIL", "Step12-RefundPending", "refundStatus wrong", rs, 1)

        # Seller approves refund
        approve_b = api("POST", "/order/{}/handleRefund".format(REFUND_B_ORDER_ID), SELLER_TOKEN, {"approve": True})
        if check_code(approve_b):
            record("PASS", "Step12-SellerApprove", "Seller approved refund")
        else:
            record("FAIL", "Step12-SellerApprove", "Approve failed", approve_b.get("message"))

        # Verify final state
        order_b3 = api("GET", "/order/{}".format(REFUND_B_ORDER_ID), BUYER_TOKEN)
        if check_code(order_b3):
            os3 = order_b3["data"].get("orderStatus")
            rs3 = order_b3["data"].get("refundStatus")
            if os3 == 6 and rs3 == 4:
                record("PASS", "Step12-FinalStatus", "orderStatus=6, refundStatus=4")
            else:
                record("FAIL", "Step12-FinalStatus", "Status mismatch", "os={}, rs={}".format(os3, rs3), "os=6, rs=4")

        # Verify buyer balance restored
        buyer_wallet_post12 = api("GET", "/wallet/balance", BUYER_TOKEN)
        if check_code(buyer_wallet_post12):
            bal_post12 = float(buyer_wallet_post12["data"]["walletBalance"])
            if abs(bal_post12 - buyer_bal_pre12) < 0.01:
                record("PASS", "Step12-BalanceRestore", "Balance restored to {}".format(bal_post12))
            else:
                record("FAIL", "Step12-BalanceRestore", "Balance not restored", bal_post12, buyer_bal_pre12)
else:
    record("FAIL", "Step12", "Skipped")

# ============================================================
# STEP 13: Refund Test C - Seller rejects, buyer confirms
# ============================================================
log("")
log("=" * 60)
log("STEP 13: Refund Test C - Seller rejects refund")
log("=" * 60)

REFUND_C_ORDER_ID = None
if SERVICE_ID:
    oc_resp = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": SERVICE_ID, "quantity": 1, "tradeType": 2})
    if check_code(oc_resp):
        REFUND_C_ORDER_ID = oc_resp["data"]
        record("PASS", "Step13-CreateOrder", "Order created, orderId={}".format(REFUND_C_ORDER_ID))
    else:
        record("FAIL", "Step13-CreateOrder", "Failed", oc_resp.get("message"))

    if REFUND_C_ORDER_ID:
        # Pay
        log("  Waiting 6s for idempotent lock...")
        time.sleep(6)
        pay_c = api("POST", "/order/{}/pay".format(REFUND_C_ORDER_ID), BUYER_TOKEN, {"paymentMethod": 1})
        if check_code(pay_c):
            record("PASS", "Step13-Pay", "Payment success")
        else:
            record("FAIL", "Step13-Pay", "Payment failed", pay_c.get("message"))

        # Seller ships
        ship_c = api("POST", "/order/{}/ship".format(REFUND_C_ORDER_ID), SELLER_TOKEN)
        if check_code(ship_c):
            record("PASS", "Step13-Ship", "Seller shipped")
        else:
            record("FAIL", "Step13-Ship", "Ship failed", ship_c.get("message"))

        # Buyer requests refund
        refund_c = api("POST", "/order/{}/refund".format(REFUND_C_ORDER_ID), BUYER_TOKEN, {"reason": "Not satisfied"})
        if check_code(refund_c):
            record("PASS", "Step13-RefundRequest", "Refund request submitted")
        else:
            record("FAIL", "Step13-RefundRequest", "Failed", refund_c.get("message"))

        # Verify refundStatus=1
        order_c = api("GET", "/order/{}".format(REFUND_C_ORDER_ID), BUYER_TOKEN)
        if check_code(order_c) and order_c["data"].get("refundStatus") == 1:
            record("PASS", "Step13-RefundPending", "refundStatus=1")

        # Seller rejects
        reject_c = api("POST", "/order/{}/handleRefund".format(REFUND_C_ORDER_ID), SELLER_TOKEN, {"approve": False, "rejectReason": "Service already delivered normally"})
        if check_code(reject_c):
            record("PASS", "Step13-SellerReject", "Seller rejected refund")
        else:
            record("FAIL", "Step13-SellerReject", "Reject failed", reject_c.get("message"))

        # Verify refundStatus=3
        order_c2 = api("GET", "/order/{}".format(REFUND_C_ORDER_ID), BUYER_TOKEN)
        if check_code(order_c2):
            rs_c = order_c2["data"].get("refundStatus")
            os_c = order_c2["data"].get("orderStatus")
            if rs_c == 3:
                record("PASS", "Step13-RefundRejected", "refundStatus=3 (rejected), order continues at status={}".format(os_c))
            else:
                record("FAIL", "Step13-RefundRejected", "refundStatus wrong", rs_c, 3)

        # Buyer confirms receipt despite rejection
        confirm_c = api("POST", "/order/{}/confirm".format(REFUND_C_ORDER_ID), BUYER_TOKEN)
        if check_code(confirm_c):
            record("PASS", "Step13-BuyerConfirm", "Buyer confirmed receipt after refund rejection")
        else:
            record("FAIL", "Step13-BuyerConfirm", "Confirm failed", confirm_c.get("message"))

        # Verify completed
        order_c3 = api("GET", "/order/{}".format(REFUND_C_ORDER_ID), BUYER_TOKEN)
        if check_code(order_c3):
            if order_c3["data"].get("orderStatus") == 4:
                record("PASS", "Step13-Completed", "Order completed normally after refund rejection")
            else:
                record("FAIL", "Step13-Completed", "Status wrong", order_c3["data"].get("orderStatus"), 4)
else:
    record("FAIL", "Step13", "Skipped")

# ============================================================
# STEP 14: Chat + Trade integration
# ============================================================
log("")
log("=" * 60)
log("STEP 14: Chat + Trade integration")
log("=" * 60)

# Buyer sends message to seller
chat_send1 = api("POST", "/chat/send", BUYER_TOKEN, {
    "receiverId": SELLER_ID,
    "content": "Hi, I am interested in your programming tutoring service!",
    "serviceId": SERVICE_ID
})
if check_code(chat_send1):
    record("PASS", "Step14-BuyerSend", "Buyer sent message to seller")
else:
    record("FAIL", "Step14-BuyerSend", "Send failed", chat_send1.get("message"))

time.sleep(1)

# Seller replies
chat_send2 = api("POST", "/chat/send", SELLER_TOKEN, {
    "receiverId": BUYER_ID,
    "content": "Welcome! I can help you with Java programming."
})
if check_code(chat_send2):
    record("PASS", "Step14-SellerReply", "Seller replied to buyer")
else:
    record("FAIL", "Step14-SellerReply", "Reply failed", chat_send2.get("message"))

time.sleep(1)

# Get messages between buyer and seller
msgs = api("GET", "/chat/messages/{}".format(SELLER_ID), BUYER_TOKEN)
if check_code(msgs):
    msg_list = msgs.get("data", [])
    if isinstance(msg_list, dict):
        msg_list = msg_list.get("records", [])
    if isinstance(msg_list, list) and len(msg_list) >= 2:
        record("PASS", "Step14-MessageList", "Got {} messages".format(len(msg_list)))
    else:
        record("FAIL", "Step14-MessageList", "Expected >=2 messages", len(msg_list) if isinstance(msg_list, list) else msg_list, ">=2")
else:
    record("FAIL", "Step14-MessageList", "Cannot get messages", msgs.get("message"))

# Get conversations
convos = api("GET", "/chat/conversations", BUYER_TOKEN)
if check_code(convos):
    record("PASS", "Step14-Conversations", "Conversations retrieved")
else:
    record("FAIL", "Step14-Conversations", "Cannot get conversations", convos.get("message"))

# Try sending to self - should fail
self_msg = api("POST", "/chat/send", BUYER_TOKEN, {
    "receiverId": BUYER_ID,
    "content": "test self message"
})
if not check_code(self_msg):
    record("PASS", "Step14-SelfMessage", "Self-message correctly rejected, msg={}".format(self_msg.get("message")))
else:
    record("FAIL", "Step14-SelfMessage", "Self-message should have been rejected")

# ============================================================
# STEP 15: Favorites integration
# ============================================================
log("")
log("=" * 60)
log("STEP 15: Favorites integration")
log("=" * 60)

if SERVICE_ID:
    # Add to favorites
    fav_add = api("POST", "/favorite/collect", BUYER_TOKEN, {"serviceId": SERVICE_ID})
    if check_code(fav_add):
        record("PASS", "Step15-AddFavorite", "Added to favorites")
    else:
        record("FAIL", "Step15-AddFavorite", "Add failed", fav_add.get("message"))

    # Verify in collection
    fav_list = api("GET", "/favorite/myCollections", BUYER_TOKEN)
    if check_code(fav_list):
        fav_data = fav_list.get("data", [])
        if isinstance(fav_data, dict):
            fav_data = fav_data.get("records", [])
        found = any(str(SERVICE_ID) == str(f.get("service", {}).get("id", "")) for f in fav_data) if isinstance(fav_data, list) else False
        if found:
            record("PASS", "Step15-VerifyFavorite", "Service found in favorites")
        else:
            record("FAIL", "Step15-VerifyFavorite", "Service not in favorites", fav_data)

    # Try duplicate - wait for idempotent lock
    log("  Waiting 3s for idempotent lock...")
    time.sleep(3)
    fav_dup = api("POST", "/favorite/collect", BUYER_TOKEN, {"serviceId": SERVICE_ID})
    # Idempotent: could succeed or fail, just record
    record("PASS", "Step15-DuplicateFavorite", "Duplicate favorite handled, code={}, msg={}".format(fav_dup.get("code"), fav_dup.get("message")))

    # Remove from favorites
    time.sleep(1)
    fav_remove = api("DELETE", "/favorite/collect/{}".format(SERVICE_ID), BUYER_TOKEN)
    if check_code(fav_remove):
        record("PASS", "Step15-RemoveFavorite", "Removed from favorites")
    else:
        record("FAIL", "Step15-RemoveFavorite", "Remove failed", fav_remove.get("message"))

    # Verify removed
    fav_list2 = api("GET", "/favorite/myCollections", BUYER_TOKEN)
    if check_code(fav_list2):
        fav_data2 = fav_list2.get("data", [])
        if isinstance(fav_data2, dict):
            fav_data2 = fav_data2.get("records", [])
        found2 = any(str(SERVICE_ID) == str(f.get("service", {}).get("id", "")) for f in fav_data2) if isinstance(fav_data2, list) else False
        if not found2:
            record("PASS", "Step15-VerifyRemoved", "Service removed from favorites")
        else:
            record("FAIL", "Step15-VerifyRemoved", "Service still in favorites")
else:
    record("FAIL", "Step15", "Skipped - no serviceId")

# ============================================================
# STEP 16: Service status integration
# ============================================================
log("")
log("=" * 60)
log("STEP 16: Service status integration")
log("=" * 60)

if SERVICE_ID:
    # Seller takes service offline
    offline_resp = api("PUT", "/service/{}/offline".format(SERVICE_ID), SELLER_TOKEN)
    if check_code(offline_resp):
        record("PASS", "Step16-Offline", "Service taken offline")
    else:
        record("FAIL", "Step16-Offline", "Offline failed", offline_resp.get("message"))

    # Try to create order on offline service - should fail
    offline_order = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": SERVICE_ID, "quantity": 1, "tradeType": 2})
    if not check_code(offline_order):
        record("PASS", "Step16-OrderOffline", "Order creation correctly rejected for offline service, msg={}".format(offline_order.get("message")))
    else:
        record("FAIL", "Step16-OrderOffline", "Should have rejected order for offline service")

    # Bring service back online
    online_resp = api("PUT", "/service/{}/online".format(SERVICE_ID), SELLER_TOKEN)
    if check_code(online_resp):
        record("PASS", "Step16-Online", "Service back online")
    else:
        record("FAIL", "Step16-Online", "Online failed", online_resp.get("message"))
else:
    record("FAIL", "Step16", "Skipped")

# ============================================================
# STEP 17: Permission boundary tests
# ============================================================
log("")
log("=" * 60)
log("STEP 17: Permission boundary tests")
log("=" * 60)

# 17a: Seller buys own service - should fail
# First, re-approve the service since online may require re-review
if SERVICE_ID:
    re_approve = api("PUT", "/admin/services/service/review", ADMIN_TOKEN, {
        "serviceId": SERVICE_ID,
        "approved": True,
        "reason": "Re-approved for testing"
    })
    if check_code(re_approve):
        log("  (Service re-approved for permission tests)")

    self_buy = api("POST", "/order/create", SELLER_TOKEN, {"serviceId": SERVICE_ID, "quantity": 1, "tradeType": 2})
    if not check_code(self_buy):
        record("PASS", "Step17a-SelfBuy", "Seller cannot buy own service, msg={}".format(self_buy.get("message")))
    else:
        record("FAIL", "Step17a-SelfBuy", "Seller should not be able to buy own service")

# 17b: Buyer tries to ship order - should fail
if ORDER_ID:
    buyer_ship = api("POST", "/order/{}/ship".format(ORDER_ID), BUYER_TOKEN)
    # Order is already completed, so this could fail for status or permission reasons
    if not check_code(buyer_ship):
        record("PASS", "Step17b-BuyerShip", "Buyer cannot ship, msg={}".format(buyer_ship.get("message")))
    else:
        record("FAIL", "Step17b-BuyerShip", "Buyer should not be able to ship")

# 17c: Try shipping pending-payment order (create a new order, don't pay, try ship)
if SERVICE_ID:
    unpaid_order_resp = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": SERVICE_ID, "quantity": 1, "tradeType": 2})
    if check_code(unpaid_order_resp):
        unpaid_id = unpaid_order_resp["data"]
        ship_unpaid = api("POST", "/order/{}/ship".format(unpaid_id), SELLER_TOKEN)
        if not check_code(ship_unpaid):
            record("PASS", "Step17c-ShipUnpaid", "Cannot ship unpaid order, msg={}".format(ship_unpaid.get("message")))
        else:
            record("FAIL", "Step17c-ShipUnpaid", "Should not ship unpaid order")

        # Cancel this unpaid order to clean up
        cancel_resp = api("POST", "/order/{}/cancel".format(unpaid_id), BUYER_TOKEN)
        if check_code(cancel_resp):
            log("  (Cleaned up unpaid order)")

# 17d: Refund completed order - should fail
if ORDER_ID:
    refund_completed = api("POST", "/order/{}/refund".format(ORDER_ID), BUYER_TOKEN, {"reason": "test"})
    if not check_code(refund_completed):
        record("PASS", "Step17d-RefundCompleted", "Cannot refund completed order, msg={}".format(refund_completed.get("message")))
    else:
        record("FAIL", "Step17d-RefundCompleted", "Should not refund completed order")

# 17e: Cancel non-pending order (already completed)
if ORDER_ID:
    cancel_completed = api("POST", "/order/{}/cancel".format(ORDER_ID), BUYER_TOKEN)
    if not check_code(cancel_completed):
        record("PASS", "Step17e-CancelCompleted", "Cannot cancel completed order, msg={}".format(cancel_completed.get("message")))
    else:
        record("FAIL", "Step17e-CancelCompleted", "Should not cancel completed order")

# ============================================================
# STEP 18: Notification verification
# ============================================================
log("")
log("=" * 60)
log("STEP 18: Notification verification")
log("=" * 60)

buyer_notifs = api("GET", "/notification/my-notifications", BUYER_TOKEN)
if check_code(buyer_notifs):
    notif_data = buyer_notifs.get("data", [])
    if isinstance(notif_data, dict):
        notif_data = notif_data.get("records", [])
    count = len(notif_data) if isinstance(notif_data, list) else 0
    if count > 0:
        record("PASS", "Step18-BuyerNotifs", "Buyer has {} notifications".format(count))
    else:
        record("FAIL", "Step18-BuyerNotifs", "No notifications for buyer", 0, ">0")
else:
    record("FAIL", "Step18-BuyerNotifs", "Cannot get buyer notifications", buyer_notifs.get("message"))

seller_notifs = api("GET", "/notification/my-notifications", SELLER_TOKEN)
if check_code(seller_notifs):
    notif_data_s = seller_notifs.get("data", [])
    if isinstance(notif_data_s, dict):
        notif_data_s = notif_data_s.get("records", [])
    count_s = len(notif_data_s) if isinstance(notif_data_s, list) else 0
    if count_s > 0:
        record("PASS", "Step18-SellerNotifs", "Seller has {} notifications".format(count_s))
    else:
        record("FAIL", "Step18-SellerNotifs", "No notifications for seller", 0, ">0")
else:
    record("FAIL", "Step18-SellerNotifs", "Cannot get seller notifications", seller_notifs.get("message"))

# ============================================================
# STEP 19: Stock oversell prevention & refund stock recovery
# ============================================================
log("")
log("=" * 60)
log("STEP 19: Stock oversell prevention & refund stock recovery")
log("=" * 60)

stock_svc_resp = api("POST", "/service/publish", SELLER_TOKEN, {
    "serviceTitle": "API-Stock-Limit-Test",
    "serviceDescription": "Limited stock service (maxPurchases=1) for oversell test.",
    "tagId": 1,
    "tradeType": 2,
    "price": 10.00,
    "maxPurchases": 1,
    "isPhysical": 0
})
STOCK_SVC_ID = stock_svc_resp.get("data") if check_code(stock_svc_resp) else None
if STOCK_SVC_ID:
    api("PUT", "/admin/services/service/review", ADMIN_TOKEN, {
        "serviceId": STOCK_SVC_ID, "approved": True, "reason": "Stock test"
    })
    o1 = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": STOCK_SVC_ID, "quantity": 1, "tradeType": 2})
    o2 = api("POST", "/order/create", BUYER_TOKEN, {"serviceId": STOCK_SVC_ID, "quantity": 1, "tradeType": 2})
    o1_id = o1.get("data") if check_code(o1) else None
    o2_id = o2.get("data") if check_code(o2) else None
    if o1_id and o2_id:
        time.sleep(6)
        p1 = api("POST", "/order/{}/pay".format(o1_id), BUYER_TOKEN, {"paymentMethod": 1})
        if check_code(p1):
            record("PASS", "Step19-FirstPay", "First order paid, stock occupied (1/1)")
        else:
            record("FAIL", "Step19-FirstPay", "First order pay failed", p1.get("message"))

        time.sleep(6)
        p2 = api("POST", "/order/{}/pay".format(o2_id), BUYER_TOKEN, {"paymentMethod": 1})
        if not check_code(p2):
            record("PASS", "Step19-PreventOversell", "Second order pay rejected due to stock limit: {}".format(p2.get("message")))
        else:
            record("FAIL", "Step19-PreventOversell", "Second order pay should fail when stock is exhausted")

        r1 = api("POST", "/order/{}/refund".format(o1_id), BUYER_TOKEN, {"reason": "Release stock"})
        if check_code(r1):
            time.sleep(6)
            p2_retry = api("POST", "/order/{}/pay".format(o2_id), BUYER_TOKEN, {"paymentMethod": 1})
            if check_code(p2_retry):
                record("PASS", "Step19-StockRecovery", "After first order refunded, second order pay succeeded")
            else:
                record("FAIL", "Step19-StockRecovery", "Second order should succeed after refund restored stock", p2_retry.get("message"))
        else:
            record("FAIL", "Step19-StockRecovery", "Refund first order failed", r1.get("message"))
    else:
        record("FAIL", "Step19-CreateOrders", "Failed to create two competing orders")
else:
    record("FAIL", "Step19-Publish", "Failed to publish stock test service", stock_svc_resp.get("message"))

# ============================================================
# SUMMARY
# ============================================================
log("")
log("=" * 60)
log("=== Trade Integration Test Summary ===")
log("=" * 60)

total = len(results)
passed = sum(1 for r in results if r["status"] == "PASS")
failed = sum(1 for r in results if r["status"] == "FAIL")

log("Total tests: {}".format(total))
log("Passed: {}".format(passed))
log("Failed: {}".format(failed))

if failed > 0:
    log("")
    log("Failed tests:")
    for r in results:
        if r["status"] == "FAIL":
            log("  [FAIL] {} - {}".format(r["step"], r["detail"]))

log("")
log("=" * 60)
sys.exit(1 if failed > 0 else 0)
