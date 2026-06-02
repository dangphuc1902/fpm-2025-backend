import json

collection = {
    "info": {
        "name": "FPM-2025 Monolith API",
        "description": "Toàn bộ API của hệ thống Monolith giao tiếp qua Nginx Gateway (Port 8090).",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {"key": "base_url", "value": "http://localhost:8090/api/v1", "type": "string"},
        {"key": "token", "value": "", "type": "string"},
        {"key": "wallet_id", "value": "1", "type": "string"},
        {"key": "transaction_id", "value": "1", "type": "string"},
        {"key": "family_id", "value": "1", "type": "string"},
        {"key": "invitation_id", "value": "1", "type": "string"},
        {"key": "job_id", "value": "1", "type": "string"},
        {"key": "attachment_id", "value": "1", "type": "string"}
    ],
    "item": [
        {
            "name": "1. User Auth Service",
            "item": [
                {
                    "name": "Register",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {"mode": "raw", "raw": "{\n  \"email\": \"test@example.com\",\n  \"password\": \"Password123!\",\n  \"fullName\": \"Test User\",\n  \"phoneNumber\": \"0123456789\"\n}"},
                        "url": {"raw": "{{base_url}}/auth/register", "host": ["{{base_url}}"], "path": ["auth", "register"]}
                    }
                },
                {
                    "name": "Login",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {"mode": "raw", "raw": "{\n  \"email\": \"test@example.com\",\n  \"password\": \"Password123!\"\n}"},
                        "url": {"raw": "{{base_url}}/auth/login", "host": ["{{base_url}}"], "path": ["auth", "login"]}
                    }
                },
                {
                    "name": "Get Profile",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/users/me", "host": ["{{base_url}}"], "path": ["users", "me"]}
                    }
                },
                {
                    "name": "Create Family",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"name\": \"Gia đình nhà Test\"\n}"},
                        "url": {"raw": "{{base_url}}/families", "host": ["{{base_url}}"], "path": ["families"]}
                    }
                },
                {
                    "name": "Get Families",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/families", "host": ["{{base_url}}"], "path": ["families"]}
                    }
                },
                {
                    "name": "Get Invitations",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/families/invitations", "host": ["{{base_url}}"], "path": ["families", "invitations"]}
                    }
                },
                {
                    "name": "Accept Invitation",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/families/invitations/{{invitation_id}}/accept", "host": ["{{base_url}}"], "path": ["families", "invitations", "{{invitation_id}}", "accept"]}
                    }
                },
                {
                    "name": "Reject Invitation",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/families/invitations/{{invitation_id}}/reject", "host": ["{{base_url}}"], "path": ["families", "invitations", "{{invitation_id}}", "reject"]}
                    }
                },
                {
                    "name": "Invite Member",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"email\": \"member@example.com\",\n  \"role\": \"MEMBER\"\n}"},
                        "url": {"raw": "{{base_url}}/families/{{family_id}}/invite", "host": ["{{base_url}}"], "path": ["families", "{{family_id}}", "invite"]}
                    }
                },
                {
                    "name": "Get Family Members",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/families/{{family_id}}/members", "host": ["{{base_url}}"], "path": ["families", "{{family_id}}", "members"]}
                    }
                },
                {
                    "name": "Refresh Token",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Content-Type", "value": "application/json"}],
                        "body": {"mode": "raw", "raw": "{\n  \"refreshToken\": \"some-refresh-token\"\n}"},
                        "url": {"raw": "{{base_url}}/auth/refresh", "host": ["{{base_url}}"], "path": ["auth", "refresh"]}
                    }
                },
                {
                    "name": "Logout",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/auth/logout", "host": ["{{base_url}}"], "path": ["auth", "logout"]}
                    }
                }
            ]
        },
        {
            "name": "2. Wallet Service",
            "item": [
                {
                    "name": "Create Wallet",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"name\": \"Ví sinh hoạt\",\n  \"type\": \"CASH\",\n  \"currency\": \"VND\",\n  \"initialBalance\": 5000000,\n  \"icon\": \"ic_wallet\"\n}"},
                        "url": {"raw": "{{base_url}}/wallets", "host": ["{{base_url}}"], "path": ["wallets"]}
                    }
                },
                {
                    "name": "Get All Wallets",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/wallets", "host": ["{{base_url}}"], "path": ["wallets"]}
                    }
                },
                {
                    "name": "Get Active Wallets",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/wallets/active", "host": ["{{base_url}}"], "path": ["wallets", "active"]}
                    }
                },
                {
                    "name": "Get Shared Wallets",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/wallets/shared", "host": ["{{base_url}}"], "path": ["wallets", "shared"]}
                    }
                },
                {
                    "name": "Share Wallet",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"userId\": 2,\n  \"permissionLevel\": \"VIEWER\"\n}"},
                        "url": {"raw": "{{base_url}}/wallets/{{wallet_id}}/share", "host": ["{{base_url}}"], "path": ["wallets", "{{wallet_id}}", "share"]}
                    }
                },
                {
                    "name": "Get Categories",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/categories", "host": ["{{base_url}}"], "path": ["categories"]}
                    }
                }
            ]
        },
        {
            "name": "3. Transaction Service",
            "item": [
                {
                    "name": "Create Transaction",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"walletId\": {{wallet_id}},\n  \"categoryId\": 1,\n  \"amount\": 50000,\n  \"type\": \"EXPENSE\",\n  \"note\": \"Ăn sáng\",\n  \"transactionDate\": \"2026-03-31T08:00:00\"\n}"},
                        "url": {"raw": "{{base_url}}/transactions", "host": ["{{base_url}}"], "path": ["transactions"]}
                    }
                },
                {
                    "name": "List Transactions",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {
                            "raw": "{{base_url}}/transactions?walletId={{wallet_id}}&page=0&size=10",
                            "host": ["{{base_url}}"],
                            "path": ["transactions"],
                            "query": [
                                {"key": "walletId", "value": "{{wallet_id}}"},
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "10"}
                            ]
                        }
                    }
                },
                {
                    "name": "Update Transaction",
                    "request": {
                        "method": "PUT",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"amount\": 60000,\n  \"note\": \"Ăn sáng (cập nhật)\"\n}"},
                        "url": {"raw": "{{base_url}}/transactions/{{transaction_id}}", "host": ["{{base_url}}"], "path": ["transactions", "{{transaction_id}}"]}
                    }
                },
                {
                    "name": "Delete Transaction",
                    "request": {
                        "method": "DELETE",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/transactions/{{transaction_id}}", "host": ["{{base_url}}"], "path": ["transactions", "{{transaction_id}}"]}
                    }
                },
                {
                    "name": "Upload Attachment",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "file", "type": "file", "src": ""}
                            ]
                        },
                        "url": {"raw": "{{base_url}}/transactions/{{transaction_id}}/attachments", "host": ["{{base_url}}"], "path": ["transactions", "{{transaction_id}}", "attachments"]}
                    }
                },
                {
                    "name": "Delete Attachment",
                    "request": {
                        "method": "DELETE",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/transactions/{{transaction_id}}/attachments/{{attachment_id}}", "host": ["{{base_url}}"], "path": ["transactions", "{{transaction_id}}", "attachments", "{{attachment_id}}"]}
                    }
                }
            ]
        },
        {
            "name": "4. Reporting Service",
            "item": [
                {
                    "name": "Get Dashboard",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/dashboard", "host": ["{{base_url}}"], "path": ["dashboard"]}
                    }
                },
                {
                    "name": "Get Monthly Report",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {
                            "raw": "{{base_url}}/reports/monthly?month=3&year=2026",
                            "host": ["{{base_url}}"],
                            "path": ["reports", "monthly"],
                            "query": [
                                {"key": "month", "value": "3"},
                                {"key": "year", "value": "2026"}
                            ]
                        }
                    }
                },
                {
                    "name": "Export PDF",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/reports/export/pdf?month=3&year=2026", "host": ["{{base_url}}"], "path": ["reports", "export", "pdf"]}
                    }
                },
                {
                    "name": "Export Excel",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/reports/export/excel?month=3&year=2026", "host": ["{{base_url}}"], "path": ["reports", "export", "excel"]}
                    }
                },
                {
                    "name": "Spending by Category",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/reports/spending-by-category?month=3&year=2026", "host": ["{{base_url}}"], "path": ["reports", "spending-by-category"]}
                    }
                },
                {
                    "name": "Trends",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/reports/trends?year=2026", "host": ["{{base_url}}"], "path": ["reports", "trends"]}
                    }
                },
                {
                    "name": "Budget Comparison",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/reports/budget-comparison?month=3&year=2026", "host": ["{{base_url}}"], "path": ["reports", "budget-comparison"]}
                    }
                },
                {
                    "name": "Export Async",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"month\": 3,\n  \"year\": 2026,\n  \"format\": \"PDF\"\n}"},
                        "url": {"raw": "{{base_url}}/reports/export", "host": ["{{base_url}}"], "path": ["reports", "export"]}
                    }
                },
                {
                    "name": "Get Export Status",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/reports/export/{{job_id}}", "host": ["{{base_url}}"], "path": ["reports", "export", "{{job_id}}"]}
                    }
                },
                {
                    "name": "Download Export Result",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/reports/export/{{job_id}}/download", "host": ["{{base_url}}"], "path": ["reports", "export", "{{job_id}}", "download"]}
                    }
                }
            ]
        },
        {
            "name": "5. Notification Service",
            "item": [
                {
                    "name": "Receive Bank Notification",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"smsContent\": \"GD: +50,000VND vao tai khoan...\",\n  \"sender\": \"Vietcombank\"\n}"},
                        "url": {"raw": "{{base_url}}/notifications/receive", "host": ["{{base_url}}"], "path": ["notifications", "receive"]}
                    }
                },
                {
                    "name": "Register FCM Token",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"token\": \"fcm-token-123456\"\n}"},
                        "url": {"raw": "{{base_url}}/notifications/fcm/register", "host": ["{{base_url}}"], "path": ["notifications", "fcm", "register"]}
                    }
                },
                {
                    "name": "Get Notification History",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {"raw": "{{base_url}}/notifications/history", "host": ["{{base_url}}"], "path": ["notifications", "history"]}
                    }
                }
            ]
        },
        {
            "name": "6. OCR & AI Services",
            "item": [
                {
                    "name": "OCR Extract Bill",
                    "request": {
                        "method": "POST",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "body": {
                            "mode": "formdata",
                            "formdata": [
                                {"key": "file", "type": "file", "src": ""}
                            ]
                        },
                        "url": {"raw": "{{base_url}}/ocr/extract", "host": ["{{base_url}}"], "path": ["ocr", "extract"]}
                    }
                },
                {
                    "name": "AI Analyze NLP",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"text\": \"Uống cafe 50k\",\n  \"context\": \"None\"\n}"},
                        "url": {"raw": "{{base_url}}/ai/nlp", "host": ["{{base_url}}"], "path": ["ai", "nlp"]}
                    }
                },
                {
                    "name": "AI Check Anomaly",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"walletId\": {{wallet_id}},\n  \"amount\": 50000000,\n  \"categoryId\": 1\n}"},
                        "url": {"raw": "{{base_url}}/ai/anomaly", "host": ["{{base_url}}"], "path": ["ai", "anomaly"]}
                    }
                },
                {
                    "name": "AI Chat",
                    "request": {
                        "method": "POST",
                        "header": [
                            {"key": "Authorization", "value": "Bearer {{token}}"},
                            {"key": "Content-Type", "value": "application/json"}
                        ],
                        "body": {"mode": "raw", "raw": "{\n  \"message\": \"Tôi đã chi bao nhiêu tiền trong tháng này?\"\n}"},
                        "url": {"raw": "{{base_url}}/ai/chat", "host": ["{{base_url}}"], "path": ["ai", "chat"]}
                    }
                }
            ]
        }
    ]
}

output_path = "d:/WorkSpace/App_Dev/FPM_Project/Backend/FPM_Monolith_Postman_Collection.json"
with open(output_path, "w", encoding="utf-8") as f:
    json.dump(collection, f, ensure_ascii=False, indent=4)
print(f"Postman collection successfully generated at: {output_path}")
