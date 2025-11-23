package com.commerce.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * @author Yixi Wan
 * @date 2025/11/4 15:07
 * @package com.commerce.order.config
 * <p>
 * Description:
 */
@Configuration
@EnableStateMachineFactory
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderState, OrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderState, OrderEvent> states) throws Exception {
        states.withStates()
                .initial(OrderState.CREATED)
                .state(OrderState.PAID)
                .state(OrderState.SHIPPED)
                .state(OrderState.COMPLETED)
                .state(OrderState.CANCELLED)
                .state(OrderState.PAYMENT_FAILED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderState, OrderEvent> transitions) throws Exception {

        transitions
                // 支付成功
                .withExternal().source(OrderState.CREATED).target(OrderState.PAID)
                .event(OrderEvent.PAYMENT_SUCCEEDED)

                .and()
                // 支付失败
                .withExternal().source(OrderState.CREATED).target(OrderState.PAYMENT_FAILED)
                .event(OrderEvent.PAYMENT_FAILED)

                .and()
                // 支付过期自动取消
                .withExternal().source(OrderState.CREATED).target(OrderState.CANCELLED)
                .event(OrderEvent.PAYMENT_EXPIRED)

                .and()
                // 管理员取消
                .withExternal().source(OrderState.CREATED).target(OrderState.CANCELLED)
                .event(OrderEvent.ADMIN_CANCELLED)

                .and()
                // 发货
                .withExternal().source(OrderState.PAID).target(OrderState.SHIPPED)
                .event(OrderEvent.ORDER_SHIPPED)

                .and()
                // 订单完成
                .withExternal().source(OrderState.SHIPPED).target(OrderState.COMPLETED)
                .event(OrderEvent.ORDER_COMPLETED);
    }
}


