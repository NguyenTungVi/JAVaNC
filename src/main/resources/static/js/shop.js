
$(document).ready(function() {

    // ⭐ HÀM AJAX ĐỂ CẬP NHẬT ICON CART ⭐
    function updateHeaderCount(newCount) {
        // Cập nhật thẻ <span> trong header.html
        $('#cart-item-count').text('(' + (newCount || 0) + ')');
    }

    // ⭐ Gửi yêu cầu AJAX để lấy số lượng giỏ hàng ban đầu ⭐
    function loadInitialCartCount() {
        $.ajax({
            url: '/cart/api/cart-count', // Endpoint đã được tạo
            type: 'GET',
            success: function(response) {
                updateHeaderCount(response.count);
            },
            error: function() {
                console.error('Không thể lấy số lượng giỏ hàng ban đầu.');
            }
        });
    }

    // ⭐ Tải số lượng ban đầu khi trang tải xong ⭐
    $.ajax({
        url: '/cart/api/cart-count',
        type: 'GET',
        success: function(response) {
            updateHeaderCount(response.count);
        }
    });


    // ⭐ Xử lý AJAX khi nhấn nút "Giỏ hàng" ⭐
    $('.add-to-cart-ajax').on('click', function(e) {
        e.preventDefault();

        const $button = $(this);
        const productId = $button.data('product-id');
        const quantity = $button.data('current-qty') || 1; // Mặc định là 1

        if (!productId) return;

        // Vô hiệu hóa nút và hiển thị trạng thái tải
        $button.prop('disabled', true).text('Đang thêm...');

        $.ajax({
            url: '/cart/add/' + productId,
            type: 'GET',
            data: { qty: quantity }, // Không cần redirect parameter nữa
            success: function(response) {
                // Kiểm tra phản hồi thành công từ Controller (ví dụ: HTTP Status 200)
                if (response.success) {
                    // 1. Cập nhật số lượng giỏ hàng trên Header
                    updateHeaderCount(response.cartCount);

                    // 2. Khôi phục nút
                    $button.prop('disabled', false).html('<i class="bi bi-cart-plus"></i> Giỏ hàng');

                    // 3. Hiển thị thông báo (nếu cần)
                    alert(response.message);
                } else {
                    // Xử lý lỗi nếu server trả về status 200 nhưng nội dung là lỗi logic
                    alert('Lỗi: ' + response.error);
                    $button.prop('disabled', false).html('<i class="bi bi-cart-plus"></i> Giỏ hàng');
                }
            },
            error: function(xhr) {
                // Xử lý lỗi HTTP (401, 500, 404, v.v.)
                const errorMsg = xhr.responseJSON ? (xhr.responseJSON.error || xhr.responseJSON.message) : 'Lỗi kết nối hoặc phiên hết hạn.';
                alert('Lỗi: ' + errorMsg);
                $button.prop('disabled', false).html('<i class="bi bi-cart-plus"></i> Giỏ hàng');
            }
        });
    });
    // Khởi chạy cập nhật giỏ hàng khi trang tải xong
    loadInitialCartCount();
});
