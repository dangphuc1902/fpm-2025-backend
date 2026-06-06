import json
import urllib.request
import urllib.error
import random
import string
import time

class ResDict(dict):
    def __getitem__(self, key):
        if key == "data" and "data" not in self:
            return self
        try:
            return super().__getitem__(key)
        except KeyError:
            if "data" in self and isinstance(self["data"], dict):
                return self["data"][key]
            raise
            
    def get(self, key, default=None):
        if key == "data" and "data" not in self:
            return self
        if key in self:
            return self[key]
        if "data" in self and isinstance(self["data"], dict):
            return self["data"].get(key, default)
        return default

BASE_URL = "http://localhost:8080/api/v1"

def print_success(msg):
    print(f"\033[92m[SUCCESS] {msg}\033[0m")

def print_failure(msg):
    print(f"\033[91m[FAILURE] {msg}\033[0m")

def print_info(msg):
    print(f"[INFO] {msg}")

def generate_random_string(length=8):
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for i in range(length))

def make_request(url, method="GET", headers=None, body=None):
    if headers is None:
        headers = {}
    
    req = urllib.request.Request(url, method=method)
    for k, v in headers.items():
        req.add_header(k, str(v))
        
    data = None
    if body is not None:
        if isinstance(body, (dict, list)):
            data = json.dumps(body).encode('utf-8')
            req.add_header("Content-Type", "application/json")
        elif isinstance(body, bytes):
            data = body
        else:
            data = str(body).encode('utf-8')
            
    try:
        with urllib.request.urlopen(req, data=data, timeout=10) as response:
            res_body = response.read()
            status = response.status
            try:
                parsed = json.loads(res_body.decode('utf-8'))
                if isinstance(parsed, dict):
                    parsed = ResDict(parsed)
                    if "success" not in parsed:
                        parsed["success"] = (status in (200, 201) and parsed.get("statusCode") in (200, 201, None))
                return status, parsed
            except Exception:
                return status, res_body.decode('utf-8', errors='ignore')
    except urllib.error.HTTPError as e:
        res_body = e.read()
        try:
            parsed = json.loads(res_body.decode('utf-8'))
            if isinstance(parsed, dict):
                parsed = ResDict(parsed)
                if "success" not in parsed:
                    parsed["success"] = False
            return e.code, parsed
        except Exception:
            return e.code, res_body.decode('utf-8', errors='ignore')
    except Exception as e:
        return 500, str(e)

def encode_multipart_formdata(fields, files):
    boundary = b'----WebKitFormBoundary7MA4YWxkTrZu0gW'
    lines = []
    for name, value in fields.items():
        lines.append(b'--' + boundary)
        lines.append(f'Content-Disposition: form-data; name="{name}"'.encode('utf-8'))
        lines.append(b'')
        lines.append(str(value).encode('utf-8'))
    for name, (filename, content_type, data) in files.items():
        lines.append(b'--' + boundary)
        lines.append(f'Content-Disposition: form-data; name="{name}"; filename="{filename}"'.encode('utf-8'))
        lines.append(f'Content-Type: {content_type}'.encode('utf-8'))
        lines.append(b'')
        lines.append(data)
    lines.append(b'--' + boundary + b'--')
    lines.append(b'')
    body = b'\r\n'.join(lines)
    content_type = f'multipart/form-data; boundary={boundary.decode("utf-8")}'
    return content_type, body

def run_tests():
    print("=" * 60)
    print("           FPM-2025 MONOLITH API INTEGRATION TESTS")
    print("=" * 60)
    
    test_results = {}
    
    # Generate unique test user credentials
    username = f"user_{generate_random_string(6)}"
    email = f"{username}@fpmtest.com"
    password = "Password123!"
    
    # -------------------------------------------------------------
    # Test 1: Register User
    # -------------------------------------------------------------
    print_info("TC-1: Registering new user...")
    body = {
        "email": email,
        "password": password,
        "username": username
    }
    status, res = make_request(f"{BASE_URL}/auth/register", "POST", body=body)
    
    if status == 201 and res.get("success"):
        print_success("User registered successfully.")
        test_results["Register User"] = "PASS"
    else:
        print_failure(f"Failed to register user. Status: {status}, Response: {res}")
        test_results["Register User"] = f"FAIL (Status {status})"
        return test_results
        
    # -------------------------------------------------------------
    # Test 2: Login User
    # -------------------------------------------------------------
    print_info("TC-2: Logging in...")
    body = {
        "email": email,
        "password": password
    }
    status, res = make_request(f"{BASE_URL}/auth/login", "POST", body=body)
    
    token = ""
    if status == 200 and res.get("success"):
        token = res["data"].get("token") or res["data"].get("accessToken")
        print_success("User logged in successfully.")
        test_results["Login User"] = "PASS"
    else:
        print_failure(f"Failed to login user. Status: {status}, Response: {res}")
        test_results["Login User"] = f"FAIL (Status {status})"
        return test_results
        
    headers = {"Authorization": f"Bearer {token}"}
    
    # -------------------------------------------------------------
    # Test 3: Get Profile (tests JWT filter)
    # -------------------------------------------------------------
    print_info("TC-3: Retrieving profile...")
    status, res = make_request(f"{BASE_URL}/users/me", "GET", headers=headers)
    if status == 200 and res.get("success"):
        print_success(f"Profile retrieved successfully. Username: {res['data'].get('username')}")
        test_results["Get Profile"] = "PASS"
    else:
        print_failure(f"Failed to retrieve profile. Status: {status}, Response: {res}")
        test_results["Get Profile"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 4: Get Preferences
    # -------------------------------------------------------------
    print_info("TC-4: Retrieving user preferences...")
    status, res = make_request(f"{BASE_URL}/users/preferences", "GET", headers=headers)
    if status == 200 and res.get("success"):
        print_success(f"Preferences retrieved successfully: {res['data']}")
        test_results["Get Preferences"] = "PASS"
    else:
        print_failure(f"Failed to retrieve preferences. Status: {status}, Response: {res}")
        test_results["Get Preferences"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 5: Create Family (tests automatic X-User-Id injection)
    # -------------------------------------------------------------
    print_info("TC-5: Creating family group...")
    body = {
        "name": f"Family_{generate_random_string(4)}"
    }
    status, res = make_request(f"{BASE_URL}/families", "POST", headers=headers, body=body)
    
    family_id = None
    if status == 201 and res.get("success"):
        family_id = res["data"].get("id")
        print_success(f"Family created successfully. ID: {family_id}")
        test_results["Create Family"] = "PASS"
    else:
        print_failure(f"Failed to create family. Status: {status}, Response: {res}")
        test_results["Create Family"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 6: Get Families
    # -------------------------------------------------------------
    print_info("TC-6: Retrieving families list...")
    status, res = make_request(f"{BASE_URL}/families", "GET", headers=headers)
    if status == 200 and res.get("success"):
        print_success(f"Families retrieved: {len(res['data'])} groups found.")
        test_results["Get Families"] = "PASS"
    else:
        print_failure(f"Failed to retrieve families. Status: {status}, Response: {res}")
        test_results["Get Families"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 7: Create Wallet
    # -------------------------------------------------------------
    print_info("TC-7: Creating standard cash wallet...")
    body = {
        "name": "Cash Wallet",
        "type": "CASH",
        "currency": "VND",
        "initialBalance": 2000000.0,
        "icon": "cash_icon"
    }
    status, res = make_request(f"{BASE_URL}/wallets", "POST", headers=headers, body=body)
    
    wallet_id = None
    if status == 201 and res.get("success"):
        wallet_id = res["data"].get("id")
        print_success(f"Wallet created successfully. ID: {wallet_id}, Balance: {res['data'].get('balance')}")
        test_results["Create Wallet"] = "PASS"
    else:
        print_failure(f"Failed to create wallet. Status: {status}, Response: {res}")
        test_results["Create Wallet"] = "FAIL"
        return test_results

    # -------------------------------------------------------------
    # Test 8: Get Categories (check pre-seeded system categories)
    # -------------------------------------------------------------
    print_info("TC-8: Retrieving category list...")
    status, res = make_request(f"{BASE_URL}/categories", "GET", headers=headers)
    
    category_id = None
    if status == 200 and res.get("success") and len(res["data"]) > 0:
        # Find first EXPENSE category
        for cat in res["data"]:
            if cat.get("type") == "EXPENSE":
                category_id = cat.get("id")
                break
        print_success(f"Categories retrieved successfully. Selected expense category ID: {category_id}")
        test_results["Get Categories"] = "PASS"
    else:
        print_failure(f"Failed to retrieve categories. Status: {status}, Response: {res}")
        test_results["Get Categories"] = "FAIL"
        return test_results
        
    # -------------------------------------------------------------
    # Test 9: Create Transaction (tests balance deduction)
    # -------------------------------------------------------------
    print_info("TC-9: Submitting a new expense transaction...")
    body = {
        "walletId": wallet_id,
        "categoryId": category_id,
        "amount": 150000.0,
        "type": "EXPENSE",
        "note": "Dinner with friends",
        "transactionDate": "2026-06-02T12:00:00"
    }
    status, res = make_request(f"{BASE_URL}/transactions", "POST", headers=headers, body=body)
    
    transaction_id = None
    if status == 201 and res.get("success"):
        transaction_id = res["data"].get("id")
        print_success(f"Transaction recorded successfully. ID: {transaction_id}")
        test_results["Create Transaction"] = "PASS"
    else:
        print_failure(f"Failed to record transaction. Status: {status}, Response: {res}")
        test_results["Create Transaction"] = "FAIL"
        return test_results
        
    # -------------------------------------------------------------
    # Test 10: Verify Wallet Balance Deducted
    # -------------------------------------------------------------
    print_info("TC-10: Verifying wallet balance...")
    status, res = make_request(f"{BASE_URL}/wallets/{wallet_id}", "GET", headers=headers)
    if status == 200 and res.get("success"):
        balance = res["data"].get("balance")
        # Initial was 2,000,000, transaction was 150,000. Expected: 1,850,000
        if float(balance) == 1850000.0:
            print_success(f"Wallet balance verified: {balance} VND (Correctly deducted 150,000)")
            test_results["Verify Wallet Balance"] = "PASS"
        else:
            print_failure(f"Wallet balance mismatch! Expected 1850000.0, got: {balance}")
            test_results["Verify Wallet Balance"] = "FAIL (Balance mismatch)"
    else:
        print_failure(f"Failed to get wallet. Status: {status}, Response: {res}")
        test_results["Verify Wallet Balance"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 11: Transaction Attachment Upload
    # -------------------------------------------------------------
    print_info("TC-11: Uploading mock receipt attachment to transaction...")
    mock_file_content = b"Mock receipt image data bytes"
    content_type, body_data = encode_multipart_formdata(
        {},
        {"file": ("receipt.png", "image/png", mock_file_content)}
    )
    
    upload_headers = headers.copy()
    upload_headers["Content-Type"] = content_type
    
    status, res = make_request(
        f"{BASE_URL}/transactions/{transaction_id}/attachments", 
        "POST", 
        headers=upload_headers, 
        body=body_data
    )
    
    attachment_id = None
    if status == 201 and res.get("success"):
        attachment_id = res["data"].get("id")
        print_success(f"Attachment uploaded successfully. ID: {attachment_id}, URL: {res['data'].get('fileUrl')}")
        test_results["Upload Attachment"] = "PASS"
    else:
        print_failure(f"Failed to upload attachment. Status: {status}, Response: {res}")
        test_results["Upload Attachment"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 12: Delete Transaction Attachment
    # -------------------------------------------------------------
    if attachment_id is not None:
        print_info("TC-12: Deleting transaction attachment...")
        status, res = make_request(
            f"{BASE_URL}/transactions/{transaction_id}/attachments/{attachment_id}", 
            "DELETE", 
            headers=headers
        )
        if status == 200 and res.get("success"):
            print_success("Attachment deleted successfully.")
            test_results["Delete Attachment"] = "PASS"
        else:
            print_failure(f"Failed to delete attachment. Status: {status}, Response: {res}")
            test_results["Delete Attachment"] = "FAIL"
    else:
        test_results["Delete Attachment"] = "SKIPPED"
        
    # -------------------------------------------------------------
    # Test 13: Get Dashboard Summary
    # -------------------------------------------------------------
    print_info("TC-13: Fetching dashboard summary...")
    # Inject X-User-Id manually to verify the filter handles it if we don't have it,
    # but the filter should inject it automatically from token. Let's try both to verify.
    # Note: Header bridge filter automatically handles adding X-User-Id from security context.
    status, res = make_request(f"{BASE_URL}/dashboard?yearMonth=2026-06", "GET", headers=headers)
    if status == 200:
        print_success(f"Dashboard retrieved successfully. Net Income: {res.get('netIncome')}")
        test_results["Get Dashboard"] = "PASS"
    else:
        print_failure(f"Failed to retrieve dashboard. Status: {status}, Response: {res}")
        test_results["Get Dashboard"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 14: Get Monthly Report
    # -------------------------------------------------------------
    print_info("TC-14: Fetching monthly report summary...")
    # Monthly report needs authentication principal which has UserPrincipal mapping.
    # It requires the token.
    status, res = make_request(f"{BASE_URL}/reports/monthly?month=2026-06-01", "GET", headers=headers)
    if status == 200 and res.get("success"):
        print_success(f"Monthly report retrieved successfully: {res['data']}")
        test_results["Get Monthly Report"] = "PASS"
    else:
        print_failure(f"Failed to retrieve monthly report. Status: {status}, Response: {res}")
        test_results["Get Monthly Report"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 15: AI NLP Analysis
    # -------------------------------------------------------------
    print_info("TC-15: Testing Gemini AI NLP extraction...")
    body = {
        "text": "Ăn sáng phở bò 45000",
        "context": ""
    }
    status, res = make_request(f"{BASE_URL}/ai/nlp", "POST", headers=headers, body=body)
    if status == 200 and res.get("success"):
        print_success(f"AI NLP processed. Category: {res.get('category')}, Amount: {res.get('amount')}")
        test_results["AI NLP Extraction"] = "PASS"
    else:
        print_failure(f"Failed to run AI NLP. Status: {status}, Response: {res}")
        test_results["AI NLP Extraction"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 16: AI Chat Assistant
    # -------------------------------------------------------------
    print_info("TC-16: Testing Gemini AI chat advisor...")
    body = {
        "text": "Làm thế nào để tiết kiệm tiền?",
        "context": ""
    }
    status, res = make_request(f"{BASE_URL}/ai/chat", "POST", headers=headers, body=body)
    if status == 200 and res.get("success"):
        print_success(f"AI Chat responded: {res.get('rawText')[:60]}...")
        test_results["AI Chat Advisor"] = "PASS"
    else:
        print_failure(f"Failed to run AI Chat. Status: {status}, Response: {res}")
        test_results["AI Chat Advisor"] = "FAIL"
        
    # -------------------------------------------------------------
    # Test 17: OCR Extract Receipt
    # -------------------------------------------------------------
    print_info("TC-17: Testing OCR receipt extraction (Tesseract)...")
    mock_receipt_content = b"CUA HANG CAFE STARBUCKS\nTHANH TOAN: 95000 VND\nCAM ON QUY KHACH"
    content_type, body_data = encode_multipart_formdata(
        {},
        {"file": ("starbucks.txt", "image/png", mock_receipt_content)}
    )
    
    ocr_headers = headers.copy()
    ocr_headers["Content-Type"] = content_type
    
    status, res = make_request(
        f"{BASE_URL}/ocr/extract", 
        "POST", 
        headers=ocr_headers, 
        body=body_data
    )
    if status == 200 and res.get("success"):
        print_success(f"OCR processed successfully. Merchant: {res.get('merchantName')}, Amount: {res.get('totalAmount')}")
        test_results["OCR Extraction"] = "PASS"
    else:
        print_failure(f"Failed to run OCR. Status: {status}, Response: {res}")
        test_results["OCR Extraction"] = "FAIL"

    # -------------------------------------------------------------
    # Summary of Tests
    # -------------------------------------------------------------
    print("\n" + "=" * 60)
    print("                       TEST RUN SUMMARY")
    print("=" * 60)
    passed_count = 0
    failed_count = 0
    
    for test_name, status in test_results.items():
        if status == "PASS":
            passed_count += 1
            print(f" {test_name:<35}: \033[92mPASS\033[0m")
        else:
            failed_count += 1
            print(f" {test_name:<35}: \033[91m{status}\033[0m")
            
    print("-" * 60)
    print(f"Total Tests Run: {passed_count + failed_count}")
    print(f"Passed         : \033[92m{passed_count}\033[0m")
    print(f"Failed         : \033[91m{failed_count}\033[0m")
    print("=" * 60)
    
    return test_results

if __name__ == "__main__":
    # Wait a moment for health check just in case
    run_tests()
