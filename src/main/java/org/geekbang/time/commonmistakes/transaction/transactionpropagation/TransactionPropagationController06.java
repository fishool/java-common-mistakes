package org.geekbang.time.commonmistakes.transaction.transactionpropagation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("transactionpropagation")
@Slf4j
public class TransactionPropagationController06 {


    /**
     * 第一，因为配置不正确，导致方法上的事务没生效。我们务必确认调用 @Transactional 注
     * 解标记的方法是 public 的，并且是通过 Spring 注入的 Bean 进行调用的。
     *
     * 第二，因为异常处理不正确，导致事务虽然生效但出现异常时没回滚。Spring 默认只会对
     * 标记 @Transactional 注解的方法出现了 RuntimeException 和 Error 的时候回滚，如果
     * 我们的方法捕获了异常，那么需要通过手动编码处理事务回滚。如果希望 Spring 针对其他
     * 异常也可以回滚，那么可以相应配置 @Transactional 注解的 rollbackFor 和
     * noRollbackFor 属性来覆盖其默认设置。
     *
     * 第三，如果方法涉及多次数据库操作，并希望将它们作为独立的事务进行提交或回滚，那么
     * 我们需要考虑进一步细化配置事务传播方式，也就是 @Transactional 注解的
     * Propagation 属性
     **/

    @Autowired
    private UserService userService;

    @GetMapping("wrong")
    public int wrong(@RequestParam("name") String name) {
        try {
            userService.createUserWrong(new UserEntity(name));
        } catch (Exception ex) {
            log.error("createUserWrong failed, reason:{}", ex.getMessage());
        }
        return userService.getUserCount(name);
    }

    @GetMapping("wrong2")
    public int wrong2(@RequestParam("name") String name) {
        try {
            userService.createUserWrong2(new UserEntity(name));
        } catch (Exception ex) {
            log.error("createUserWrong2 failed, reason:{}", ex.getMessage(), ex);
        }
        return userService.getUserCount(name);
    }

    @GetMapping("right")
    public int right(@RequestParam("name") String name) {
        userService.createUserRight(new UserEntity(name));
        return userService.getUserCount(name);
    }
}
