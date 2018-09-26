package it.loreii.AppSyslogd;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.loreii.AppSyslogd.SyslogMsg.Data;

@Controller
public class ActionWebController {


	@Autowired
	SyslogDataRepository repo;
	
    @RequestMapping("/th-index")
    public String index(Model model) {
    	List<Data> result = repo.findAll();
		model.addAttribute("list", result);
        return "index";
    }

}
