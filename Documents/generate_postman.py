import json

collection = {
    "info": {
        "name": "FPM-2025 Microservices API",
        "description": "Toàn bộ API (REST) giao tiếp qua API Gateway (Port 8080).",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {"key": "base_url", "value": "http://localhost:8080/api/v1", "type": "string"},
        {"key": "token", "value": "", "type": "string"},
        {"key": "wallet_id", "value": "1", "type": "string"},
        {"key": "transaction_id", "value": "1", "type": "string"},
        {"key": "family_id", "value": "1", "type": "string"}
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
                    "name": "Get Wallet Transactions",
                    "request": {
                        "method": "GET",
                        "header": [{"key": "Authorization", "value": "Bearer {{token}}"}],
                        "url": {
                            "raw": "{{base_url}}/transactions/wallet/{{wallet_id}}?page=0&size=10",
                            "host": ["{{base_url}}"],
                            "path": ["transactions", "wallet", "{{wallet_id}}"],
                            "query": [
                                {"key": "page", "value": "0"},
                                {"key": "size", "value": "10"}
                            ]
                        }
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
                }
            ]
        },
        {
            "name": "5. OCR & AI Services",
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
                }
            ]
        }
    ]
}

with open("d:/WorkSpace/App_Dev/FPM_Project/Backend/Documents/FPM_2025_Postman_Collection.json", "w", encoding="utf-8") as f:
    json.dump(collection, f, ensure_ascii=False, indent=4)
