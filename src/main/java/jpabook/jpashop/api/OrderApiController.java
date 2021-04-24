package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        return orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
    }

    /**
     * 컬렉션 패치조인
     * distinct : DB에 실제 distinct쿼리를 날려주고
     * 가져온 데이터에서 중복인 엔티티를 제거해준다
     * 단점 : 페이징 불가
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        return orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
    }

    /**
     * xtoOne 관계인 것만 fetch join 후 *페이징처리*
     * 나머지는 LAZY로딩하는데 default_batch_fetch_size 옵션을 줘서 in쿼리로 처리
     * 위 옵션은 글로벌한 옵션이고, 각 엔티티마다 적용하고 싶으면 @BatchSize(size=...)사용
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(@RequestParam(value="offset", defaultValue = "0") int offset,
                                        @RequestParam(value="limit", defaultValue = "100") int limit
                                        ) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        return orders.stream()
                .map(o -> new OrderDto(o))
                .collect(toList());
    }

    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * ToOne 관계만 조인쿼리로 전부 가져온 뒤 orderItems를 가져오기 위해
     * orderId를 in절로 조회함, 조회한 orderItems들은 orderId별로 Map에
     * List형태로 담아둔 뒤 Order를 foreach로 돌리며 세팅
     * api스펙에 정확히 맞춰서 데이터를 가져옴
     * 대신, 코드가 복잡해짐
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * 모든 연관관계를 조인쿼리로 한번에 가져옴
     * 가져온 데이터들을 stream으로 돌리며 중복데이터 그룹핑처리 및 api스펙에 맞게
     * dto변환처리
     * V5에 비해 수행되는 쿼리횟수가 적음
     * 데이터 양이 많아 질 수록 조회하는 데이터양이 기하 급수적으로 늘어남
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        return flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                                o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(toList());

    }

    @Data
    static class OrderDto {

        private Long oderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;


        public OrderDto(Order order) {
            this.oderId = order.getId();
            this.name = order.getMember().getName();
            this.orderDate = order.getOrderDate();
            this.orderStatus = order.getStatus();
            this.address = order.getDelivery().getAddress();
            this.orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(toList());
        }
    }

    @Getter
    static class OrderItemDto{

        private String itemName; //상품명
        private int orderPrice; //주문 가격
        private int count; //주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
