package com.fpm_2025.wallet_service.dto.payload.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Generic pagination response wrapper.
 *
 * @param <T> the type of content in the page
 */
@Data
@Builder
public class PageResponse<T> {

    private List<T> content;       // Danh sách dữ liệu trong trang hiện tại
    private int pageNumber;        // Số thứ tự của trang hiện tại (bắt đầu từ 0)
    private int pageSize;          // Kích thước của mỗi trang (số phần tử mỗi trang)
    private long totalElements;    // Tổng số phần tử trong toàn bộ kết quả
    private int totalPages;        // Tổng số trang
    private boolean last;          // Có phải là trang cuối cùng hay không
}