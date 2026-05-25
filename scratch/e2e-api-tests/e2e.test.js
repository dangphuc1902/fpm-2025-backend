const axios = require('axios');

const BASE_URL = 'http://localhost:8080/api/v1';

describe('FPM E2E API Tests', () => {
    jest.setTimeout(30000); // Tăng timeout lên 30s để tránh lỗi timeout khi khởi động

    let token = '';
    let walletId = '';
    let testEmail = `test_${Date.now()}@example.com`;
    let testPassword = 'Password123!';

    // Cấu hình axios instance để có sẵn headers mặc định
    const apiClient = axios.create({
        baseURL: BASE_URL,
        headers: {
            'Content-Type': 'application/json'
        }
    });

    // Thêm interceptor để tự động gắn token vào request nếu có
    apiClient.interceptors.request.use(config => {
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    });

    test('1. Đăng ký User mới', async () => {
        const response = await apiClient.post('/auth/register', {
            email: testEmail,
            password: testPassword,
            username: `e2e_test_user_${Date.now()}`
        });

        expect(response.status).toBe(201);
        expect(response.data.statusCode).toBe(200);

        // Lưu token nếu API register trả về luôn token
        if (response.data.data && response.data.data.token) {
            token = response.data.data.token;
        }
    });

    test('2. Đăng nhập để lấy accessToken', async () => {
        const response = await apiClient.post('/auth/login', {
            email: testEmail,
            password: testPassword
        });

        expect(response.status).toBe(200);
        expect(response.data.statusCode).toBe(200);
        token = response.data.data.token;
        expect(token).toBeDefined();
    });

    test('3. Gọi API lấy thông tin Profile', async () => {
        const response = await apiClient.get('/users/me');

        expect(response.status).toBe(200);
        expect(response.data.statusCode).toBe(200);
    });

    test('4. Gọi API tạo ví mới (CASH - VND)', async () => {
        const response = await apiClient.post('/wallets', {
            name: "Ví sinh hoạt E2E",
            type: "CASH",
            currency: "VND",
            balance: 0,
            color: "#FFFFFF",
            icon: "wallet"
        });

        expect(response.status).toBe(201);
        expect(response.data.statusCode).toBe(200);
        walletId = response.data.data.id;
        expect(walletId).toBeDefined();
    });

    test('5. Gọi API ghi chép một giao dịch chi tiêu mới từ ví vừa tạo', async () => {
        const response = await apiClient.post('/transactions', {
            walletId: walletId,
            categoryId: 1, // Giả sử ID 1 là một category hợp lệ (ví dụ: Ăn uống)
            amount: 50000,
            type: "EXPENSE",
            date: new Date().toISOString(),
            description: "Test chi tiêu E2E",
            currency: "VND"
        });

        expect(response.status).toBe(201);
        expect(response.data.statusCode).toBe(200);
        expect(response.data.data.id).toBeDefined();
    });

    test('6. Gọi API lấy báo cáo Dashboard', async () => {
        // Đợi một chút để Reporting Service (nếu dùng event-driven) có thời gian đồng bộ
        await new Promise(resolve => setTimeout(resolve, 2000));

        const response = await apiClient.get('/dashboard');

        expect(response.status).toBe(200);
        expect(response.data.statusCode).toBe(200);
        // Kiểm tra xem dữ liệu có trả về thành công không
        expect(response.data.data).toBeDefined();
    });
});
