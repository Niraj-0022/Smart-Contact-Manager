package com.smart.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@RequestMapping("/home")
	public String home(Model model) {
		model.addAttribute("title", "Home-Smart Contact Manager");
		return "home";
	}
	
	@RequestMapping("/about")
	public String about(Model model) {
		model.addAttribute("title","About-Smart Contact Manager");
		return "about";
	}
	
	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title","Register-Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}
	
	@RequestMapping(value="/do_register",method=RequestMethod.POST)
    public String registerUser(@ModelAttribute("user") User user,@RequestParam(value="agreement",defaultValue="false")boolean agreement,Model model,HttpSession session,RedirectAttributes redirectAttributes) {
        
		try {
			if(!agreement) {
	        	System.out.println("You have not agrred the terms and conditions");
	        	throw new Exception("You have not agreed the terms and conditions");
			}
            user.setRole("ROLE_USER");
            user.setEnabled(true);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message",
                    new Message("Successfully Registered!", "alert-success"));
		   return "redirect:/signup";  // ✔ redirect after POS
		  } catch (Exception e) {
		   redirectAttributes.addFlashAttribute("message",
		                    new Message("Something went wrong! " + e.getMessage(), "alert-danger"));
		    redirectAttributes.addFlashAttribute("user", user);
		    return "redirect:/signup";
		  }

    }
	
	   @GetMapping("/signin")
	    public String customLogin(Model model)
	    {
		   model.addAttribute("title","Login Page");
		   return "login";
	    }

	
	@GetMapping("/test")
	@ResponseBody
	public String test() {
		
		User user=new User();
		user.setName("Durgesh Tiwari");
		user.setEmail("durgesh@dev.io");
		userRepository.save(user);
		return "Working";
	}
}
