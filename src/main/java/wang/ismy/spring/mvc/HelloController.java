package wang.ismy.spring.mvc;

import wang.ismy.spring.mvc.annotation.Controller;
import wang.ismy.spring.mvc.annotation.RequestMapping;

/**
 * @author MY
 * @date 2020/1/14 14:37
 */
@Controller
public class HelloController {

    @RequestMapping("/index")
    public String hello(){
        return "index";
    }
}
