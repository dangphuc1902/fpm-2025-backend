# 📱 FPM-2025: App Workflow & Feature Experience Guide

> **Phiên bản:** 1.0 (Dựa trên Source Code Backend cập nhật 2026-04-06)  
> **Mục tiêu:** Cung cấp cái nhìn toàn diện về tính năng và trải nghiệm người dùng (UI/UX) cho đội ngũ phát triển Mobile/Frontend.

---

## 🏗️ 1. Concept & Hệ Sinh Thái Người Dùng

FPM-2025 không chỉ là app quản lý chi tiêu cá nhân, mà là một **Trung tâm Tài chính Gia đình Thông minh**. Hệ thống xoay quanh 3 thực thể chính:
1.  **Individual (Cá nhân):** Quản lý ví riêng, thu nhập riêng.
2.  **Family (Gia đình):** Nhóm các thành viên để cùng theo dõi tổng chi tiêu gia đình.
3.  **Shared Wallet (Ví dùng chung):** Ví cho phép nhiều người cùng nộp tiền vào và chi tiêu (ví dụ: Ví tiền Chợ, Ví du lịch).

---

## 🚀 2. Danh Sách Tính Năng Đầy Đủ (Feature Catalog)

### A. Quản lý Tài khoản & Định danh (Auth & Profile)
*   **Smart Login/Register:** Hỗ trợ Email/Password và Google OAuth2 (tích hợp sẵn gRPC validation).
*   **Family Hub:** 
    *   Tạo gia đình mới, mời thành viên qua Email.
    *   Quản lý vai trò (Owner, Admin, Member) trong gia đình.
*   **User Preferences:** Tùy chỉnh ngôn ngữ (vi/en), đơn vị tiền tệ (VND/USD), Theme (Light/Dark/Glassmorphism) và Múi giờ.

### B. Hệ thống Ví đa năng (Wallet Management)
*   **Phân loại ví:** Ví Tiền mặt (Cash), Thẻ ngân hàng (Bank/Card), Ví dùng chung (Shared).
*   **Shared Permission:** Phân quyền chi tiết trên ví (Viewer, Editor, Admin).
*   **Auto-Reconciliation:** Tự động cập nhật số dư ví khi có giao dịch phát sinh từ bất kỳ nguồn nào.

### C. Ghi chép Giao dịch Thông minh (Transaction Engine)
Đây là "trái tim" của App với 4 cách nhập liệu cực nhanh:
1.  **Manual Input (Thủ công):** UI tối ưu với Calculator tích hợp để nhập số tiền nhanh.
2.  **OCR Scan (Quét hóa đơn):** Chụp ảnh hóa đơn, AI tự động tách số tiền, hạng mục, tên cửa hàng (Merchant) và ngày giờ.
3.  **AI Smart NLP (Ngôn ngữ tự nhiên):** Người dùng chỉ cần gõ hoặc nói: *"Vừa ăn phở hết 50k bằng ví tiền mặt"* -> AI tự động tạo giao dịch hoàn chỉnh.
4.  **Bank System Sync (Đồng bộ SMS/Notif):** Chặn và đọc thông báo từ App Ngân hàng (VCB, MB, MoMo...) để tự động ghi chép (Auto-reconcile).

### D. Ngân sách & Cảnh báo (Budgeting)
*   **Flexible Budgets:** Thiết lập hạn mức chi tiêu theo tháng/tuần cho từng hạng mục (Ví dụ: Ăn uống max 5 triệu/tháng).
*   **Smart Alerts:** Gửi thông báo đẩy (FCM) khi chi tiêu chạm ngưỡng 80% hoặc vượt 100% hạn mức.

### E. Báo cáo & Phân tích (Reporting & Analytics)
*   **Dashboard Tổng quan:** Biểu đồ xu hướng thu chi trong 6 tháng gần nhất (Line chart).
*   **Spending Breakdown:** Biểu đồ tròn (Pie chart) phân tích tỷ lệ các hạng mục chi tiêu.
*   **Comparison:** So sánh chi tiêu thực tế với kế hoạch ngân sách.
*   **Professional Export:** Xuất báo cáo đẹp mắt dưới dạng PDF hoặc Excel để lưu trữ hoặc nộp cho "Nội tướng" gia đình.

---

## 🔄 3. Luồng Công Việc Chính (Core Workflows)

### Workflow 1: Thiết lập Gia đình & Ví chung (Onboarding)
1.  **User A** tạo tài khoản -> App tự động tạo 1 "Ví tiền mặt" mặc định.
2.  **User A** tạo "Gia đình Alpha" -> Mời **User B** (vợ/chồng).
3.  **User A** tạo "Ví đi chợ" -> Share quyền `Editor` cho **User B**.
4.  Cả hai cùng thấy số dư ví này và nhận thông báo khi người kia vừa tiêu tiền từ ví chung.

### Workflow 2: Ghi chép chi tiêu "Một chạm" (Daily Tracking)
1.  Sau khi ăn cơm, người dùng mở App, chọn tính năng **Scan Receipt**.
2.  Chụp hóa đơn -> **OCR Service** xử lý ảnh -> **AI Service** phân tích text.
3.  App hiển thị màn hình Review với các thông tin đã điền sẵn -> Người dùng nhấn **Save**.
4.  **Wallet Service** trừ tiền ví tương ứng -> **Reporting Service** cập nhật biểu đồ ngay lập tức.

### Workflow 3: Cảnh báo Ngân sách (Security & Control)
1.  **Transaction Service** ghi nhận một khoản chi lớn cho "Mua sắm".
2.  Giao dịch đẩy qua Kafka -> **Reporting Service** tính toán lại tổng chi tiêu tháng.
3.  Nếu tổng chi tiêu > 80% định mức -> **Notification Service** gửi Push Notif: *"Cẩn thận! Bạn đã tiêu hết 80% ngân sách ăn uống tháng này."*

---

## 🎨 4. Trải Nghiệm UI/UX Mong Đợi (Design Principles)

Để xứng tầm với Backend mạnh mẽ, UI cần đạt các tiêu chí **Premium**:
*   **Glassmorphism Effect:** Sử dụng các lớp nền kính mờ cho các Card thông tin, tạo cảm giác hiện đại, thanh thoát.
*   **Micro-Animations:** Hiệu ứng số tiền "nhảy" (count up) khi load Dashboard, hiệu ứng chuyển trang mượt mà.
*   **Vibrant Color Palette:** Sử dụng màu sắc hài hòa cho các Type giao dịch (Xanh cho Thu nhập, Đỏ cam cho Chi tiêu).
*   **Contextual Actions:** Menu thông minh gợi ý các hạng mục thường dùng nhất dựa trên thói quen người dùng.

---
*Tài liệu này được trích xuất trực tiếp từ logic triển khai của các Microservices trong thư mục `Backend/`.*
