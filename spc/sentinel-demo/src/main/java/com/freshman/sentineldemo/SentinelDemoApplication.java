package com.freshman.sentineldemo;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@RestController
public class SentinelDemoApplication {

    /**
     * 热点限流的资源名
     */
    private static String resourceName = "freqParam";

    public static void main(String[] args) {
        SpringApplication.run(SentinelDemoApplication.class, args);
        initFlowRules();
    }

    private static void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("HelloWorld");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(2);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);

        // 定义热点限流的规则，对第一个参数设置 qps 限流模式，阈值为5
        ParamFlowRule rule_hot = new ParamFlowRule(resourceName)
                .setParamIdx(0)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(3);

        //针对int类型的参数456，单独设置限流QPS阈值位10，而不是全局的阈值3
        ParamFlowItem item = new ParamFlowItem().setObject(String.valueOf(456))
                //参数类型一定要匹配
                .setClassType(int.class.getName())
                .setCount(10);
        rule_hot.setParamFlowItemList(Collections.singletonList(item));
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule_hot));

    }

    public String hi() {
        while (true) {
            Entry entry = null;
            try {
                entry = SphU.entry("HelloWorld");
                /*您的业务逻辑 - 开始*/
                System.out.println("hello world");
                /*您的业务逻辑 - 结束*/
            } catch (BlockException e1) {
                /*流控逻辑处理 - 开始*/
                System.out.println("block!");
                break;
                /*流控逻辑处理 - 结束*/
            } finally {
                if (entry != null) {
                    entry.exit();
                }
            }
        }
        return "ok";
    }

    @RequestMapping(value = "/h")
    public String hello() {
        hi();
        return "ok";
    }

    /**
     * 热点参数限流
     */
    @GetMapping("/freqParamFlow")
    public String freqParamFlow(@RequestParam("uid") int uid, @RequestParam("ip") Long ip) {
        Entry entry = null;
        String retVal;
        try {
            // 只对参数uid进行限流，参数ip不进行限制
            entry = SphU.entry(resourceName, EntryType.IN, 1, uid);
            retVal = "passed";
        } catch (BlockException e) {
            retVal = "blocked";
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(dateFormat.format(date) + retVal);
        return retVal;
    }


    // 原本的业务方法.
    @GetMapping("/freqParamFlow_r")
    @SentinelResource(blockHandler = "blockHandlerForGetUser")
    public String getUserById(String id) {
        return "getUserById command ok";
    }

    // blockHandler 函数，原方法调用被限流/降级/系统保护的时候调用
    public String blockHandlerForGetUser(String id, BlockException ex) {
        return "getUserById command failed";
    }

}
